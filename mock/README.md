# Eraser

> *Erase it when you're done.*

An Android companion app for Blackboard: home-screen widgets for due dates and announcements,
local triage (done / snooze / dismiss), and your own custom deadlines. Material 3, Pixel-native.

**Status:** planning complete, zero lines of app code written.
**Not** a fork of the official Blackboard app — a read-only companion that sits next to it.

---

## Read this first

If you are a human picking this up, or an AI starting a new context, read in this order:

| # | File | Why |
|---|---|---|
| 1 | **`README.md`** (this file) | State, decisions, next action. 5 minutes. |
| 2 | **`PLAN.md`** | Architecture, data model, phases, risks. The contract. |
| 3 | **`BUILD-GUIDE.md`** | Tooling, learning order, build order, beginner traps. |
| 4 | **`DESIGN.md`** | Colors, sizes, widget specs, interaction model. |
| 5 | **`design/pixel-mockup.html`** | Visual reference. Open in a browser. |
| 6 | `PRIOR-ART.md` | Why this isn't already solved. Read once, low priority. |

`design/widget-mockup.html` is an earlier, narrower widget-only study. Superseded by
`pixel-mockup.html`; keep it for reference, don't implement from it.

`device-sync/` is **unrelated** to this project. It's a note about syncing OpenCode agent config
between machines. Ignore it unless you're setting up a new dev machine.

---

## Where things stand

### Done
- [x] Problem definition, scope, non-goals
- [x] Architecture + stack chosen (Kotlin / Compose / Glance / Room / WorkManager)
- [x] Room schema drafted, including the `ItemState` separation
- [x] Triage model defined (four states, not one checkbox)
- [x] Phased build order (10 phases)
- [x] Risk table with mitigations
- [x] Prior-art survey
- [x] Tooling decision + first-week plan (`BUILD-GUIDE.md`)
- [x] Pixel mockups: 9 screens, light + dark, live interactions
- [x] Design spec extracted to `DESIGN.md`

### Not done
- [ ] Phase 0 — toolchain install
- [ ] Phase 1 — discovery (ICS sample, Ultra vs Original, announcements HTML, REST cookie test)
- [ ] Everything after that
- [ ] Git repo initialized (this folder is **not** under version control)

---

## Decisions that are locked

Changing any of these invalidates work elsewhere, so treat them as settled unless you deliberately reopen them.

| # | Decision | Why |
|---|---|---|
| 1 | **Companion app, not a fork** | Official app is proprietary; no injection path exists |
| 2 | **ICS-first, auth later** | Due dates need zero login; ships a useful widget in week one |
| 3 | **Kotlin + Compose + Glance** | Native widgets are the product; cross-platform frameworks do them worse |
| 4 | **`ItemState` is a separate table** | Sync writes `Item`, user writes `ItemState` — sync can never clobber a dismissal |
| 5 | **Four-state triage** | Done ≠ dismissed; conflating them makes the widget lie |
| 6 | **Soft delete + stable IDs** | Dismissal must survive an item disappearing and reappearing |
| 7 | **Read-only toward Blackboard** | No write-back, no auto-submit, bounded risk |
| 8 | **Sideload only** | Trademark + ToS; no Play Store |
| 9 | **Name: Eraser**, package `dev.<you>.eraser` | Trademark-clear, works as noun/verb/widget label |
| 10 | **Build the widget fourth, not first** | Glance is the least-documented, hardest-to-debug layer; data layer must exist first |
| 11 | **Announcements open in a bottom sheet** | Full text, no truncation; hosted by a translucent `TrampolineActivity` from widget taps |

---

## Open questions — answer these before Phase 1

These block real decisions, not just nice-to-haves:

1. **Device + Android version?** Affects exact alarms, dynamic color, Glance `LazyColumn` behaviour.
2. **Does your school's login use MFA** (Duo / Okta / Authenticator)? Changes the WebView auth path.
3. **Noise level:** everything (readings, lectures) or only graded/due items?
4. **Which term(s):** current only, or all active?
5. **Do ICS events carry course codes**, or do we need a one-time feed → course mapping?

Record answers by editing this list, then move them into `PLAN.md` §14 (Decision log).

---

## Hard rules for anyone (human or AI) working here

- **Do not start with the widget.** Follow the build order in `BUILD-GUIDE.md` §5.
- **Never write to Blackboard.** Read-only. No submissions, no grade changes, no API mutations.
- **Never hard-delete an `Item`.** Use `removedAt`. Dismissal state must survive.
- **Never put Blackboard branding in the app name, icon, or repo.** Trademark.
- **Never commit** `local.properties`, `.idea/`, `*.jks`, `*.keystore`, or the ICS feed URL.
- **Do not add Hilt/Dagger** until the app is large enough to hurt. Manual singletons first.
- **One screen or function at a time.** Generate, run it, then continue. No 800-line dumps.
- **Do not manually bump Gradle or AGP.** Version mismatches are the #1 beginner time sink.

---

## Environment facts (this machine)

Recorded at planning time — re-verify on a new machine:

- Windows, PowerShell.
- **No** Node, npm, Python, or Git on `PATH`.
- `winget` available at `C:\Users\cvlvelasco1\AppData\Local\Microsoft\WindowsApps\winget.exe`.
- Not a git repository.

Install command for a fresh machine:

```powershell
winget install Google.AndroidStudio
winget install Git.Git
```

Then install SDK + platform-tools + API 35 image, accept licenses, and enable USB debugging on the phone.

---

## Next action

**Phase 1 — discovery**, before writing app code. It's ~3 hours and it de-risks everything after it:

1. Grab the ICS URL, save a sample `.ics`, inspect `UID`, `TZID`, `CATEGORIES`.
2. Log in on desktop — is it Ultra (`/ultra/`) or Original (`/webapps/portal`)?
3. Capture the announcements page HTML for one course.
4. Test whether `/learn/api/public/v1/users/me` answers with a plain session cookie.
5. Note whether login involves MFA.

Output: `discovery/report.md` + fixtures (`sample.ics`, `announcements.html`).

Phase 0 (install the toolchain) can happen in parallel — it's mostly waiting on downloads.
