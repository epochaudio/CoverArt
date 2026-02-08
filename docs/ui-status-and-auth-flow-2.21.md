# UI Status Visibility & Authorization Flow (v2.21)

## Background
- This app runs on an always-on art display. Local interaction should be minimal.
- The server side (Roon Core) is the primary control surface.
- The device should never feel like a black box: users must be able to *see* what's happening.
- Art Wall mode should remain visually clean (album covers only) unless something needs attention.

## Goals
1. Keep the artwork visually clean during normal operation (no status text overlay).
2. Surface only actionable exceptions (auth required / disconnected / errors), so the device never feels "dead".
3. Surface the "Enable extension in Roon" instruction reliably, even when `registry/register` response is missing/delayed.
4. Keep changes low-risk: avoid adding complex UI interaction.

## What Changed In 2.21

### 1) English UI Text
All user-facing UI/status/empty placeholders were switched to English (startup, discovery, connection, empty album, authorization prompt, etc).

### 2) Status Overlay (Alert-Only Over Artwork)
We render `statusText` as a small overlay, but **hide it by default** when artwork is being displayed.

Policy:
- **Art Wall mode**: hide status overlay unless something needs attention.
- **Single cover mode** (album art visible): hide status overlay unless something needs attention.
- Show overlay only for actionable states: authorization required, disconnected (after a short grace period), or explicit errors/warnings.

Implementation:
- `MainActivity.attachStatusOverlay()` creates a small top-left overlay container and adds `statusText` into it.
- `updateStatus(...)` now calls `refreshStatusOverlayVisibility()` after updating the state/text.

Key file:
- `app/src/main/java/com/example/roonplayer/MainActivity.kt`

### 3) Clean Artwork: Hide Normal "Connected/Zone/Subscribed/Registering"
This device is an "art display". Even if the app is connected and healthy, showing strings like:
- `Connected`
- `✅ Zone: ...`
- `Subscribed...`
- `Registering...`
would visually pollute the cover display.

Rule (current implementation):
- If we're showing artwork (Art Wall grid OR current album cover), the overlay is **alert-only**.
- Connection flaps are de-noised with a short disconnect grace window (so brief Wi‑Fi blips don't flash a banner).

Logic is centralized in:
- `MainActivity.shouldShowStatusOverlay()`
- `MainActivity.refreshStatusOverlayVisibility()`

### 4) First Pairing Authorization Hint (No Token)
Problem:
- The existing prompt to "Enable extension in Roon" only triggered when we received a `registry/register` response without a `token`.
- If that response never arrives, the user sees no instruction.

Fix:
- After `sendRegistration()`, if there is **no saved token**, schedule a watchdog.
- If pairing still hasn't completed after a short delay (currently `min(10s, ws_read_timeout_ms)`), automatically call `showAuthorizationInstructions()`.
- The job is canceled on successful pairing and on disconnect.

Functions:
- `MainActivity.sendRegistration()`
- `MainActivity.scheduleAuthorizationHintIfNeeded(...)`
- `MainActivity.showAuthorizationInstructions()`

## Known Limitations / Next Steps
1. **String-based alert detection**:
   - Today we infer "alert" states partially from the status text prefix/keywords (`❌` / `⚠️` / "enable the extension", etc).
   - Next step: introduce an explicit UI phase enum (AUTH_REQUIRED/DISCONNECTED/ERROR/READY) so UI logic never depends on strings.

2. **Registration response correlation**:
   - `registry/register` isn't correlated by Request-Id in the UI layer.
   - Next step: track the last `registerRequestId` and ignore stale/out-of-order responses.

3. **Authorization retry loop**:
   - The old retry loop is intentionally disabled to avoid duplicate in-flight registrations.
   - With proper in-flight guard + Request-Id correlation, a low-frequency retry (15-30s) in AUTH_REQUIRED state can be safely re-enabled.

4. **Toast reliability**:
   - Toast can be missed. We keep the status overlay as the primary signal.
   - Next step: optionally render a short 2-3 line persistent instruction in the main UI when AUTH_REQUIRED, while keeping Art Wall clean in steady-state.
