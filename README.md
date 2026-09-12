# MeshTalk Android MVP

Offline nearby group chat for Android. Phones discover and connect through Google Nearby Connections (`P2P_CLUSTER`). Messages are flooded between connected peers with a unique ID, duplicate suppression, and a six-hop TTL.

## Run

1. Open this folder in Android Studio.
2. Allow Gradle Sync to finish.
3. Run on two or more physical Android phones with Google Play Services.
4. Grant Nearby Devices permissions, enter a different name on each phone, and tap **دخول للشبكة**.

No mobile data or Wi-Fi internet connection is required. For testing, keep the app open on every phone and keep the phones within Bluetooth/Wi-Fi range.

## Current MVP scope

- Automatic nearby advertising and discovery
- Multiple simultaneous peer connections
- Group messages relayed for up to six hops
- Duplicate-loop protection with `messageId`
- Android 8+ permission handling

## Important limitations

- This first build is an open local room; it does not yet provide private recipients or encryption.
- It works reliably while the app is visible. A foreground service is needed for background operation.
- Store-and-forward persistence, identity keys, message history, delivery receipts, and anti-spam controls are next-phase features.
- `INTERNET` is declared because Nearby/Google Play Services may require it internally, but the chat transport is local and does not upload messages to an app server.

Do not use this MVP for sensitive communication until end-to-end encryption and authenticated identities are implemented.
