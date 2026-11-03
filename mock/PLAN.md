# Eraser — Android Plan

**Status:** planning complete, no app code written. See `README.md` for current state and next action.
**Target:** Android phone, personal sideload
**Stack:** Kotlin + Jetpack Compose + Glance widgets + Room + WorkManager
**Core idea:** a companion app that puts your Blackboard due dates and announcements on the home screen, lets you triage them, and lets you add your own deadlines.

> Design values, widget specs and the announcement-sheet interaction live in `DESIGN.md`.
> Tooling and build order live in `BUILD-GUIDE.md`.

**Confirmed inputs:**
- Sign-in is **school username + password** (no Google/Microsoft SSO) → embedded WebView login is viable.
- A **Blackboard ICS calendar feed exists** and already works with Google Calendar.
- Scope is **both**: home-screen widgets *and* in-app browsing.
- Widget items must be **dismissible when you're done with them**.

---

## 0. What "expand the Play Store app" actually means

We are **not** forking the official Blackboard Learn app. It's proprietary, obfuscated, and ships no source. There's no legal or practical path to injecting widgets into it.

This is a **companion app** that:

1. Reads your due dates from the **ICS feed** (no login needed).
2. Reads **announcements and course structure** from Blackboard's web UI once you sign in.
3. Caches everything locally in Room.
4. Renders **home-screen widgets** you can triage — done, snooze, dismiss.
5. Lets you **add your own due dates** and **override wrong/missing Blackboard dates**.
6. Later folds in Cengage and Coursera.

The increment over the official app is: real widgets, local triage state, custom items, cross-platform unification. We don't rebuild course-content browsing or push — the official app still does those.

---

## 1. The key insight: due dates need no auth

You already have an ICS feed. That changes the build order dramatically.

| Source | Needs login? | Gives you | Stability |
|---|---|---|---|
| **ICS feed** | **No** | Every due date on your calendar | High — designed for machine consumption |
| Blackboard web UI (authenticated) | Yes | Announcements, courses, item detail | Medium — HTML drift |
| Blackboard mobile REST API | Yes (+ unofficial client) | Everything, cleanly | Medium-low |
| Cengage / Coursera (WebView) | Yes | Third-party assignments | Low — brittle |

**Consequence:** you get a genuinely useful, working home-screen widget **before writing a single line of auth code**. Auth only unlocks announcements and in-app browsing.

So the plan is deliberately ordered: **ICS → widgets → triage → custom items → auth → announcements → platforms.**

---

## 2. Architecture

```
┌────────────────────────────────────────────────────────┐
│  HOME SCREEN                                           │
│  Glance widgets: DueSoon · Today · Announcements ·     │
│  Course · QuickAdd                                     │
└───────────────▲────────────────────────────────────────┘
                │ updateAll() after sync; Room Flow queries
┌───────────────┴────────────────────────────────────────┐
│  REPOSITORY  (single source of truth)                  │
│  Room: courses · items · item_state · local_items ·    │
│        announcements · sync_state                      │
└───────────────▲────────────────────────────────────────┘
                │
┌───────────────┴────────────────────────────────────────┐
│  SYNC ENGINE  (WorkManager)                            │
│  IcsSource        — always on, no auth                 │
│  BlackboardHtml   — after login                        │
│  Cengage / Coursera — later                            │
│  → normalize → dedupe → upsert → diff → notify         │
└───────────────▲────────────────────────────────────────┘
                │ HTTPS (OkHttp)
┌───────────────┴────────────────────────────────────────┐
│  AUTH (only for announcements + browsing)              │
│  WebView SSO → cookie jar → EncryptedSharedPrefs       │
└────────────────────────────────────────────────────────┘
```

### Stack

| Concern | Choice | Why |
|---|---|---|
| Language / UI | Kotlin + Jetpack Compose | Standard; widgets interop cleanly |
| Widgets | **Glance** (`androidx.glance:glance-appwidget`) | Compose-style widget API, maintained |
| Local DB | Room + KSP | Reactive queries feed widgets directly |
| Background | WorkManager | Survives reboot, respects Doze |
| HTTP | OkHttp | Simple; no Retrofit ceremony needed |
| ICS parsing | **biweekly** | Handles `VTIMEZONE`/`TZID` correctly — naive parsers get DST wrong |
| HTML parse | Jsoup | For announcements/courses |
| Settings | DataStore | Async, no SharedPreferences footguns |
| Secrets | EncryptedSharedPreferences + Keystore | ICS token, cookies |
| Exact reminders | AlarmManager + `SCHEDULE_EXACT_ALARM` | WorkManager isn't exact enough for "10 min before" |

