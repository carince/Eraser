# Build Guide — your first Android app

You've never built an Android app. That's fine. The stack is already decided
(Kotlin + Compose + Room + WorkManager + Glance); this doc is about *how* to actually get there
without drowning.

---

## 1. The tool: Android Studio

Use Android Studio. There is no serious alternative for this project.

| Option | Verdict |
|---|---|
| **Android Studio** | ✅ Official IDE. Bundles JDK, SDK, Gradle, emulator, Logcat, Layout Inspector, Database Inspector, Compose previews, device mirroring. One install, everything works. |
| VS Code + Android CLI | ❌ You'd hand-assemble the SDK, Gradle, emulator, and debugger. More setup than the app. |
| Flutter | ❌ Widgets are the whole point, and Glance is native Android. Flutter home-screen widgets are second-class and painful. |
| React Native | ❌ Same problem, worse. |
| Cloud IDE (Firebase Studio, Codespaces) | ❌ Android emulators in the cloud are slow and flaky. Local is better for a phone app. |
| FlutterFlow / no-code | ❌ Cannot produce a Glance widget. |
| Tasker / KWGT | ❌ No access to Blackboard data. Fine for a toy, not this. |

**Kotlin, not Java.** **Compose, not XML layouts.** Both are the current defaults and both make this
app easier — Compose especially, since the widget and the app share a mental model.

---

## 2. What you're actually signing up for

Honest scope, first-timer pace, evenings and weekends:

| Milestone | Realistic time |
|---|---|
| Environment working, hello-world on your phone | 1 evening (plus download time) |
| Kotlin + Compose comfortable enough | 1 week |
| ICS parsing + list on screen | 1 week |
| Room + background sync | 1 week |
| First widget on your home screen | 1–2 weeks |
| Triage + custom items | 1 week |
| Auth + announcements | 1–2 weeks |

That's roughly **6–8 weeks** to the full plan, or **~3 weeks** to the version that's already useful
(ICS → list → widget). This is not a weekend project, and that's normal.

---

## 3. Machine prep

Requirements: Windows 10/11, **16 GB RAM recommended** (8 GB works but the emulator will hurt),
**~15 GB free disk**, virtualization enabled in BIOS (needed for the emulator).

```powershell
winget install Google.AndroidStudio
winget install Git.Git
```

Then on first launch:
1. Let it install the **SDK**, **platform-tools**, and an **API 35** system image.
2. Accept the SDK licenses when prompted.
3. Create a device: **Pixel 9** profile, API 35.

**Use a physical Pixel if you have one.** For this app it's not optional-ish: widgets behave
differently on real launchers, background sync is subject to real battery restrictions, and you need
to see it on your actual home screen. The emulator is a fallback, not the target.

On the phone: Settings → About phone → tap **Build number** 7× → Developer options → **USB debugging**.
Then `adb devices` should list it.

---

## 4. Project setup choices (decide once, they're painful to change)

- Template: **Empty Activity** (Compose)
- Language: **Kotlin**
- Package name: `dev.<yourname>.eraser` — pick it now, renaming later is a slog
- `minSdk 26`, `targetSdk` = latest
- Keep the **version catalog** (`gradle/libs.versions.toml`) the template gives you
- **Do not manually upgrade Gradle or the Android Gradle Plugin.** Version mismatches are the #1
  beginner time sink. Upgrade only when Android Studio prompts you and the build is green.
- `git init` immediately, private GitHub repo, commit after every working milestone
- `.gitignore`: `local.properties`, `.idea/`, `*.jks`, `*.keystore`

---

## 5. The order to learn — and the order to build

These are different, and getting the build order wrong is the main way this project dies.

