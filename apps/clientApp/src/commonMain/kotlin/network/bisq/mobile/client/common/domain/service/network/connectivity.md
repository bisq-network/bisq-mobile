# Client connectivity

HTTP → WebSocket → health-monitor stack for the client app.

Source of truth is the code. Update this file whenever connectivity behaviour changes so reviewers can skim the diff here for a high-level picture of what changed.

## Key files

| File | Role |
|------|------|
| `httpclient/HttpClientService.kt` | Settings-driven `HttpClient` lifecycle; `recreateClient()` for iOS recovery |
| `httpclient/HttpClientSettings.kt` | Tor proxy defaults, `sessionExpiresAt`; iOS maps proxy host `127.0.0.1` → `localhost` |
| `access/session/SessionValidity.kt` | Session expiry checks (15m min remaining, cold-start deferral) |
| `utils/PlatformAbstractions.{android,ios}.kt` | `createHttpClient()`, TLS/proxy wiring |
| `websocket/WebSocketClientService.kt` | WS factory, subscriptions, reconnect/recreate orchestration |
| `websocket/WebSocketClientImpl.kt` | Connect, listen, request/response, reconnect backoff |
| `websocket/WebSocketClient.kt` | Connect timeouts: 15s clearnet / 60s `.onion` |
| `service/network/ClientConnectivityService.kt` | Health polling, derives raw status, calls `setConnectivityStatus()` |
| `data/.../KmpTorService.kt` | Tor runtime; `signalNewNym()` on force recreation (Tor proxy only) |
| `data/.../ConnectivityService.kt` | Shared `ConnectivityStatus` enum, `setConnectivityStatus()`, RECONNECTING timeout |

```text
Settings + Tor ──► HttpClientService ──► httpClientChangedFlow
                              │
                              ▼
                   WebSocketClientService ──► WebSocketClientImpl
                              │
                              ▼
                   ClientConnectivityService ──► ConnectivityStatus (UI)
```

---

## Flow 1 — Initial connect

```text
Settings / Tor port change
        │
        ▼
HttpClientService ──► createHttpClient() ──► httpClientChangedFlow
        │
        ▼
WebSocketClientService.updateWebSocketClient()
        │
        ├─ currentClient != null AND settings == previousSettings
        │       └─► return (keep live client; duplicate httpClientChangedFlow)
        │
        ├─ live client healthy, topology unchanged, sessionId changed, ≥15m left
        │       └─► return (keep WS; HTTP gets new sessionId from settings)
        │
        ├─ proxy mode changed (Tor ↔ clearnet) ──► cancel stateCollectionJob
        │
        ▼
dispose prior client (if any) ──► _connectionState = Disconnected
        │
        ├─ sessionId or clientId blank ──► return (pairing; no new WS yet)
        │
        ├─ no live client AND session expired / expiring within 15m / expiry unknown
        │       └─► return (wait for bootstrap session POST)
        │
        ▼
new WebSocketClientImpl + currentClientSettings = settings
        │
        ▼
collect webSocketClientStatus ──► proactive connect(determineTimeout(host))
        │
        ▼
connect() [connectionMutex]
        ├─ already Connected ─────────────────────────────► return
        ▼
webSocketSession(/websocket + SESSION_ID/CLIENT_ID headers)
        ├─ fail ───────────────────────────────────────────► Disconnected(error)
        ▼
withTimeout(remainingTime after WS handshake)
        GET /api/v1/settings/version (API compatibility)
        ├─ 401/403 ──► UnauthorizedApiAccessException
        ├─ incompatible version ──► IncompatibleHttpApiVersionException
        ▼
Connected ──► applySubscriptions()
```

Note: Pairing still waits for credentials before creating a WS. A bootstrap `sessionId` rotation alone does not always dispose the live client — see skip branch above. After 401/403, recreation runs as before.

External callers (`ClientApplicationBootstrapFacade`, `TrustedNodeSetupUseCase`) can call `WebSocketClientService.connect()` directly. Uses `clientUpdateMutex` snapshot (or `getWsClient()` fallback), then `WebSocketClientImpl.connect(determineTimeout(host))`.

