# Prior Art — has this been done?

Short answer: **the idea, yes. The exact thing, no.**

Nobody has shipped a good Blackboard → Android → Material 3 widget with local triage and custom due dates. The closest projects are all either (a) Canvas-only, (b) Google Classroom, (c) dead Blackboard Android clients from 2012–2017, or (d) browser extensions.

---

## The closest matches

### 1. `lelrxuch/canvas-today-android` — the architectural twin (2026, Kotlin, MIT)
**This is the single most useful reference.** Canvas LMS → instant list + **Jetpack Glance home-screen widget** + its own reminders + personal notes. Same stack as our plan: `core/ data/ work/ widget/ ui/`, WorkManager, Compose, encrypted token in Keystore.

What it proves:
- Glance widget for an LMS is viable and shippable.
- "Show cached list instantly, refresh in background" is the right UX.
- Token encryption via AES-GCM + Android Keystore is the pattern to copy.
- **Battery killers are real:** its README explicitly tells Samsung/Xiaomi users to set the app to "Unrestricted."
- Distribution is a GitHub Releases APK. No Play Store.

Why it doesn't cover us: Canvas has a **personal access token** flow (Account → Settings → New Access Token). Blackboard does not. That's the whole reason our plan leans on the ICS feed.

### 2. `InsaneCoder789/ClassSync` — closest product shape (2026, Kotlin)
Local-first Android academic dashboard: Google Classroom sync, optional Gmail discovery, **manual tasks**, planner, exam mode, reminders, **home-screen widget**. Room + DataStore + WorkManager + Compose. Actually on Play Store (closed testing).

Why it matters: it validates the exact feature mix we want — synced items **and** user-created tasks in one list, plus a widget. Swap Classroom for Blackboard and it's our app.

### 3. `fennifith/Blackboard` — the cautionary tale (2017, Java, Apache-2.0)
An Android Blackboard client built by someone annoyed at the official app: *"NOT BEING ABLE TO CLICK URLs IN THE DESCRIPTION OF AN ASSIGNMENT (this almost cost me a grade)."* Pure HTML scraping + JS injection.

Two lessons:
- **It worked.** Authenticated HTML scraping of Blackboard is proven.
- **It died.** The README says it's only verified against one school (NASD) and "will likely break if used to access another organization." Hardcoded per-institution selectors = dead project.

### 4. `gvsucis/blackboard-android-app` (2015, Java, GPL-2.0)
Open-source Blackboard Android app, written as a GVSU grad student's MS project. Also dead.

### 5. `adammw/py-blackboard` (2013–2016, Python)
"Blackboard Mobile API Client / Scraper (WIP)." Confirms the unofficial mobile-API route exists, and that it's a WIP that never matured.

### 6. `Lceoliu/ShangHaITech` (2026, Flutter)
One-stop campus app for ShanghaiTech: integrates **Blackboard + Gradescope deadlines**, auto course selection, email summaries. iOS + Android. Institution-specific, not a general product.

---

## Commercial / adjacent

| Product | What it does | Why it's not this |
|---|---|---|
| **BetterCampus** (2M+ students) | Browser extension: Canvas/Blackboard/Brightspace/Moodle, to-do list, grade calculator, dark mode, AI notes | Desktop browser only, no Android widget |
| **Better Blackboard Learn** | Chrome extension, theming | Cosmetic only |
| **dormway.app** | AI student planner, auto-syncs Canvas/Blackboard/Moodle read-only, syllabus parsing | Web app, not a home-screen widget |
| **DueQueue** | Canvas deadline dashboard + reminders | Canvas only, web |
| **Due Gooder** | AI syllabus extraction → Google Calendar | Not live data |
| **Boost** | Canvas assignment reminders + planner | Canvas only, mobile app |
| **Canvas Sync for Notion** | Canvas → Notion database | Canvas only |

## API libraries (useful, not competitors)

| Library | What |
|---|---|
| **bblearn** (PyPI) | Blackboard Learn REST API client over OAuth 2.0 |
| **Blackboard-LMS-CLI** (PyPI) | CLI for the BB REST API — built because *"the blackboard web interface can be ineffective and awkward to use"* |
| **chawk-py** | Python wrapper for BB admin tasks |
| **developer.blackboard.com** | Official REST API docs; requires an app registered by the institution |

## Historical footnote

Blackboard's old **Mobile Learn** app *did* ship a home-screen widget — a "Mobile Learn Dashboard" you added via the Android 2.x widget menu. It's gone. So the official app had this feature over a decade ago and dropped it.

---

## What this means for us

### Already solved — copy, don't rebuild
- **Glance widget + WorkManager + Room + encrypted token**: `canvas-today-android` is a working reference. Read its `widget/`, `work/`, and `data/` folders before writing ours.
- **Manual tasks mixed with synced items**: `ClassSync` shows the model works.
- **Read-only positioning**: BetterCampus and dormway both market "never writes back to your LMS." That's the safe, defensible framing.

### Genuinely open
1. **Blackboard + Android widget.** Every Blackboard-specific Android project is from 2012–2017 and dead. The Canvas ecosystem got all the tooling because Canvas has an open API.
2. **ICS-fed widget.** Every existing tool authenticates with an API token. Nobody builds the widget off the calendar feed. That's our zero-auth wedge.
3. **Four-state triage** (done / snooze / dismiss / open). None of the prior art has "dismiss" as distinct from "done." ClassSync and Canvas Today both just have completion.
4. **Custom due dates + local overrides** that survive sync. ClassSync has manual tasks; nobody has *overrides* of synced dates.
5. **Cross-platform** (Blackboard + Cengage + Coursera) on Android. Nobody does this.

### The strategic read
Canvas has an official open API, which is why all the polished tools are Canvas-first. Blackboard doesn't, which is why the Blackboard tools are either dead scrapers or institutional one-offs. **That gap is the opportunity — and the risk.** Our ICS-first design sidesteps the API problem entirely, which is the part everyone else got stuck on.

### The trap to avoid
`fennifith/Blackboard` died from hardcoded per-school selectors. Keep every HTML selector in one config module, versioned, with fixture tests. If Blackboard changes markup, the fix should be one file, not a rewrite.

---

## Verdict

- **Novelty:** low on concept, high on execution. "Student planner with a widget" is a crowded space.
- **Differentiation:** Blackboard-first, ICS-fed (no login needed for due dates), Material 3 widgets, triage semantics, local overrides.
- **Competition:** none directly in the Blackboard Android widget niche. Adjacent players are web-first or Canvas-first.
- **Risk:** the market exists but is small, and the official Blackboard app could add widgets at any time. Build it for yourself first; treat "product" as a bonus.
