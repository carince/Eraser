# Eraser — Design Spec

Extracted from `design/pixel-mockup.html` so it survives without opening a browser.
The HTML is the visual reference; this file is the source of truth for values.

**Device reference:** Pixel, 412 × 915 dp. All measurements below are dp/sp.

---

## 1. Screens

| Screen | Purpose | Mockup anchor |
|---|---|---|
| **Home — light** | Widget stack on the launcher, dynamic color from wallpaper | `Home screen` |
| **Home — dark** | Same, dark tokens | `Home screen` |
| **Today** | Grouped list: Overdue / Today / This week / Later | `The app` |
| **Inbox** | Announcements as cards, local unread state | `The app` |
| **Detail** | One item: metadata, body, actions | `The app` |
| **Quick add** | Bottom sheet for a custom deadline | `The app` |
| **Dark app** | Today screen with M3 dark tokens | `The app` |
| **Widget sizes** | 4×4 + two 2×2 on one home screen | `Widget sizes` |
| **Announcement popup** | Full-text sheet over the home screen | `Announcement popup` |

---

## 2. Color tokens (Material 3)

| Token | Light | Dark |
|---|---|---|
| `surface` | `#F7F2FA` | `#1D1B20` |
| `background` (app) | `#FDF8FD` | `#141218` |
| `onSurface` | `#1D1B20` | `#E6E0E9` |
| `onSurfaceVariant` | `#49454F` | `#CAC4D0` |
| `outline` | `#79747E` | `#938F99` |
| `outlineVariant` | `#CAC4D0` | `#49454F` |
| `primary` | `#6750A4` | `#D0BCFF` |
| `onPrimary` | `#FFFFFF` | `#381E72` |
| `primaryContainer` | `#EADDFF` | `#4F378B` |
| `onPrimaryContainer` | `#21005D` | `#EADDFF` |
| `secondaryContainer` | `#E8DEF8` | `#4A4458` |
| `onSecondaryContainer` | `#1D192B` | `#E8DEF8` |
| `error` | `#B3261E` | `#F2B8B5` |

**In Compose:** consume these through `MaterialTheme.colorScheme` with dynamic color on
(Android 12+), and treat the table above as the *fallback* palette when dynamic color is off.
Do not hardcode these hex values in composables.

**In Glance:** widgets cannot use `MaterialTheme` dynamic color directly. Read
`GlanceTheme.colors` (M3 tokens are mapped for you in `androidx.glance:glance-material3`) and
fall back to the table above. This is the single most common widget styling mistake.

---

## 3. Course chip palette

Each course gets one tonal pair. Assigned deterministically from `Course.id` hash so the color is
stable across syncs and reinstalls.

| Course | Light bg / fg | Dark bg / fg |
|---|---|---|
| CS 3410 (default) | `#EADDFF` / `#21005D` | `#4F378B` / `#EADDFF` |
| MATH 2210 | `#FFD9E2` / `#3E001D` | `#633B48` / `#FFD9E2` |
| ENG 2100 | `#C8F0EC` / `#00312E` | `#004F4A` / `#A7F2EA` |
| **Custom item** | `#E8DEF8` / `#1D192B` | `#4A4458` / `#E8DEF8` |

Custom items use the neutral `secondaryContainer` pair plus the literal label **`Custom`** —
never a course code. Your own deadlines must never masquerade as Blackboard ones.

Chip metrics: 10.5sp / weight 500 / 6dp radius / 1dp×6dp padding (widget).
11sp / 8dp radius / 2dp×8dp padding (app cards).

---

## 4. Shape & spacing

| Element | Radius | Size |
|---|---|---|
| Widget container | 28dp | full width, height by cell count |
| App card (Inbox) | 22dp | full width |
| Bottom sheet | 30dp top corners | max 78% screen height |
| FAB | 19dp (rounded square) | 58dp |
| Bottom nav | — | 88dp tall, pill indicator |
| Checkbox (widget) | 5dp | 21dp |
| Checkbox (app) | 6dp | 22dp |
| `⋯` overflow button | 50% | 26dp |
| List row | 14dp (widget) / 18dp (app) | — |

**FAB is a rounded square, not a circle.** That's the current Material 3 / Pixel language and it
reads as "add" rather than "compose."

---

## 5. Widget specs

| Widget | Size | Rows shown | Content |
|---|---|---|---|
| **Due Soon** | 4×2 | 2 + `+N more` | checkbox, title, course chip, time, relative time, `⋯` |
| **Due Soon** | 4×3 | 3 | same |
| **Due Soon** | 4×4 | 5 | same |
| **Today** | 2×2 | 3 compact lines | `HH:mm` + title |
| **Announcements** | 4×2 / 4×4 | 2–3 | unread dot, 2-line title, course + age |

**Row anatomy (Due Soon):**
`[21dp checkbox] [title 13.5sp / meta 11.5sp] [relative time] [⋯ 26dp]`

**Relative time:** `3h`, `Tomorr.`, `Thu`, `2d`, `1d late`. Tabular numerals.
Turns `error` color when under 24h or overdue.