---

## Flow 2 — Health monitor (every 5s)

`ClientConnectivityService.startMonitoring()` — 5s period. Default start delay: 5s clearnet, 20s Tor (two-phase: wait 5s for settings, then read `WebSocketClientService.isTorProxy`, then +15s if Tor).

```text
checkConnectivity() loop
        │
        ├─ !isConnected() OR connectionUntrusted
        │       ├─ !isConnected()
        │       │       ├─ triggerReconnect() if not already connected at WCS layer
        │       │       └─ 12 consecutive cycles ──► forceClientRecreation()
        │       │
        │       └─ isConnected() but untrusted (prior health check failed)
        │               ├─ health check pass ──► restore trust, derive status
        │               └─ health check fail ──► forceReconnect(); may forceClientRecreation()
        │
        └─ connected and trusted
                ├─ health check pass ──► derive status
                └─ health check fail ──► connectionUntrusted=true, forceReconnect()

Status derivation (when health check passes):
        failed subscription topics ──► CONNECTED_WITH_LIMITATIONS
        avg RTT > threshold (≥4 samples) ──► REQUESTING_INVENTORY
              (500ms clearnet, 3000ms Tor)
        else ──► CONNECTED_AND_DATA_RECEIVED

        ▼
setConnectivityStatus(rawStatus)   // base class applies RECONNECTING rules (Flow 2b)
        ▼
(log transition if status changed)

On unhandled errors in checkConnectivity ──► setConnectivityStatus(DISCONNECTED)
```

Health check = `GET /api/v1/settings/version` via WS REST proxy (`sendHealthCheck`, `TIMEOUT` = 5s). Sent with `awaitConnection = false`: if the WS is already disconnected the check returns `false` immediately without waiting for reconnect. 401/403 in the response body throw `UnauthorizedApiAccessException` (Flow 4).

Subclasses must publish status via `setConnectivityStatus()` (not `_status` directly). `NodeConnectivityService` does the same when mapping inventory/connection state.

---

## Flow 2b — RECONNECTING status & 3-minute timeout (`ConnectivityService`)

Added in `ConnectivityService`: UI can stay in `RECONNECTING` while the WS stack keeps retrying, but prolonged reconnecting is capped.

```text
setConnectivityStatus(RECONNECTING)
        │
        ├─ isReconnectingTimedOut already true
        │       └─► keep / set DISCONNECTED (ignore repeated RECONNECTING from monitor)
        │
        ├─ already RECONNECTING (no change)
        │       └─► return (timeout job keeps running)
        │
        ▼
_status = RECONNECTING; start serviceScope job: delay(maxReconnectingDurationMs)
        │
        ├─ any non-RECONNECTING setConnectivityStatus before delay ends
        │       └─► cancel job; clear isReconnectingTimedOut; apply new status
        │
        └─ still RECONNECTING after delay (default 3 min, MAX_RECONNECTING_DURATION_MS)
                └─► isReconnectingTimedOut = true; _status = DISCONNECTED
                    (WS may still retry; UI shows disconnected / “unable to connect”)
```

After timeout, health polls may still compute `rawStatus = RECONNECTING`, but `setConnectivityStatus` maps those calls to `DISCONNECTED` until a non-`RECONNECTING` status is set (e.g. health succeeds → `CONNECTED_AND_DATA_RECEIVED`).

---

## Flow 3 — Reconnect & force recreation