**minSdk 26, targetSdk 35.**

---

## 3. Data model (Room)

```kotlin
@Entity Course(
  id: String,              // "bb:_12345_1"
  platform: String,        // blackboard | cengage | coursera
  code: String,            // "CS 3410"
  title: String,
  term: String,
  instructor: String?,
  colorArgb: Int,
  url: String,
)

@Entity Item(              // Blackboard-sourced, read-only mirror
  id: String,              // stable: "ics:<uid>" or "bb:<sourceId>"
  courseId: String?,
  kind: String,            // assignment|quiz|exam|discussion|reading|lecture|announcement
  title: String,
  body: String?,
  dueAt: Long?,            // epoch millis UTC
  dueAtRaw: String?,       // what the source actually said — debug timezone bugs
  dueHasTime: Boolean,     // all-day vs timed
  points: Double?,
  status: String?,         // not-started|submitted|graded|late
  url: String,
  confidence: String,      // high|medium|low
  contentHash: String,
  firstSeen: Long,
  lastSeen: Long,
  removedAt: Long?,        // soft delete — never hard-delete, state must survive
)

@Entity ItemState(         // ← local triage state. One row per item. Never written back.
  itemId: String,          // PK, FK → Item.id
  doneAt: Long?,           // checked off
  snoozedUntil: Long?,     // hidden from widgets until then
  dismissedAt: Long?,      // hidden from widgets permanently (still in app)
  note: String?,
  dueOverride: Long?,      // ← your correction of a wrong/missing BB date
  overrideNote: String?,
  updatedAt: Long,
)

@Entity LocalItem(         // ← your own deadlines, no Blackboard counterpart
  id: String,              // UUID
  courseId: String?,       // null = personal
  title: String,
  notes: String?,
  dueAt: Long,
  dueHasTime: Boolean,
  rrule: String?,          // optional recurrence, e.g. weekly study block
  reminderOffsets: String, // "1440,120,10" minutes
  colorArgb: Int?,
  doneAt: Long?,
  createdAt: Long,
)

@Entity Announcement(
  id: String,
  courseId: String,
  title: String,
  body: String?,
  postedAt: Long,
  readLocally: Boolean,    // local only
  url: String,
)

@Entity SyncState(
  source: String,          // PK
  lastSuccessAt: Long?,
  lastAttemptAt: Long?,
  status: String,          // ok|needs_reauth|error|partial
  message: String?,
  itemCount: Int,
)
```

### Three schema decisions that matter

1. **`ItemState` is a separate table.** Sync writes `Item`; the user writes `ItemState`. They never collide, so a sync can never clobber your dismissal or date correction.
2. **`removedAt` instead of delete.** If Blackboard drops an item and re-adds it, your "dismissed" state must survive. Soft delete, and never change `Item.id` for the same source item.
3. **`dueAtRaw` + `dueHasTime`.** All three platforms render dates differently and some are all-day. Keep the raw string; parse with the school's timezone from settings, not the phone's. This is the difference between a 5-minute bug fix and an archaeology project.

**Effective due date** = `itemState.dueOverride ?: item.dueAt`.

---

## 4. The triage model — "dismiss it when I'm done"

This is a core product concept, not a checkbox. Four distinct actions, each with different semantics:

| Action | State | Widget behavior | App behavior | Reversible |
|---|---|---|---|---|
| **Done** | `doneAt` | Disappears immediately | Strikethrough, collapsed under "Done" | Yes |
| **Snooze** | `snoozedUntil` | Hidden until time T, then reappears | Shows "snoozed until…" | Yes |
| **Dismiss** | `dismissedAt` | Hidden permanently | Moves to "Dismissed" section | Yes |
| **Open** | — | Launches detail / Blackboard | — | — |

**Rules:**
- All four are **local only**. Nothing is ever written back to Blackboard.
- State survives sync, app restart, and reboot.
- If an item's content changes after you dismiss it (e.g. the due date moves), **resurface it** — a dismissed item that changes is news.
- Dismissing an item cancels its pending notification.