**Text rules:** single-line title with ellipsis. Never wrap — a 2-line row in a 4×2 widget
breaks the whole stack.

**Why fixed `take(N)` and not a scrollable `LazyColumn`:** collection widgets misbehave across
launchers, and a widget you can't see the bottom of is worse than one that says `+2 more`.
`LazyColumn` in Glance is a Phase 10 experiment, not a Phase 3 requirement.

---

## 6. Interaction model

Widgets have **no long-press** and unreliable swipe. Every action needs an explicit target.

| Target | Action |
|---|---|
| Tap row | Open item detail (deep link `eraser://item/<id>`) |
| Tap checkbox | **Done** — writes `ItemState.doneAt` |
| Tap `⋯` | Small dialog activity: Snooze 1h / Tonight / Tomorrow · Dismiss · Open in Blackboard |
| Tap announcement row | Open the announcement sheet (see §7) |
| Tap widget header | Open the app on that list |

**Triage semantics** (full table in `PLAN.md` §4):

- **Done** → `doneAt`, strikethrough, disappears from widget
- **Snooze** → `snoozedUntil`, hidden until then, returns automatically
- **Dismiss** → `dismissedAt`, hidden permanently, still visible in app
- **Open** → no state, hands off to detail or Blackboard

All four are local-only. Nothing is written back.

---

## 7. Announcement popup (the bottom sheet)

The one interaction that spans both surfaces.

**From the widget:** tap an announcement row → the sheet appears over the **dimmed home screen**.
The app never launches. Dismissing drops you back on the launcher.

**In the app:** tap an Inbox card → the same sheet, same composable.

### Layout

```
[handle 38×4, outline]
[course chip] [2h ago · Prof. Ramirez]           [× close]
[title 22sp, weight 500, letter-spacing -0.3]
[body 14sp / 1.62 line-height, scrollable, mask fade top+bottom]
────────────────────────────────────────────
[Mark read]                    [Open in Blackboard]
```

- Sheet: `surface`, 30dp top radius, 10dp/22dp/24dp padding, `0 -12px 50px rgba(0,0,0,.38)` shadow
- Scrim: `rgba(0,0,0,.45)`
- Body is the **full text**, not a truncation. Cap at 78% height, scroll inside.
- Bottom border: `onSurface` at 10% opacity above the action row.
- Enter animation: 40dp translateY + fade, 260ms `cubic-bezier(.2,.8,.2,1)`

### Why a sheet, not a dialog

Announcements run long. A centered dialog either clips or becomes a cramped scroll box. A sheet
grows with the content, scrolls naturally, and has room for two actions. It's also the M3 pattern
for exactly this case.

### Implementation

```
widget tap
  → actionStartActivity(eraser://announcement/<id>)
  → TrampolineActivity (theme: Theme.Translucent.NoTitleBar, no animation)
  → setContent { AnnouncementSheet(announcementId) }
  → ModalBottomSheet(onDismissRequest = { finish() })
```

One composable, reused in-app via the same route. Do not build two versions.

**Gotcha:** the trampoline must be `exported="true"` for the widget PendingIntent, and must
`finish()` on dismiss or you leave a phantom task in recents.

---

## 8. Typography

| Role | Size / weight |
|---|---|
| Screen title | 29sp / 500 / -0.5 tracking |
| Detail title | 26sp / 500 / -0.4 |
| Sheet title | 22sp / 500 / -0.3 |
| List item title | 14.5sp / 500 |
| Widget row title | 13.5sp / 500 |
| Widget announcement title | 13sp / 400 (500 if unread) |
| Meta / secondary | 11.5–12.5sp / 400 |
| Section header | 11.5sp / 600 / 0.6 tracking / uppercase |
| Chip | 10.5–11sp / 500 |

Font: `Google Sans` where available, `Roboto` fallback. Use the system font stack; don't bundle.

---

## 9. Accessibility requirements

- Checkbox hit target ≥ 48dp even though the visual is 21dp (wrap in padding, not size).
- `contentDescription` on every checkbox: *"Mark HW5 Graph traversal done"* — not *"checkbox"*.
- Relative time must not be the only signal: colour alone fails for colour-blind users, so
  overdue also gets the word `late`.
- Test at 200% font scale. Widget rows will need to drop to 2 lines max and truncate harder.
- Dark mode is not optional; the widget inherits the launcher theme.

---

## 10. Open design questions

1. **Announcements: separate widget or merged into one tall widget?** Separate is more glanceable;
   merged saves a home-screen row. Current mockup: separate.
2. **"Add this date to my deadlines"** button in the announcement sheet? Bridges announcements →
   due dates, cheap to build. Not in the mockup yet.
3. **Courses tab:** the bottom nav has a slot for it, but in-app browsing is Phase 7. Leave the tab
   hidden until Phase 7, or show a placeholder?
4. **Dynamic color on a non-purple wallpaper:** tonal chips may lose separation on green/orange.
   Verify on the real device before locking the palette.
