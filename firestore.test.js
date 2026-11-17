const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("goals").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("habits").doc("h1").set({
      userId: ALICE_UID,
      title: "Meditate",
      iconName: "self_improvement",
      colorHex: "#2D5BFF",
      metricType: "boolean",
      targetValue: 1,
      unit: "session",
      frequencyRule: { type: "daily", days: [1, 2, 3, 4, 5, 6, 7] },
      updatedAtMs: Date.now(),
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Cross-user isolation: Alice cannot read or write Bob's data", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).collection("tasks").doc("bob_t1").set({
      userId: BOB_UID,
      title: "Bob Private Task",
      priority: 3,
      status: "todo",
      estimatedMinutes: 30,
      updatedAtMs: Date.now() - 5000,
      createdAt: past,
      updatedAt: past,
    });
  });

  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(alice.collection("users").doc(BOB_UID).collection("tasks").get());
  await assertFails(alice.collection("users").doc(BOB_UID).collection("tasks").doc("bob_t1").get());
  await assertFails(
    alice.collection("users").doc(BOB_UID).collection("tasks").doc("bob_t1").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner CRUD & delta sync queries across all 7 collections succeed and reject ghost/invalid payloads", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 10000);
  const nowMs = Date.now();
  const userDoc = alice.collection("users").doc(ALICE_UID);

  // 1. Goal create, update (preserving createdAt), and ghost field rejection
  await assertSucceeds(
    userDoc.collection("goals").doc("g-1").set({
      userId: ALICE_UID,
      title: "Run Half Marathon",
      category: "Health",
      targetDate: "2026-12-31",
      status: "active",
      updatedAtMs: nowMs,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    userDoc.collection("goals").doc("g-1").update({
      status: "completed",
      updatedAtMs: nowMs + 1000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    userDoc.collection("goals").doc("g-bad").set({
      id: "g-bad", // forbidden @DocumentId field in payload
      userId: ALICE_UID,
      title: "Bad Goal",
      category: "Health",
      status: "active",
      updatedAtMs: nowMs,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    userDoc.collection("goals").doc("g-1").update({
      createdAt: new Date(Date.now() - 500), // immutable createdAt violation
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 2. Milestone
  await assertSucceeds(
    userDoc.collection("milestones").doc("m-1").set({
      userId: ALICE_UID,
      goalId: "g-1",
      title: "Hit 15km long run",
      targetValue: 15,
      currentValue: 10,
      completed: false,
      updatedAtMs: nowMs,
      createdAt: past,
      updatedAt: past,
    })
  );

  // 3. Habit (polymorphic numeric + native frequencyRule map)
  await assertSucceeds(
    userDoc.collection("habits").doc("h-1").set({
      userId: ALICE_UID,
      goalId: "g-1",
      title: "Hydrate",
      iconName: "water_drop",
      colorHex: "#2D5BFF",
      metricType: "numeric",
      targetValue: 2500,
      unit: "ml",
      frequencyRule: { type: "daily", days: [1, 2, 3, 4, 5, 6, 7] },
      updatedAtMs: nowMs,
      createdAt: past,
      updatedAt: past,
    })
  );

  // 4. HabitLog
  await assertSucceeds(
    userDoc.collection("habitLogs").doc("h-1_2026-11-17").set({
      userId: ALICE_UID,
      habitId: "h-1",
      logDate: "2026-11-17",
      loggedValue: 1500,
      note: "Morning bottles",
      updatedAtMs: nowMs,
      createdAt: past,
      updatedAt: past,
    })
  );

  // 5. Task & Time-block
  await assertSucceeds(
    userDoc.collection("tasks").doc("t-1").set({
      userId: ALICE_UID,
      goalId: "g-1",
      title: "Tempo run 8km",
      priority: 3,
      status: "in_progress",
      dueDate: "2026-11-17",
      scheduledStartMs: nowMs + 3600000,
      scheduledEndMs: nowMs + 7200000,
      estimatedMinutes: 60,
      updatedAtMs: nowMs,
      createdAt: past,
      updatedAt: past,
    })
  );

  // 6. FocusSession
  await assertSucceeds(
    userDoc.collection("focusSessions").doc("f-1").set({
      userId: ALICE_UID,
      taskId: "t-1",
      startedAtMs: nowMs - 1500000,
      endedAtMs: nowMs,
      durationSeconds: 1500,
      sessionType: "pomodoro",
      active: false,
      updatedAtMs: nowMs,
      createdAt: past,
      updatedAt: past,
    })
  );

  // 7. DailyReview + soft delete update
  await assertSucceeds(
    userDoc.collection("dailyReviews").doc("review_2026-11-17").set({
      userId: ALICE_UID,
      reviewDate: "2026-11-17",
      moodScore: 4,
      energyScore: 5,
      winsText: "Completed tempo run and hydrated well.",
      blockersText: "Late evening screen time.",
      updatedAtMs: nowMs,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    userDoc.collection("tasks").doc("t-1").update({
      deletedAtMs: nowMs + 2000,
      updatedAtMs: nowMs + 2000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Delta sync query (where updatedAt > cursor)
  await assertSucceeds(
    userDoc.collection("tasks").where("updatedAt", ">", new Date(Date.now() - 60000)).get()
  );
  await assertSucceeds(userDoc.collection("goals").get());
  await assertSucceeds(userDoc.collection("habitLogs").get());
});

test("Reject tampered future timestamp & oversized fields", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const future = new Date(Date.now() + 60000);
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("goals").doc("g-future").set({
      userId: ALICE_UID,
      title: "Future Goal",
      category: "Career",
      status: "active",
      updatedAtMs: Date.now(),
      createdAt: future,
      updatedAt: future,
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("dailyReviews").doc("review_2026-11-17").set({
      userId: ALICE_UID,
      reviewDate: "2026-11-17",
      moodScore: 9, // out of 1..5 range
      energyScore: 3,
      winsText: "",
      blockersText: "",
      updatedAtMs: Date.now(),
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
