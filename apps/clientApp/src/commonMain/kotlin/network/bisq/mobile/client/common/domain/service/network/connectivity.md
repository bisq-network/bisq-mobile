# Client connectivity

HTTP → WebSocket → health-monitor stack. **Source of truth is the code** — update this when behaviour changes.

## Key files

| File | Role |
|------|------|
| `httpclient/HttpClientService.kt` | Settings-driven `HttpClient` lifecycle |
| `utils/PlatformAbstractions.{android,ios}.kt` | `createHttpClient()`, iOS `NSURLSession` registry |
| `websocket/WebSocketClientService.kt` | WS factory, subscriptions, reconnect/recreate orchestration |
| `websocket/WebSocketClientImpl.kt` | Connect, listen, request/response, reconnect backoff |
| `service/network/ClientConnectivityService.kt` | Health polling, UI `ConnectivityStatus`, pending blocks |
| `data/.../ConnectivityService.kt` | Base status enum + 3 min RECONNECTING timeout |

```
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

How a paired client gets a live WebSocket.

```
Settings / Tor port change
        │
        ▼
HttpClientService ──► createHttpClient() ──► httpClientChangedFlow
        │
        ▼
WebSocketClientService.updateWebSocketClient()
        │
        ├─ settings unchanged ─────────────────────────────► skip
        ├─ topology same, only credentials changed ────────► skip (unless prior 401)
        ├─ sessionId or clientId blank ──────────────────────► skip (pairing)
        │
        ▼
new WebSocketClientImpl ──► collect status ──► proactive connect()
        │
        ▼
connect() [connectionMutex]
        ├─ hasLiveSession? ────────────────────────────────► done
        ▼
webSocketSession(/websocket + SESSION_ID/CLIENT_ID)
        ├─ fail ───────────────────────────────────────────► Disconnected(error)
        ▼
GET /api/v1/settings/version
        ├─ 401/403 ──► UnauthorizedApiAccessException ──► session renewal (Flow 4)
        ├─ bad version ──► IncompatibleHttpApiVersionException
        ▼
Connected ──► applySubscriptions()
```

---

## Flow 2 — Health monitor (every 5s)

`ClientConnectivityService` keeps UI status honest, especially on iOS where dead TCP can look connected.

```
checkConnectivity() every 5s (first check after 5s delay)
        │
        ├─ !isConnected()
        │       ├─ triggerReconnect()
        │       └─ iOS: 12 failed cycles ──► forceClientRecreation()
        │
        ├─ connected but connectionUntrusted (prior health check failed)
        │       ├─ health check pass ──► restore trust, derive status
        │       └─ health check fail ──► triggerReconnect(force=true)
        │
        └─ connected and trusted
                ├─ health check pass ──► derive status, run pending blocks if newly connected
                └─ health check fail ──► connectionUntrusted=true, triggerReconnect(force=true)

Status derivation (when health check passes):
        failed subscription ──► CONNECTED_WITH_LIMITATIONS
        slow RTT (Tor-aware) ──► REQUESTING_INVENTORY
        else ──► CONNECTED_AND_DATA_RECEIVED

Base class: RECONNECTING > 3 min ──► DISCONNECTED
```

Health check = `GET /api/v1/settings/version` over the WS REST proxy (`ClientConnectivityService.TIMEOUT` = 5s).

---

## Flow 3 — Reconnect & iOS force recreation

```
reconnect()  [WCS on disconnect | CCS triggerReconnect | iOS fallback]
        │
        ├─ already reconnecting + stuck > ~35s ──► cancel job, retry
        ├─ already reconnecting, not stuck ──────► return
        ▼
doDisconnect() ──► backoff (2s × 2^n, max 10s) ──► connect(min(hostTimeout, 30s))
        │
        ├─ attempts >= 5 ──► Disconnected(MaximumRetryReachedException)
        ├─ success ──► Connected
        └─ retryable error ──► reconnect() again
           401 / bad API version ──► stop (session renewal or user action)

iOS forceClientRecreation() — when reconnect alone is not enough:
        Triggers: 12 failed CCS cycles | connect timeout | "bad URL" SOCKS failure
                  (30s cooldown between recreations)

        dispose WS ──► invalidateUnderlyingSession() [NSURLSession.invalidateAndCancel]
        ──► HttpClientService.recreateClient() ──► httpClientChangedFlow
        ──► updateWebSocketClient() ──► new HttpClient + WS ──► connect()
```

Connect timeouts: 15s clearnet / 60s Tor (`WebSocketClient`). Max reconnect attempts: 5 (`WebSocketClientImpl`).

---

## Flow 4 — Session renewal vs revocation

Both 401 and 403 from the health check or API version probe surface as `UnauthorizedApiAccessException`. **Do not** `triggerReconnect(force=true)` on 401 — that reuses a stale `sessionId`.

```
401 or 403 from health check / version probe
        ▼
attemptSessionRenewal() [30s cooldown]
        ├─ success ──► new sessionId in settings ──► httpClientChangedFlow ──► new WS
        └─ renewal returns 401 ──► handleClientRevocation()
                                        │
                                        ▼
                               clear creds, disposeClient(), clientRevoked=true ──► pairing UI
```

On WS disconnect with 401, `WebSocketClientService` also calls `attemptSessionRenewal()` reactively.

---

## Rules & gotchas

1. **401 → session renewal, not force reconnect.** Force reconnect keeps the stale session and loops.
2. **Routine HttpClient swap:** `releaseUnderlyingSessionTracking()` + close — lets in-flight WS close gracefully.
3. **Forced teardown (iOS zombies):** `invalidateUnderlyingSession()` before close — cancels in-flight NSURLSession tasks immediately.
4. **`connectionUntrusted`** — after a failed health check, `isConnected()` alone is not enough (half-open TCP).
5. **`dispose()` order** — cancel reconnect job + `clientScope` before `disconnect()` (avoids ~30s mutex hang).
6. **Credentials-only settings update during Tor handshake** — intentionally skips WS recreation; 401 later triggers renewal.
7. **iOS `CancellationException` on disconnect** — treated as network error → reconnect (Darwin wraps many socket errors this way).
8. **iOS SIGSEGV** — extract `requestId` from raw JSON before deserializing sealed WS messages.
9. **`unSubscribe()`** — not implemented.
10. **Demo** — `demo.bisq:21` → `WebSocketClientDemo` (`WebSocketClientFactory`).

### WS client recreation guards (`updateWebSocketClient`)

| Guard | Effect |
|-------|--------|
| Settings identical | Skip update |
| Topology unchanged, only credentials | Skip (WS auth is handshake-scoped) |
| Prior disconnect with 401 | Bypass skip → recreate with new creds |
| Proxy mode changed (Tor ↔ clearnet) | Cancel state collector, full dispose + recreate |
| Missing sessionId/clientId | Skip WS creation (pairing) |

### Platform differences (Android vs iOS)

| | Android (OkHttp) | iOS (Darwin) |
|--|------------------|--------------|
| Dead TCP detection | Reliable | Unreliable — health checks required |
| `invalidateUnderlyingSession` | no-op | `NSURLSession.invalidateAndCancel()` |
| Tor SOCKS host | as configured | `127.0.0.1` → `localhost` (`HttpClientSettings`) |
| Zombie WS after timeout | N/A | `forceClientRecreation()` |
