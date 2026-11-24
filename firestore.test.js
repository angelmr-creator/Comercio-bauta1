const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_bauta";
const BOB_UID = "bob_actor";
const CARLOS_UID = "carlos_actor";
const ROOT_ADMIN_UID = "root_admin_uid";

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

test("Unauthenticated access is denied across locales, actors, settlements, and workspaces", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/locales`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/actors`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/settlements`).get());
  await assertFails(unauthDb.collection("workspaces").doc("comercio-bauta").get());
  await assertFails(unauthDb.collection("workspaces/comercio-bauta/actors").get());
  await assertFails(unauthDb.collection("workspaces/comercio-bauta/settlements").get());
  await assertFails(unauthDb.collection("workspaces/comercio-bauta/payment_events").get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/locales`).doc("loc_1").set({
      code: "LOC-01",
      name: "Portal",
      monthlyRentCents: 500000,
      status: "DISPONIBLE",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Cross-user reads and writes are strictly denied on personal /users/{uid}", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection(`users/${BOB_UID}/actors`).doc("actor_bob").set({
      name: "Cafetería Sur",
      legalType: "TCP",
      representativeName: "Roberto Pérez",
      monthlyRentCents: 350000,
      profitSharePercent: 30,
      active: true,
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${BOB_UID}/actors`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/actors`).doc("actor_bob").get());
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/actors`).doc("actor_bob").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can list empty collections and create/update/delete locales", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const localesCol = aliceDb.collection(`users/${ALICE_UID}/locales`);

  await assertSucceeds(localesCol.get());

  const created = new Date(Date.now() - 10000);
  await assertSucceeds(
    localesCol.doc("loc_1").set({
      code: "LOC-01",
      name: "Local Boulevard",
      address: "Ave 251 #14208, Bauta",
      areaSqm: 42.5,
      monthlyRentCents: 800000,
      status: "DISPONIBLE",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    localesCol.doc("loc_1").update({
      status: "ARRENDADO",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    localesCol.doc("loc_2").set({
      id: "loc_2",
      code: "LOC-02",
      name: "Bodega",
      monthlyRentCents: 400000,
      status: "DISPONIBLE",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    localesCol.doc("loc_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create and update personal economic actors and monthly settlements", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const actorsCol = aliceDb.collection(`users/${ALICE_UID}/actors`);
  const settlementsCol = aliceDb.collection(`users/${ALICE_UID}/settlements`);
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    actorsCol.doc("actor_1").set({
      name: "Panadería El Trigal",
      legalType: "PDL",
      representativeName: "María González",
      phone: "5355551234",
      taxId: "NIT-900123",
      localeId: "loc_1",
      localeName: "LOC-01 · Local Boulevard",
      monthlyRentCents: 800000,
      profitSharePercent: 30,
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    settlementsCol.doc("set_1").set({
      actorId: "actor_1",
      actorName: "Panadería El Trigal",
      localeId: "loc_1",
      localeName: "LOC-01 · Local Boulevard",
      periodYear: 2026,
      periodMonth: 11,
      grossIncomeCents: 10000000,
      operatingExpensesCents: 6000000,
      netProfitCents: 4000000,
      profitSharePercent: 30,
      profitShareCents: 1200000,
      spaceRentCents: 800000,
      totalDueCents: 2000000,
      paidCents: 0,
      paymentStatus: "PENDIENTE",
      declaredBy: "ACTOR",
      paymentChannel: "NO_DEFINIDO",
      paymentReference: "",
      notes: "Declaración de noviembre",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    settlementsCol.doc("set_1").update({
      paidCents: 2000000,
      paymentStatus: "PAGADO",
      paymentChannel: "QR_LINK",
      paymentReference: "ENZ-948271",
      paidAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    settlementsCol.doc("set_1").update({
      actorId: "actor_1",
      actorName: "Panadería El Trigal",
      localeId: "loc_1",
      localeName: "LOC-01 · Local Boulevard",
      periodYear: 2026,
      periodMonth: 11,
      grossIncomeCents: 12000000,
      operatingExpensesCents: 6500000,
      netProfitCents: 5500000,
      profitSharePercent: 30,
      profitShareCents: 1650000,
      spaceRentCents: 800000,
      totalDueCents: 2450000,
      paidCents: 2000000,
      paymentStatus: "PARCIAL",
      declaredBy: "DIRECTOR",
      paymentChannel: "QR_LINK",
      paymentReference: "ENZ-948271",
      notes: "Actualización de ingresos y gastos desde app",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(settlementsCol.get());

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/config`).doc("payment").set({
      bankName: "BANDEC",
      bankAccount: "9225 9598 1122 3344",
      accountHolder: "Empresa Municipal de Comercio Bauta",
      qrPaymentUrl: "https://pagos.bauta.cu/qr/comercio",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    settlementsCol.doc("set_2").set({
      actorId: "actor_1",
      actorName: "Panadería El Trigal",
      periodYear: 2026,
      periodMonth: 12,
      grossIncomeCents: 5000000,
      operatingExpensesCents: 3000000,
      netProfitCents: 2000000,
      profitSharePercent: 30,
      profitShareCents: 600000,
      spaceRentCents: 800000,
      totalDueCents: 1400000,
      paidCents: 0,
      paymentStatus: "PENDIENTE",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );
});

test("Shared comercio-bauta workspace syncs actors, settlements, and payment_events to Manager while keeping actors isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const carlosDb = testEnv.authenticatedContext(CARLOS_UID).firestore();
  const rootAdminDb = testEnv.authenticatedContext(ROOT_ADMIN_UID, {
    email: "amena4430@gmail.com",
    email_verified: true,
  }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: "amena4430@gmail.com",
    email_verified: false,
  }).firestore();
  const nowPast = new Date(Date.now() - 5000);

  // 1. Manager Alice creates shared workspace "comercio-bauta"
  await assertSucceeds(
    aliceDb.collection("workspaces").doc("comercio-bauta").set({
      ownerId: ALICE_UID,
      name: "Empresa Municipal de Comercio Bauta",
      bankName: "BANDEC",
      bankAccount: "0618221336820012",
      accountHolder: "Empresa Municipal de Comercio Bauta",
      qrPaymentUrl: "",
      memberUids: [ALICE_UID],
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  // 2. Empty scoped list queries succeed for owner and actors; unfiltered list fails for Bob
  await assertSucceeds(
    aliceDb.collection("workspaces/comercio-bauta/actors").where("ownerId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("workspaces/comercio-bauta/settlements").where("createdByUid", "==", BOB_UID).get()
  );
  await assertFails(bobDb.collection("workspaces/comercio-bauta/actors").get());
  await assertFails(bobDb.collection("workspaces/comercio-bauta/settlements").get());
  await assertFails(bobDb.collection("workspaces/comercio-bauta/payment_events").get());

  // 3. Spoofed unverified admin email CANNOT do unfiltered reads
  await assertFails(spoofedAdminDb.collection("workspaces/comercio-bauta/settlements").get());

  // 4. Bob registers his business in shared workspace (even before joining memberUids)
  await assertSucceeds(
    bobDb.collection("workspaces/comercio-bauta/actors").doc("actor_bob_cafeteria").set({
      workspaceId: "comercio-bauta",
      ownerId: ALICE_UID,
      createdByUid: BOB_UID,
      name: "Cafetería La Plaza",
      legalType: "TCP",
      representativeName: "Roberto Pérez",
      phone: "5355559999",
      taxId: "NIT-111222",
      localeId: "",
      localeName: "",
      monthlyRentCents: 0,
      profitSharePercent: 30,
      active: true,
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  // Carlos cannot read Bob's actor record
  await assertFails(carlosDb.collection("workspaces/comercio-bauta/actors").doc("actor_bob_cafeteria").get());

  // 5. Bob submits a monthly settlement declaration (Actor only!)
  await assertFails(
    bobDb.collection("workspaces/comercio-bauta/settlements").doc("set_bob_forged").set({
      workspaceId: "comercio-bauta",
      ownerId: ALICE_UID,
      createdByUid: BOB_UID,
      actorId: "actor_bob_cafeteria",
      actorName: "Cafetería La Plaza",
      periodYear: 2026,
      periodMonth: 11,
      grossIncomeCents: 20000000,
      operatingExpensesCents: 10000000,
      netProfitCents: 10000000,
      profitSharePercent: 30,
      profitShareCents: 3000000,
      spaceRentCents: 500000,
      totalDueCents: 3500000,
      paidCents: 0,
      paymentStatus: "PENDIENTE",
      declaredBy: "DIRECTOR", // Forbidden for actor!
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  await assertSucceeds(
    bobDb.collection("workspaces/comercio-bauta/settlements").doc("set_bob_nov").set({
      workspaceId: "comercio-bauta",
      ownerId: ALICE_UID,
      createdByUid: BOB_UID,
      actorUid: BOB_UID,
      actorId: "actor_bob_cafeteria",
      actorName: "Cafetería La Plaza",
      localeId: "",
      localeName: "",
      periodYear: 2026,
      periodMonth: 11,
      grossIncomeCents: 20000000,
      operatingExpensesCents: 10000000,
      netProfitCents: 10000000,
      profitSharePercent: 30,
      profitShareCents: 3000000,
      spaceRentCents: 500000,
      totalDueCents: 3500000,
      paidCents: 1500000,
      paymentStatus: "PARCIAL",
      declaredBy: "ACTOR",
      paymentChannel: "TRANSFERENCIA",
      paymentReference: "TM-771122",
      notes: "Declaración desde teléfono del actor",
      paidAt: nowPast,
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  // Carlos cannot read Bob's settlement
  await assertFails(carlosDb.collection("workspaces/comercio-bauta/settlements").doc("set_bob_nov").get());

  // 6. Bob sends live payment event (works offline/immediately without needing memberUids sync first!)
  await assertFails(
    bobDb.collection("workspaces/comercio-bauta/payment_events").doc("evt_cheat").set({
      workspaceId: "comercio-bauta",
      ownerId: ALICE_UID,
      createdByUid: BOB_UID,
      settlementId: "set_bob_nov",
      actorId: "actor_bob_cafeteria",
      actorName: "Cafetería La Plaza",
      periodYear: 2026,
      periodMonth: 11,
      amountCents: 1500000,
      paymentChannel: "TRANSFERENCIA",
      paymentReference: "TM-771122",
      reportedBy: "ACTOR",
      verificationStatus: "CONFIRMADO", // Actor cannot auto-confirm
      clientCreatedAtMillis: Date.now() - 1000,
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  await assertSucceeds(
    bobDb.collection("workspaces/comercio-bauta/payment_events").doc("evt_bob_1").set({
      workspaceId: "comercio-bauta",
      ownerId: ALICE_UID,
      createdByUid: BOB_UID,
      settlementId: "set_bob_nov",
      actorId: "actor_bob_cafeteria",
      actorName: "Cafetería La Plaza",
      periodYear: 2026,
      periodMonth: 11,
      amountCents: 1500000,
      paymentChannel: "TRANSFERENCIA",
      paymentReference: "TM-771122",
      reportedBy: "ACTOR",
      verificationStatus: "POR_VERIFICAR",
      clientCreatedAtMillis: Date.now() - 1000,
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  // 7. Manager Alice AND Verified Root Admin see ALL actors, settlements, and payments!
  await assertSucceeds(
    aliceDb.collection("workspaces/comercio-bauta/actors").where("ownerId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("workspaces/comercio-bauta/settlements").where("ownerId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("workspaces/comercio-bauta/payment_events").where("ownerId", "==", ALICE_UID).get()
  );
  await assertSucceeds(rootAdminDb.collection("workspaces/comercio-bauta/settlements").get());
  await assertSucceeds(rootAdminDb.collection("workspaces/comercio-bauta/payment_events").get());

  // 8. Bob cannot confirm his own payment; Alice confirms it
  await assertFails(
    bobDb.collection("workspaces/comercio-bauta/payment_events").doc("evt_bob_1").update({
      verificationStatus: "CONFIRMADO",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("workspaces/comercio-bauta/payment_events").doc("evt_bob_1").update({
      verificationStatus: "CONFIRMADO",
      verifiedByUsername: "director1",
      note: "Confirmado con SMS BANDEC",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 9. Bob updates his own settlement or queries his own payment events cleanly
  await assertSucceeds(
    bobDb.collection("workspaces/comercio-bauta/payment_events").where("createdByUid", "==", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("workspaces/comercio-bauta/settlements").doc("set_bob_nov").update({
      paidCents: 3500000,
      paymentStatus: "PAGADO",
      declaredBy: "ACTOR",
      updatedAt: new Date(Date.now() - 400),
    })
  );
});

test("Manager-created actor, settlement, and payment_event tagged with actorEmail are readable and self-editable only by verified matching actor", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobVerifiedDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@bauta.cu",
    email_verified: true,
  }).firestore();
  const bobUnverifiedDb = testEnv.authenticatedContext("bob_unverified_uid", {
    email: "bob@bauta.cu",
    email_verified: false,
  }).firestore();
  const carlosDb = testEnv.authenticatedContext(CARLOS_UID, {
    email: "carlos@bauta.cu",
    email_verified: true,
  }).firestore();
  const nowPast = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("workspaces").doc("comercio-bauta").set({
      ownerId: ALICE_UID,
      name: "Comercio Municipal Bauta",
      bankName: "BANDEC",
      bankAccount: "0618221336820012",
      accountHolder: "Empresa Municipal de Comercio Bauta",
      qrPaymentUrl: "",
      memberUids: [ALICE_UID],
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  await assertSucceeds(
    aliceDb.collection("workspaces/comercio-bauta/actors").doc("actor_mgr_bob").set({
      workspaceId: "comercio-bauta",
      ownerId: ALICE_UID,
      createdByUid: ALICE_UID,
      actorEmail: "bob@bauta.cu",
      name: "Bodega El Parque",
      legalType: "TCP",
      representativeName: "Roberto Pérez",
      phone: "5355558888",
      taxId: "NIT-777888",
      localeId: "loc_1",
      localeName: "LOC-01 · Parque",
      monthlyRentCents: 600000,
      profitSharePercent: 30,
      active: true,
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  await assertSucceeds(
    aliceDb.collection("workspaces/comercio-bauta/settlements").doc("set_mgr_bob").set({
      workspaceId: "comercio-bauta",
      ownerId: ALICE_UID,
      createdByUid: ALICE_UID,
      actorEmail: "bob@bauta.cu",
      actorId: "actor_mgr_bob",
      actorName: "Bodega El Parque",
      localeId: "loc_1",
      localeName: "LOC-01 · Parque",
      periodYear: 2026,
      periodMonth: 11,
      grossIncomeCents: 15000000,
      operatingExpensesCents: 5000000,
      netProfitCents: 10000000,
      profitSharePercent: 30,
      profitShareCents: 3000000,
      spaceRentCents: 600000,
      totalDueCents: 3600000,
      paidCents: 3600000,
      paymentStatus: "PAGADO",
      declaredBy: "DIRECTOR",
      paymentChannel: "TRANSFERENCIA",
      paymentReference: "TM-999111",
      notes: "Registrado por oficina",
      paidAt: nowPast,
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  await assertSucceeds(
    aliceDb.collection("workspaces/comercio-bauta/payment_events").doc("evt_mgr_bob").set({
      workspaceId: "comercio-bauta",
      ownerId: ALICE_UID,
      createdByUid: ALICE_UID,
      actorEmail: "bob@bauta.cu",
      settlementId: "set_mgr_bob",
      actorId: "actor_mgr_bob",
      actorName: "Bodega El Parque",
      periodYear: 2026,
      periodMonth: 11,
      amountCents: 3600000,
      paymentChannel: "TRANSFERENCIA",
      paymentReference: "TM-999111",
      reportedBy: "DIRECTOR",
      verificationStatus: "CONFIRMADO",
      verifiedByUsername: "director1",
      clientCreatedAtMillis: Date.now() - 1000,
      createdAt: nowPast,
      updatedAt: nowPast,
    })
  );

  // Verified Bob can list all 3 linked collections by actorEmail
  await assertSucceeds(
    bobVerifiedDb.collection("workspaces/comercio-bauta/actors").where("actorEmail", "==", "bob@bauta.cu").get()
  );
  await assertSucceeds(
    bobVerifiedDb.collection("workspaces/comercio-bauta/settlements").where("actorEmail", "==", "bob@bauta.cu").get()
  );
  await assertSucceeds(
    bobVerifiedDb.collection("workspaces/comercio-bauta/payment_events").where("actorEmail", "==", "bob@bauta.cu").get()
  );

  // Verified Bob can update his own contact phone on the manager-created actor profile,
  // but CANNOT tamper with rent or active flag!
  await assertSucceeds(
    bobVerifiedDb.collection("workspaces/comercio-bauta/actors").doc("actor_mgr_bob").update({
      phone: "5355550000",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(
    bobVerifiedDb.collection("workspaces/comercio-bauta/actors").doc("actor_mgr_bob").update({
      monthlyRentCents: 0,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Unverified spoofed Bob and Carlos are denied!
  await assertFails(
    bobUnverifiedDb.collection("workspaces/comercio-bauta/actors").where("actorEmail", "==", "bob@bauta.cu").get()
  );
  await assertFails(
    carlosDb.collection("workspaces/comercio-bauta/settlements").where("actorEmail", "==", "bob@bauta.cu").get()
  );
  await assertFails(
    carlosDb.collection("workspaces/comercio-bauta/payment_events").doc("evt_mgr_bob").get()
  );
});