```text
reconnect()  [CCS triggerReconnect/forceReconnect | WCS on abnormal disconnect]
        │
        ├─ already reconnecting AND connect phase > 30s ──► cancel stuck reconnectJob
        ├─ already reconnecting, not stuck ─────────────► return (no parallel reconnect)
        ▼
doDisconnect() ──► exponential backoff (2s × 2^n, cap 10s)
        ▼
reconnectAttempts >= 5 ? ──► Disconnected(MaximumRetryReached); counter reset; end job
        │                      (CCS may triggerReconnect() later for a fresh 5-attempt cycle)
        ▼
connect(min(determineTimeout(host), 30s))   // 30s cap even for 60s Tor hosts
        │
        ├─ success ──► Connected
        ├─ retryable error ──► reconnect() again (invokeOnCompletion, same job)
        └─ 401 / bad API version ──► stop auto-retry in this job

WCS triggerReconnect(): only calls reconnect() when WCS reports not connected.

forceClientRecreation() (CCS after 12 failed cycles, iOS + Android):
        dispose WS ──► cancel state collector ──► HttpClientService.recreateClient()
        ├─ Tor proxy ──► KmpTorService.signalNewNym() (fresh circuits)
        └─► httpClientChangedFlow ──► updateWebSocketClient() ──► new client + connect()
```

Constants (`WebSocketClientImpl`): `STALE_RECONNECT_THRESHOLD_MS` = 30s; `MAX_RECONNECT_ATTEMPTS` = 5; `RECONNECT_CONNECT_TIMEOUT` = 30s.

Details omitted here: WCS also calls `reconnect()` from its status collector on some disconnects, and skips others via `shouldAttemptReconnect()` (see `WebSocketClientService.kt`).

---

## Flow 4 — Session renewal vs revocation

401/403 from health check or API version step → `UnauthorizedApiAccessException`.

```text
Health check 401/403 OR WS disconnect with UnauthorizedApiAccessException
        ▼
attemptSessionRenewal() [30s cooldown; needs SessionService + SensitiveSettingsRepository]
        ├─ success ──► settingsRepo.update(sessionId, sessionExpiresAt) ──► httpClientChangedFlow
        │       └─► new WS unless healthy live client skip applies (Flow 1)
        └─ renewal 401/403 ──► clear creds + sessionExpiresAt, disposeClient(), clientRevoked=true

CCS on health-check 401: attemptSessionRenewal() — NOT forceReconnect() (stale sessionId).
Bootstrap session POST also persists `sessionExpiresAt` (same as renewal).
```

---

## Rules & gotchas

1. 401 → session renewal, not force reconnect on the health-check path.
2. `connectionUntrusted` — after a failed health check, `isConnected()` alone is not trusted (half-open TCP, e.g. desktop RST).
3. `triggerReconnect` vs `forceReconnect` — former only if not connected; latter always runs `reconnect()` (stale TCP while status says connected).
4. Proxy mode change (`isTorProxy` / `externalProxyUrl`) — cancel state collector before disposing the old client.
5. Identical `HttpClientSettings` — skip WS replacement (avoids churn during Tor startup duplicates).
6. Healthy live WS + unchanged topology — bootstrap `sessionId` rotation may skip recreation when ≥15m session remains.
7. Cold start — defer WS creation when persisted session is expired, expiring within 15m, or expiry unknown.
8. Pairing — blank `sessionId`/`clientId` skips WS creation until credentials exist.
9. iOS `CancellationException` on disconnect — reconnect only when cause message contains `"Socket is not connected"`; other cancellations are not auto-retried.
10. iOS SIGSEGV guard — extract `requestId` from raw JSON before deserializing sealed WS messages.
11. `unSubscribe()` — not implemented (logs warning).
12. Demo — host `demo.bisq:21` → `WebSocketClientDemo` (`WebSocketClientFactory`).

### Platform differences (Android vs iOS)

| | Android (OkHttp) | iOS (Darwin) |
|--|------------------|--------------|
| Dead TCP detection | More reliable | Often needs health checks |
| Stuck reconnect recovery | `triggerReconnect` / backoff + `forceClientRecreation()` after 12×5s | same |
| Tor SOCKS in settings | `127.0.0.1:port` as configured | `127.0.0.1` normalized to `localhost` in `HttpClientSettings` |
| HTTP client hard reset | `disposeClient()` / settings-driven recreate | `recreateClient()` closes and re-emits settings |
