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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function validItemPayload(userId = ALICE_UID) {
  const now = new Date();
  return {
    userId,
    sku: "BRG-001",
    name: "Motor Induksi 3 Phase",
    category: "Suku Cadang",
    locationRack: "Zona A - Rak 01",
    quantity: 25,
    minStock: 10,
    unit: "Unit",
    unitPrice: 1500000,
    supplierName: "PT Teknik Jaya",
    notes: "Simpan di tempat kering",
    createdAt: now,
    updatedAt: now,
  };
}

function validMovementPayload(userId = ALICE_UID) {
  const now = new Date();
  return {
    userId,
    itemId: "item_1",
    itemName: "Motor Induksi 3 Phase",
    itemSku: "BRG-001",
    type: "IN",
    quantityChanged: 10,
    previousStock: 15,
    newStock: 25,
    referenceDoc: "PO-2026-001",
    partyName: "PT Teknik Jaya",
    notes: "Restock bulanan",
    createdAt: now,
  };
}

test("1. Unauthenticated user cannot read or write items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("items").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("items").doc("item_1").set(validItemPayload())
  );
});

test("2. Authenticated owner can create, read, update, and list their own items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("items").doc("item_1");

  await assertSucceeds(itemRef.set(validItemPayload(ALICE_UID)));
  await assertSucceeds(itemRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("items").get());

  await assertSucceeds(
    itemRef.update({
      quantity: 35,
      updatedAt: new Date(),
    })
  );
});

test("3. Cross-user isolation: Bob cannot read, list, or update Alice's items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("items").doc("item_1").set(validItemPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("items").doc("item_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("items").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("items").doc("item_1").update({
      quantity: 0,
      updatedAt: new Date(),
    })
  );
});

test("4. Shadow update test: extra unknown fields are rejected on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("items").doc("item_1");

  await assertFails(
    itemRef.set({
      ...validItemPayload(ALICE_UID),
      isVerifiedAdmin: true,
    })
  );

  await assertSucceeds(itemRef.set(validItemPayload(ALICE_UID)));
  await assertFails(
    itemRef.update({
      quantity: 20,
      ghostField: "hacked",
      updatedAt: new Date(),
    })
  );
});

test("5. Value poisoning test: negative quantity or invalid movement type is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("items").doc("item_neg");

  await assertFails(
    itemRef.set({
      ...validItemPayload(ALICE_UID),
      quantity: -5,
    })
  );

  const movRef = aliceDb.collection("users").doc(ALICE_UID).collection("movements").doc("mov_1");
  await assertFails(
    movRef.set({
      ...validMovementPayload(ALICE_UID),
      type: "INVALID_TYPE",
    })
  );
});

test("6. Stock movements are append-only: owner can create and read, but cannot update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const movRef = aliceDb.collection("users").doc(ALICE_UID).collection("movements").doc("mov_1");

  await assertSucceeds(movRef.set(validMovementPayload(ALICE_UID)));
  await assertSucceeds(movRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("movements").get());

  await assertFails(
    movRef.update({
      quantityChanged: 999,
    })
  );
});
