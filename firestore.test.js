const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_arun_1";
const BOB_UID = "bob_arun_2";

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

function validProfilePayload(uid, now) {
  return {
    userId: uid,
    displayName: "Arun Listener",
    autoSync: true,
    ytMusicConnected: true,
    ytMusicAccount: "arun@youtube.com",
    spotifyConnected: true,
    spotifyAccount: "arun_spotify",
    linkedApps: ["Apple Music", "SoundCloud"],
    streamQuality: "HIGH",
    downloadQuality: "LOSSLESS",
    glassIntensity: "BALANCED",
    eqPreset: "FLAT",
    eqBands: [0, 0, 0, 0, 0],
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write any user data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("playlists").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("library_tracks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("play_history").get());
});

test("UserProfile: owner create, partial update, cross-user denial, and invalid enum/bounds rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      eqPreset: "BASS_BOOST",
      eqBands: [6, 4, 0, 1, 2],
      autoSync: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid quality enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      streamQuality: "ULTRA_MEGA",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      autoSync: true,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject Bob reading or updating Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Reject listing /users collection
  await assertFails(aliceDb.collection("users").get());
});

test("Playlists: owner CRUD succeeds, @DocumentId id field rejected, cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_chill").set({
      userId: ALICE_UID,
      name: "Malam Santai",
      description: "Lagu ambient dan lo-fi pilihan",
      trackIds: ["trk_midnight_01", "trk_emerald_02"],
      coverKey: "midnight",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_chill").update({
      name: "Malam Santai v2",
      trackIds: ["trk_midnight_01"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject redundant id key
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_bad").set({
      id: "pl_bad",
      userId: ALICE_UID,
      name: "Invalid",
      description: "",
      trackIds: [],
      coverKey: "midnight",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("playlists").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_chill").delete());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_chill").delete()
  );
});

test("LibraryTracks & PlayHistory: owner sync succeeds, immutable history update rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library_tracks").doc("trk_midnight_01").set({
      userId: ALICE_UID,
      trackId: "trk_midnight_01",
      liked: true,
      savedAlbum: false,
      downloaded: true,
      playCount: 3,
      totalListenSec: 420,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library_tracks").doc("trk_midnight_01").update({
      savedAlbum: true,
      playCount: 4,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("play_history").doc("hist_1").set({
      userId: ALICE_UID,
      trackId: "trk_midnight_01",
      trackTitle: "Kaca Malam",
      artistName: "Arun Collective",
      genre: "Electronic",
      platform: "SPOTIFY",
      listenedSeconds: 185,
      playedAt: created,
    })
  );

  // History entries are append-only
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("play_history").doc("hist_1").update({
      listenedSeconds: 999,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("play_history").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("play_history").get()
  );
});

test("ImportedTracks: owner Spotify import create/update/list succeeds, cross-user & invalid duration rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("imported_tracks").doc("sp_4cOdK2wGLETKBW3PvgPWqT").set({
      userId: ALICE_UID,
      trackId: "sp_4cOdK2wGLETKBW3PvgPWqT",
      title: "Midnight Groove",
      artist: "Spotify Artist",
      album: "Imported Singles",
      genre: "Spotify Import",
      durationSec: 210,
      artworkUrl: "https://i.scdn.co/image/ab67616d0000b273sample",
      previewUrl: "",
      spotifyUri: "spotify:track:4cOdK2wGLETKBW3PvgPWqT",
      youtubeVideoId: "dQw4w9WgXcQ",
      youtubeTitle: "Midnight Groove (Official Audio)",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("imported_tracks").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("imported_tracks").doc("sp_4cOdK2wGLETKBW3PvgPWqT").update({
      album: "Updated Album",
      durationSec: 235,
      youtubeVideoId: "abc123XYZ00",
      youtubeTitle: "Midnight Groove (YouTube Full)",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("imported_tracks").doc("sp_bad").set({
      userId: ALICE_UID,
      trackId: "sp_bad",
      title: "Bad Track",
      artist: "Artist",
      album: "Album",
      genre: "Pop",
      durationSec: 2,
      artworkUrl: "",
      previewUrl: "",
      spotifyUri: "",
      youtubeVideoId: "",
      youtubeTitle: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("imported_tracks").get()
  );
});
