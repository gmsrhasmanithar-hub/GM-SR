const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_admin_1";
const BOB_UID = "bob_admin_2";

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

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated user cannot read or write any committee data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("committees").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("committees")
      .doc("comm_1")
      .set({
        name: "Unauthorized Committee",
        memberCount: 10,
        monthlyInstallment: 5000,
        totalMonths: 10,
        collectionDayStart: 28,
        collectionDayEnd: 8,
        payoutDay: 10,
        startMonth: "2026-11",
        endMonth: "2027-08",
        status: "ACTIVE",
        createdAt: pastDate(),
        updatedAt: pastDate(),
      })
  );
});

test("Admin profile create, partial update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "GM Hasmani Admin",
      email: "admin@example.com",
      preferredLanguage: "en",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update allowed keys only
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "ur",
      activeCommitteeId: "comm_1",
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "en",
      updatedAt: pastDate(500),
    })
  );

  // Shadow/ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isSuperAdmin: true,
      updatedAt: pastDate(500),
    })
  );
});

test("Dynamic committee, members, installments, paymentLogs, and reminderLogs parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  const commRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("committees")
    .doc("comm_25");

  // Create dynamic 25-member committee (NO @DocumentId id key)
  await assertSucceeds(
    commRef.set({
      name: "Relatives 25-Member Kameti",
      memberCount: 25,
      monthlyInstallment: 10000,
      totalMonths: 25,
      collectionDayStart: 28,
      collectionDayEnd: 8,
      payoutDay: 10,
      startMonth: "2026-11",
      endMonth: "2028-11",
      status: "ACTIVE",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject if @DocumentId `id` field is accidentally included
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("committees")
      .doc("comm_bad")
      .set({
        id: "comm_bad",
        name: "Bad",
        memberCount: 10,
        monthlyInstallment: 5000,
        totalMonths: 10,
        collectionDayStart: 28,
        collectionDayEnd: 8,
        payoutDay: 10,
        startMonth: "2026-11",
        endMonth: "2027-08",
        status: "ACTIVE",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Create member
  const memberRef = commRef.collection("members").doc("mem_1");
  await assertSucceeds(
    memberRef.set({
      name: "Tariq Hasmani",
      phone: "03001234567",
      committeeNumber: 1,
      payoutMonth: "2026-11",
      payoutStatus: "PENDING",
      payoutAmount: 250000,
      status: "ACTIVE",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create installment
  const instRef = commRef.collection("installments").doc("mem_1_2026-11");
  await assertSucceeds(
    instRef.set({
      memberId: "mem_1",
      memberName: "Tariq Hasmani",
      memberPhone: "03001234567",
      committeeNumber: 1,
      monthKey: "2026-11",
      monthIndex: 1,
      dueAmount: 10000,
      paidAmount: 0,
      status: "UNPAID",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Record payment update
  await assertSucceeds(
    instRef.update({
      paidAmount: 10000,
      status: "PAID",
      paymentDate: "2026-11-28",
      updatedAt: pastDate(1000),
    })
  );

  // Immutable createdAt cannot be modified
  await assertFails(
    instRef.update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Append-only payment log
  const logRef = commRef.collection("paymentLogs").doc("log_1");
  await assertSucceeds(
    logRef.set({
      installmentId: "mem_1_2026-11",
      memberId: "mem_1",
      memberName: "Tariq Hasmani",
      monthKey: "2026-11",
      previousPaidAmount: 0,
      newPaidAmount: 10000,
      previousStatus: "UNPAID",
      newStatus: "PAID",
      createdAt: pastDate(1000),
    })
  );
  await assertFails(logRef.update({ newPaidAmount: 5000 }));

  // Reminder log
  const remRef = commRef.collection("reminderLogs").doc("rem_1");
  await assertSucceeds(
    remRef.set({
      memberId: "mem_1",
      memberName: "Tariq Hasmani",
      memberPhone: "03001234567",
      monthKey: "2026-11",
      channel: "WHATSAPP_MANUAL",
      scheduleStage: "FIRST_28",
      messagePreview: "Assalam-o-Alaikum Tariq Hasmani...",
      dispatchStatus: "OPENED_APP",
      createdAt: pastDate(1000),
    })
  );
  await assertFails(remRef.update({ dispatchStatus: "SENT_API" }));

  // Queries aligned with client repository
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("committees")
      .orderBy("createdAt", "desc")
      .get()
  );
  await assertSucceeds(
    commRef.collection("members").orderBy("committeeNumber", "asc").get()
  );
  await assertSucceeds(commRef.collection("installments").get());

  // Cross-user isolation on subcollections
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("committees")
      .doc("comm_25")
      .collection("members")
      .get()
  );
});

test("Reject tampered future timestamps and invalid bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const futureDate = new Date(Date.now() + 120000);

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("committees")
      .doc("comm_future")
      .set({
        name: "Future",
        memberCount: 10,
        monthlyInstallment: 5000,
        totalMonths: 10,
        collectionDayStart: 28,
        collectionDayEnd: 8,
        payoutDay: 10,
        startMonth: "2026-11",
        endMonth: "2027-08",
        status: "ACTIVE",
        createdAt: futureDate,
        updatedAt: futureDate,
      })
  );

  // Oversized name (> 80 chars) rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("committees")
      .doc("comm_long")
      .set({
        name: "A".repeat(100),
        memberCount: 10,
        monthlyInstallment: 5000,
        totalMonths: 10,
        collectionDayStart: 28,
        collectionDayEnd: 8,
        payoutDay: 10,
        startMonth: "2026-11",
        endMonth: "2027-08",
        status: "ACTIVE",
        createdAt: pastDate(),
        updatedAt: pastDate(),
      })
  );
});
