const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserProfile(uid, now = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    displayName: "Aminata Ouédraogo",
    avatarId: "owl_scholar",
    totalXp: 120,
    weeklyXp: 80,
    currentStreak: 3,
    bestStreak: 5,
    hearts: 5,
    gems: 50,
    dailyGoalXp: 50,
    todayXp: 30,
    lastActiveDate: "2026-11-21",
    league: "Bronze",
    completedLevelsCount: 2,
    totalCorrect: 18,
    totalAnswered: 20,
    createdAt: now,
    updatedAt: now,
  };
}

function validLevelProgress(levelId, unitId = "unit_culture", now = new Date(Date.now() - 5000)) {
  return {
    levelId,
    unitId,
    stars: 3,
    bestScore: 90,
    attempts: 1,
    completed: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validLeaderboardEntry(uid, now = new Date(Date.now() - 5000)) {
  return {
    userId: uid,
    displayName: "Aminata Ouédraogo",
    avatarId: "owl_scholar",
    totalXp: 120,
    weeklyXp: 80,
    currentStreak: 3,
    league: "Bronze",
    completedLevelsCount: 2,
    publicVisible: true,
    createdAt: now,
    updatedAt: now,
  };
}

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

// 1. Unauthenticated access denied everywhere
test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID)));
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("level_progress").doc("lvl_1").get()
  );
  await assertFails(
    unauthDb.collection("leaderboard").where("publicVisible", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardEntry(ALICE_UID))
  );
});

// 2. User profile CRUD & cross-user isolation
test("Owner can create, read, and partially update own UserProfile; cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, created))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalXp: 170,
      weeklyXp: 130,
      todayXp: 80,
      completedLevelsCount: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable field tamper rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // List /users collection is forbidden
  await assertFails(aliceDb.collection("users").get());

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      totalXp: 9999,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

// 3. LevelProgress subcollection tests
test("Owner can manage own level_progress subcollection; invalid/cross-user rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty subcollection list works for owner (no existing() reference in allow list)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("level_progress").get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("level_progress")
      .doc("lvl_culture_1")
      .set(validLevelProgress("lvl_culture_1", "unit_culture", created))
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("level_progress")
      .doc("lvl_culture_1")
      .update({
        stars: 3,
        bestScore: 100,
        attempts: 2,
        completed: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject out-of-bounds stars (> 3)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("level_progress")
      .doc("lvl_culture_1")
      .update({
        stars: 5,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("level_progress")
      .doc("lvl_culture_2")
      .set(validLevelProgress("lvl_culture_2", "unit_culture", new Date(Date.now() + 60000)))
  );

  // Cross-user subcollection read/write blocked
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("level_progress").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("level_progress")
      .doc("lvl_culture_1")
      .set(validLevelProgress("lvl_culture_1", "unit_culture", created))
  );
});

// 4. Public Leaderboard tests (owner-write, authenticated filtered list)
test("Leaderboard: owner writes own entry; signed-in users query publicVisible == true; unfiltered fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set(validLeaderboardEntry(ALICE_UID, created))
  );
  await assertSucceeds(
    bobDb.collection("leaderboard").doc(BOB_UID).set(validLeaderboardEntry(BOB_UID, created))
  );

  // Filtered query matching Kotlin repository succeeds
  await assertSucceeds(
    aliceDb.collection("leaderboard").where("publicVisible", "==", true).limit(100).get()
  );

  // Unfiltered list query rejected by rule
  await assertFails(aliceDb.collection("leaderboard").get());

  // Bob cannot overwrite Alice's leaderboard score
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      totalXp: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid league rejected
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).update({
      league: "GrandMasterCheater",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
