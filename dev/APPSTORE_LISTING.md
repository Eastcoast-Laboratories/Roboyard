# App Store Connect listing — Roboyard (de.z11.roboyard)

Everything needed to fill the App Store web UI. Collected from the existing
Play Store metadata (fastlane/metadata/android/) and the community site.
Only non-sensitive data — account details stay in dev/.env.

## 1. Basics

- **App name:** Roboyard
- **Bundle ID:** de.z11.roboyard (explicit App ID, registered 2026-10-09)
- **SKU:** roboyard-ios
- **Primary language:** English (add German as localization)
- **Category:** Games → Puzzle (secondary: Games → Board)
- **Content rights:** own work, open source
- **Price:** Free, no IAP, no ads

## 2. Texts (from Play Store listing)

### Subtitle (30 chars max, iOS only)
```
Robot puzzle — fewest moves win
```

### Promotional text (170 chars, can change without review)
```
Guide robots through a maze of hedges. Plan the optimal route — an AI
solver shows hints and the perfect solution. Free, ad-free, open source.
```

### Description — EN (4000 chars max)
Reuse the Play Store EN text in `dev/Play Store Description.txt` /
`fastlane/metadata/android/en-US/full_description.txt`, minus the
Android-specific accessibility line — replace with VoiceOver note:

```
Inspired by the classic board game Ricochet Robots, Roboyard challenges
your logical thinking! Guide robots through a maze filled with obstacles
and find the optimal path to the goal.

GAMEPLAY:
• Robots move in straight lines and stop only at hedges or other robots.
• Plan ahead and find the shortest path to the target!
• Can you master each challenge with the perfect solution?

FEATURES:
• Unique AI-powered solution algorithm — get hints or watch the optimal
  solution!
• Multiple difficulty levels — from beginner-friendly to mind-bending.
• Save & share custom game boards — challenge your friends!
• Sharpen your logical thinking & spatial awareness.

Completely free, ad-free & open source!
Source code & more info: https://roboyard.z11.de/
```

### Description — DE
Same source: `dev/Play Store Description.txt` (### deutsch block) /
`fastlane/metadata/android/de/full_description.txt` — remove the
TalkBack/Android sentence, note VoiceOver support instead.

### Keywords (100 chars, comma separated, no spaces needed)
```
robot,puzzle,logic,maze,ricochet,brain,sliding,solver,minimal,moves,board
```

### Support URL / Marketing URL
`https://roboyard.z11.de/`

### Privacy Policy URL
`https://roboyard.z11.de/datenschutz` — page exists on the community site
(routes/web.php has the /datenschutz route + datenschutz.blade.php view).

## 3. Age rating (expected: 4+)

Questionnaire answers:
- Cartoon/fantasy violence: none
- Realistic violence: none
- Horror/fear: none
- Medical info: none
- Gambling/contests: none (daily streak is not a contest)
- User-generated content: yes — players can share self-designed maps;
  answer "infrequent/mild" for UGC + confirm no objectionable content flow
- Unrestricted web access: no
- Location services: no

→ Expected rating: **4+**

## 4. App Privacy questionnaire (nutrition label)

App data flows: account registration/login (optional, for save-game sync,
achievements, community features), map sharing, leaderboard/history sync.
Local-only gameplay; no ads, no analytics, no tracking SDKs.

| Category | Collected? | Linked to user | Purpose |
|---|---|---|---|
| Email address | Yes (optional account) | Yes | App functionality (login) |
| User ID | Yes (account) | Yes | App functionality (sync) |
| User content (save games, shared maps) | Yes | Yes | App functionality (sync/share) |
| Identifiers (username) | Yes | Yes | App functionality |
| Diagnostics/crash data | No | — | — |
| Location, contacts, browsing, purchases | No | — | — |
| Data used to track you (ATT) | **No — no ATT prompt needed** | | |

Verify against ApiClient/SyncManager before submitting — only fields
actually sent to the server count.

## 5. Screenshots

Required sizes (App Store Connect):
- 6.9″ iPhone (1320×2868) or 6.7″ (1290×2796) — take on simulator
- 6.5″ iPhone (1284×2778)
- iPad Pro 12.9″ (2048×2732) — only if iPad stays in TARGETED_DEVICE_FAMILY
  (currently "1,2" = iPhone+iPad — either produce iPad shots or restrict to
  iPhone-only "1" in project.yml)

Existing material:
- Play Store shots: `fastlane/metadata/android/{de,en-US}/images/
  phoneScreenshots/1..5.png` — Android aspect, need re-capture on iOS sim
- One iOS sim shot exists: main menu (1206×2622) at
  /var/tmp/devin/roboyard-ios.png

How to capture on the Mac (per dev/macos-build-setup.sh):
1. `xcrun simctl create <name> "com.apple.CoreSimulator.SimDeviceType.<dev>"
   "com.apple.CoreSimulator.SimRuntime.iOS-26-5"` for each required size
   (iPhone 17 Pro Max = 6.9″, iPad Pro 13-inch for iPad shots)
2. boot → install → launch → `xcrun simctl io booted screenshot <file>`
3. Navigate the app manually via VNC, or script taps with
   `xcrun simctl ui booted tap x y` (if ui subcommand unavailable:
   use simctl spawn ... sendButtonAction or just capture the main screens)

Shots to capture (mirroring the Play Store set):
1. Main menu / title
2. Game board mid-puzzle
3. Hint/solution overlay
4. Level select
5. Editor or share-map screen

## 6. Review information

- **Demo account:** login is optional — game fully playable without.
  If reviewers need account features, create a demo account on the
  community site and put credentials in the review notes field.
- **Review notes:** "Optional login for cloud save sync and shared maps.
  All gameplay works without an account. Open source:
  https://roboyard.z11.de/"
- **Version:** MARKETING_VERSION 53 / CURRENT_PROJECT_VERSION 141
  (project.yml) — bump CURRENT_PROJECT_VERSION before each upload.

## 7. Status

- [x] Build v141 uploaded to App Store Connect (2026-10-09)
- [x] `ITSAppUsesNonExemptEncryption=false` set in Info.plist
- [ ] App record created in App Store Connect (may exist already —
      upload succeeded, which usually means the record is there)
- [ ] Screenshots captured at required sizes
- [ ] Texts pasted (EN + DE localizations)
- [ ] Privacy policy URL verified live
- [ ] Age rating + App Privacy questionnaire filled
- [ ] TestFlight internal testing enabled → install on iPhone
- [ ] Submit for review
