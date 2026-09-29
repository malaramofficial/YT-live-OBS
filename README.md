# Malaram Live Engagement Tool

Real YouTube Live control center for Android.

## What is real

- Google OAuth authorization for the user's YouTube account
- Active live-broadcast discovery through the authenticated YouTube account
- Real live-chat messages from YouTube
- Real creator chat messages sent through YouTube
- Real YouTube poll creation and closing
- Real YouTube poll tallies shown when authorized by the channel owner
- Real chat-message deletion for an authorized owner/moderator
- Q&A queue backed by Room, populated from actual YouTube chat messages
- Local OBS overlay server backed by the current app state
- No demo viewers, fake chat, simulated votes, seeded Q&A, or test-live mode

## Google Cloud setup

1. Create/select a Google Cloud project.
2. Enable **YouTube Data API v3**.
3. Create an **OAuth client ID** for Android.
4. Use this package name:

   `com.malaramofficial.ytliveobs`

5. Register the SHA-1 certificate for the APK you are installing.
   - Debug APK: register the debug signing certificate SHA-1.
   - Release APK: register the release/Play signing certificate SHA-1.
6. Install the APK and tap **Connect Google / YouTube**.
7. Choose the Google account that owns or manages the YouTube channel and grant the requested YouTube permission.

The app does not ask the user to paste an API key or OAuth token.

## Connection flow

`Connect YouTube -> Google authorization -> OAuth access token -> liveBroadcasts.list(mine=true,broadcastStatus=active) -> active video -> activeLiveChatId -> live chat/poll actions`

If authorization expires or YouTube returns 401/403, the app stops treating the account as connected and asks the user to reconnect.

## OBS overlay

Start the overlay from the **OVERLAY** tab. The app starts a local HTTP server and shows the LAN URL. Add that URL as an OBS Browser Source while the Android device and OBS machine are reachable on the same network.

## Important

The app does not contain its own video encoder/streaming engine. OBS remains responsible for the actual stream. This app controls YouTube Live engagement and provides the overlay.

## Build

The GitHub Actions workflow runs unit tests and assembles a debug APK. It also supports manual dispatch from the Actions page.