**Widget interaction (widgets can't do rich gestures):**

Widgets don't reliably support swipe-to-dismiss across launchers, so don't depend on it. Give each row explicit targets:

- **Tap row** → open item detail
- **Tap checkbox** → Done
- **Tap `⋯`** → launches a small transparent dialog activity: *Snooze 1h / Tonight / Tomorrow · Dismiss · Open in Blackboard*

Snooze and dismiss are one tap deep, which is the difference between a widget you use and one you ignore.

**Why "dismiss" ≠ "done":** you'll want to hide a reading you're skipping without pretending you did it, and you'll want a 3-day-old announcement to stop occupying a row. Conflating them makes the widget lie to you.

---

## 5. Widgets

| Widget | Shows | Size | Refresh |
|---|---|---|---|
| **Due Soon** | Next 5–7 active items: course chip, title, relative time ("in 3h", "tomorrow 11:59 PM"), checkbox, `⋯` | 4×2, resizable | After sync + hourly fallback |
| **Today** | Today only: all-day, timed, custom items | 2×2 / 4×2 | After sync |
| **Announcements** | Latest 3–5, unread bolded, course + age, `⋯` to dismiss | 4×2 / 4×4 | After sync |
| **Course** | One course, configurable per widget instance | 2×2 / 4×2 | After sync |
| **Quick Add** | 1×1 tile → "new custom due date" sheet | 1×1 | n/a |

**Implementation notes:**
- `GlanceAppWidget` + `GlanceAppWidgetReceiver` per type.
- `LazyColumn` for scrollable lists — verify on your launcher/API level; fall back to a fixed top-N `Column` if it misbehaves.
- **Update:** call `updateAll(context)` at the end of every successful sync. Set `updatePeriodMillis` only as a 30-min floor; the system ignores it in Doze.
- **Tap actions:** `actionStartActivity` with deep links (`eraser://item/<id>`, `eraser://announcement/<id>`).
- **Announcement tap** opens a translucent `TrampolineActivity` hosting the shared `ModalBottomSheet` — the app never launches. See `DESIGN.md` §7.
- **Never do network work from a widget broadcast.** Widget taps enqueue a local write + a refresh.
- **Three error states, always:** *Session expired — tap to sign in*, *Sync failed — last updated 3h ago*, *No items*. Silent emptiness is how these apps lose your trust.
- **Resize pass:** test at 2×1, 4×1, 4×4, and with large system font sizes.

---

## 6. Auth (only for announcements + browsing)

Username/password means embedded WebView is viable. Two paths, in order:

### Path A — Embedded WebView (primary)
- Load the login URL in a `WebView`, let you complete login (+ MFA if your school has it), capture cookies via `CookieManager`.
- Simple, no redirect plumbing.
- **Caveat:** if your school later adds Duo/Okta device trust or a CAPTCHA, the WebView may be refused. Detect the failure and fall back.

### Path B — Custom Tabs + app link (fallback)
- Open login in Chrome Custom Tabs; redirect back to `eraser://auth/callback`.
- More plumbing (intent filters, state/nonce) but survives stricter IdPs.

### Handling
- Cookies → `CookieJar` backed by EncryptedSharedPreferences.
- Every sync hits a cheap authenticated endpoint first. On 401/redirect-to-login: set `SyncState.status = needs_reauth`, skip that source, notify, put the widget in its "needs login" state. **Never silently emit a half-empty schedule.**
- Expect re-auth every 1–4 weeks depending on the session policy.
- **The ICS feed keeps working even when auth expires** — due dates are never blocked by a login problem. That's a strong argument for ICS as the backbone.

---

## 7. ICS handling (the backbone)

- Store the feed URL encrypted; it's effectively a bearer token.
- Fetch with OkHttp, parse with **biweekly** (handles `VTIMEZONE`).
- Stable IDs from the `UID` property — that's what lets dismissal state survive.
- Some feeds omit course codes; derive from `CATEGORIES` or the summary prefix, and let the user map feeds → courses once.
- **Token rotation:** if the feed 403s, prompt to re-copy the link from Blackboard (Profile → Calendar → Get External Calendar Link). Show it as a one-line settings fix, not an error screen.
- **Advantage over Google Calendar:** Google polls every 12–24h; the app can poll hourly and compute precise relative times.

---

## 8. Sync engine

- `PeriodicWorkRequest`, 1–3h (configurable), constraints: network connected, battery not low.
- Expedited one-off sync on app open and on widget tap.
- Exponential backoff on failure; hard stop + notification on `needs_reauth`.
- Rate limit authenticated requests: 1 per 2–3s with jitter. It's someone's production server.
- **Diff after each sync:** new / changed / removed. Due-date changes are the killer feature — "professor moved the deadline" is exactly what the widget should surface.
- Battery: request optimization exemption only if you actually observe missed syncs. Don't ask on first launch.

---

## 9. Custom items & overrides

**Create (`LocalItem`):** title, optional course, date/time, notes, reminder offsets, optional weekly recurrence. Appears in Due Soon / Today / in-app list, visually distinguished by accent color.

**Override (`ItemState.dueOverride`):** long-press a Blackboard item → "Set my own date." Shown with a "modified" marker and a one-tap revert.

**Reminders:** per-item offsets (1 day / 2 h / 10 min). `AlarmManager.setExactAndAllowWhileIdle` when `SCHEDULE_EXACT_ALARM` is granted, else degrade to WorkManager delays. Request `POST_NOTIFICATIONS` when the user first adds a reminder, not at launch.

**No write-back.** Even if the API allowed it, mutating graded content is a bad idea. Local-only, clearly labeled.

---

## 10. Phases

### Phase 0 — Toolchain (≈1–2 h + big download)
Nothing dev-related is installed (no Node, Python, Git, or JDK on PATH; `winget` works).
- `winget install Google.AndroidStudio` (bundles JDK 17 + SDK manager)
- Install platform-tools, API 35, an emulator image; accept licenses
- `winget install Git.Git`
- `.gitignore`: `local.properties`, `.idea/`, `*.keystore`, `*.jks`
- **Exit:** `adb devices` sees your phone; empty app runs.

### Phase 1 — Discovery (≈3 h)
1. Grab the ICS URL; save a sample `.ics`; inspect `UID`, `TZID`, `CATEGORIES`.
2. Log in on desktop; record whether it's Ultra (`/ultra/`) or Original (`/webapps/portal`).
3. Capture the announcements page HTML for one course.
4. Check whether `/learn/api/public/v1/users/me` answers with a plain session cookie or demands a Bearer token.
5. Note whether login involves MFA (changes WebView expectations).
- **Exit:** `discovery/report.md` + fixtures (`sample.ics`, `announcements.html`).

### Phase 2 — Skeleton + ICS + due list (≈2 days, **no auth**)
- Compose shell, navigation, settings (ICS URL, term, timezone).
- Room schema from §3, `IcsSource`, parser + fixture tests.
- In-app due list grouped overdue / today / this week / later.
- **Exit:** real deadlines in the app, correct across DST, surviving reboot.

### Phase 3 — Due Soon widget + triage (≈2 days)
- Glance widget, relative time, checkbox, `⋯` dialog.
- `ItemState` writes: done / snooze / dismiss.
- `updateAll()` wired to sync; three error states.
- **Exit:** it's on your home screen, you can dismiss an item, and it stays dismissed after a sync.

### Phase 4 — Custom items + Quick Add (≈2 days)
- `LocalItem` CRUD, recurrence, reminder offsets, exact alarms.
- Quick Add tile.
- **Exit:** "Study for midterm, Thu 7 PM" sits next to Blackboard items and notifies you.

### Phase 5 — Auth + announcements (≈2 days)
- WebView login, cookie jar, `needs_reauth` handling.
- `BlackboardHtml` source: fetch + Jsoup-parse announcements.
- **Exit:** kill the app, reopen, still signed in; announcements listed.

### Phase 6 — Announcements widget + notifications (≈2 days)
- Announcements widget with dismiss.
- Local notification for new announcements; cancel on dismiss.
- **Exit:** a new announcement appears on the home screen without opening anything.

### Phase 7 — In-app browsing (≈2 days)
- Course list, course detail (items + announcements), item detail with both links.
- "Open in Blackboard" deep link.
- **Exit:** you can browse a course and tap through to Blackboard.

### Phase 8 — Cengage (≈3 days)
- LTI launch in WebView with the BB session; scrape MindTap/WebAssign assignment lists.
- Per-platform session state + re-auth detection.
- **Exit:** Cengage deadlines appear, deduped against Blackboard.

### Phase 9 — Coursera (≈3 days)
- Same pattern; prefer any JSON endpoint found in Phase 1 over DOM scraping.

### Phase 10 — Polish (ongoing)
- Drift alarm: 0 items where there were 10 → notify loudly.
- Widget sizing/dark-mode/accessibility pass.
- Selector health check after Blackboard updates.

---

## 11. Risks & mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| ICS feed token rotates / feed disappears | Med | High | Clear re-paste prompt; keep cached items so the app degrades, not breaks |
| HTML drift after Blackboard update | High | Med | Jsoup selectors isolated per page type; fixture tests; loud 0-item alert |
| MFA/CAPTCHA blocks WebView | Med | Med | Detect, fall back to Custom Tabs; ICS keeps working meanwhile |
| Doze/battery kills sync | High | Med | WorkManager + `updateAll()` on open; optional exemption |
| Widget update throttling | Med | Low | Update from sync, not a timer |
| Exact alarms denied (Android 12+) | Med | Low | Degrade to inexact; explain in settings |
| Cengage anti-bot / CAPTCHA | Med | Med | Real WebView, human pacing; **stop** if CAPTCHA appears |
| Duplicate items across platforms | High | Med | Blackboard/ICS canonical; match normalized title + due within ±24h + same course |
| Dismissed item resurrects after sync | High | High | Stable IDs + `ItemState` table + soft delete; explicit test |
| ToS / AUP complaint | Low | High | Read-only, own account, personal use, rate-limited, never published |
| Cookie/ICS token leakage | Low | High | EncryptedSharedPreferences, Keystore, never logged, no analytics |

---

## 12. Distribution

- Sideload: `adb install -r app-debug.apk`, or a private GitHub Releases page.
- Neutral package name (`dev.<you>.eraser`), neutral launcher label. No "Blackboard" in the name or icon — trademark, and it implies affiliation.
- Repo private. No Play Store.
- If you ever publish: strip all Blackboard branding; unofficial API use is the kind of thing that gets apps removed.

---

## 13. Remaining open questions

Still unanswered as of this writing. Answer them in `README.md` §"Open questions", then fold the
answers into §14.

1. **Android version + device?** Affects widget collections, exact alarms, Glance `LazyColumn` behavior.
2. **Does your school's login use MFA** (Duo/Okta push)? Changes WebView expectations.
3. **Noise level:** everything (readings, lectures) or only graded/due items?
4. **Which term(s)?** Current only, or all active?
5. **Do ICS events carry course codes**, or will we need a one-time course mapping?

---

## 14. Decision log

| # | Decision | Rationale |
|---|---|---|
| 1 | Companion app, not a fork | Official app is proprietary; no injection path |
| 2 | **ICS first, auth later** | Due dates work with zero login risk; ships a widget in week one |
| 3 | Kotlin + Glance | Native widgets are the product; cross-platform frameworks do them worse |
| 4 | `ItemState` as a separate table | Sync must never clobber your dismissals or date corrections |
| 5 | Four-state triage (done/snooze/dismiss/open) | "Dismissed" ≠ "done"; conflating them makes the widget lie |
| 6 | Soft delete + stable IDs | Dismissal must survive an item disappearing and reappearing |
| 7 | Read-only toward Blackboard | Bounded risk; nothing to break |
| 8 | Sideload only | Trademark + ToS |
| 9 | Name: **Eraser**, package `dev.<you>.eraser` | Trademark-clear; works as noun, verb, and widget label |
| 10 | Build the widget **fourth**, not first | Glance is the least-documented layer; the data layer must exist first |
| 11 | Announcements open in a **bottom sheet**, not a dialog | Full text needs scroll; sheet is the M3 pattern. Hosted by a translucent `TrampolineActivity` from widget taps |

---

## 15. Definition of done

- [ ] ICS deadlines render correctly, including DST and all-day items
- [ ] Due Soon widget on the home screen with correct relative times
- [ ] An item can be done / snoozed / dismissed, and **stays that way after sync and reboot**
- [ ] A changed due date on a dismissed item resurfaces it
- [ ] Custom due dates coexist with Blackboard items and notify correctly
- [ ] Announcements appear on the home screen without opening the app
- [ ] Sync runs unattended for two weeks without manual intervention
- [ ] No duplicates, no wrong dates, no silent empty states

---

## 16. Name

All of these are trademark-clear and avoid "Blackboard" entirely.

| Name | The joke | Package |
|---|---|---|
| **Eraser** ⭐ | Literally the app's job: erase what's done. One word, dry humor, and the icon is a blackboard eraser — a nod without the name. | `dev.you.eraser` |
| **Chalked** | Student slang for doomed/exhausted. Board-adjacent, deniable. | `dev.you.chalked` |
| **Due Process** | Legal pun on due dates. Sounds official. Isn't. | `dev.you.dueprocess` |
| **Board Stiff** | "Bored stiff." | `dev.you.boardstiff` |
| **Later** | The most honest answer to every notification. | `dev.you.later` |

**Recommended: Eraser.** It works as a noun, a verb, and a widget label — *"Eraser · 3 due today"* — and the tagline writes itself.

> *Erase it when you're done.*