### Learn in this order
1. **Kotlin** — variables, functions, classes, nullability, collections, `when`, basic coroutines
2. **Compose** — `@Composable`, `remember`, state, `Column`/`Row`/`LazyColumn`, `MaterialTheme`
3. **ViewModel + state flow** — how data reaches the UI
4. **Room** — entities, DAOs, queries, migrations
5. **WorkManager** — periodic background work
6. **Glance** — widgets (last, because it's the least documented and the most frustrating)

### Build in this order (vertical slices, not layers)

> **Do not start with the widget.** It's the thing you want most, and it's the worst place to start.
> Widgets are hard to debug, Glance has fewer examples than Compose, and you can't build a good
> widget until the data layer exists. Build the widget *fourth*, not first.

1. **Static list, fake data.** One screen, a `LazyColumn` of hardcoded items. No networking, no DB.
   Goal: get comfortable with Compose and get something on your phone.
2. **ICS parser as pure Kotlin.** `parseIcs(String): List<Item>` with JUnit tests. No Android
   dependencies. This is where timezone bugs live, so test it hard and early.
3. **Room.** Store parsed items, read them back into the list. Now it survives restarts.
4. **Real ICS fetch + WorkManager.** Download the feed, parse, upsert, refresh on a schedule.
5. **The widget.** Glance, reading from Room. This is the payoff.
6. **Triage** — done / snooze / dismiss, in the `ItemState` table.
7. **Custom items + overrides.**
8. **Auth + announcements** (WebView, Jsoup).
9. **Cengage / Coursera.**

Each step ends with something you can run on your phone. Never have two unverified layers stacked.

---

## 6. Beginner traps specific to this app

| Trap | Why it bites | Do this instead |
|---|---|---|
| Starting with the widget | Glance is poorly documented; you'll blame yourself for a framework problem | Build app + Room first |
| Adding Hilt/Dagger on day one | Dependency injection is a second problem on top of a first problem | Manual singletons until it hurts |
| Using XML layouts | You'll follow old tutorials that don't match Compose | Compose only |
| Parsing dates with `SimpleDateFormat` | DST and all-day events will silently shift | `java.time` + store the raw string |
| Trusting the emulator for widgets | Launchers differ wildly | Test on the real phone |
| Ignoring battery optimization | Samsung/Xiaomi kill background sync within hours | Ask for "Unrestricted" in settings; document it |
| Letting AI generate 800 lines at once | You can't debug what you didn't read | One function/screen at a time, run it, then continue |
| Committing `local.properties` | Leaks your SDK path and machine details | Gitignore it from commit #1 |
| Chasing "best architecture" | MVVM + clean architecture + modules for a solo app is self-sabotage | One module, three folders: `data/`, `ui/`, `work/` |

---

## 7. Your first week, concretely

**Day 1** — Install Android Studio + Git. Create the project. Run the template on your phone.
Commit.

**Day 2** — Kotlin basics. Rewrite the template screen to show a `LazyColumn` of 5 fake assignments
with a checkbox and a course chip. Commit.

**Day 3** — Split fake data into a `data/` file. Add a `ViewModel`. Make the checkbox actually toggle
state. Commit.

**Day 4** — Write `parseIcs()` against a real `.ics` sample from your Blackboard feed. Unit tests for:
timed event, all-day event, DST boundary, missing `DTEND`. Commit.

**Day 5** — Add Room. Store parsed items. List reads from the database instead of the fake list.
Commit.

**Day 6** — Download the ICS URL with OkHttp on app start. Add a pull-to-refresh. Commit.

**Day 7** — Break it, fix it, write down what confused you. That list becomes your next week's
learning plan.

By the end of week one you have a real app with real deadlines, no login, no widget. That's the
right place to be.

---

## 8. Resources that are actually worth your time

- **Android Basics with Compose** (developer.android.com/courses) — Google's own course, free, current
- **Now in Android** (github.com/android/nowinandroid) — reference app; read its structure once you're past basics
- **Compose docs** — the "Thinking in Compose" and state pages specifically
- **Glance docs + `androidx.glance` samples** — sparse, so read the source when stuck
- **biweekly** — ICS parsing library; don't hand-roll iCalendar
- **Android Studio's Database Inspector** — inspect Room live; saves hours
- **Logcat** — learn to filter by your package name on day one

---

## 9. How to use an AI assistant on this (practical)

You're already using one. The failure mode is generating a whole app you can't debug.

- Ask for **one screen or one function at a time**, and run it before moving on.
- Ask *why*, not just *what*: "explain what `collectAsStateWithLifecycle` does here."
- When something breaks, paste the **full stack trace**, not a summary.
- Let it write the boring parts (Gradle config, Room boilerplate, test fixtures). Write the logic
  yourself until you understand it.
- Keep a `NOTES.md` of every error you hit and how you fixed it. Future-you will need it.

---

## 10. Definition of "I can build this now"

You're ready to stop following tutorials and start building when you can, without help:

- [ ] Create a Compose screen with a `LazyColumn` driven by a `ViewModel`
- [ ] Add a Room entity, DAO, and read a list from it
- [ ] Explain the difference between `remember` and `rememberSaveable`
- [ ] Schedule a `WorkManager` job and see it run in Logcat
- [ ] Install a debug APK on your phone with `adb`

That's maybe two weeks of evenings. Then the plan in `PLAN.md` becomes execution instead of study.
