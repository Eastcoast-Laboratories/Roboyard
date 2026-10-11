# Prompt: Replace the stub iOS app icon with the Android artwork

Context: the iOS build currently ships a stub/default icon; Android
already has the correct Roboyard launcher icon. Replicate the fix that
was done for Lalumo in /var/www/Musici (commit "feat(ios): replace
Capacitor stub app icon with the bird artwork", build 7.3 (45)).

Read the `ios-build-scaleway-mac` skill first — remote Mac access,
signing state and keychain-unlock pitfalls are documented there.

## Recipe (as done in Lalumo)

1. Source artwork = the Android adaptive-icon layers:
   - `app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png`
     (432x432 RGBA — the visible artwork)
   - `app/src/main/res/mipmap-xxxhdpi/ic_launcher_background.png`
     (432x432 RGBA — an image in Roboyard; Lalumo used a plain
     `#FFFFFF` from `values/ic_launcher_background.xml`)
   - Adaptive definition: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`.
   - Reference for the composed look: `mipmap-xxxhdpi/ic_launcher.png`
     (192x192 — this is what Android actually renders).
2. Compose a 1024x1024 master with Python PIL (installed):
   - canvas = background layer scaled to 1024; if it has transparent
     pixels, flatten it onto the colour Android shows behind it
   - paste the foreground scaled to 1024, centered, `Image.LANCZOS`
   - flatten to RGB — App Store Connect rejects icons with an alpha
     channel
3. Install into `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/`:
   Roboyard still has the OLD multi-file appiconset (~10 slots: iphone
   20/29/40/60 @2x/@3x, ipad 76/83.5 @1x/@2x, ios-marketing 1024, ~car
   variants). Two options — prefer (a):
   - (a) Replace `Contents.json` with a single-universal-1024 entry
     (same shape Lalumo uses: one `universal`/`ios` image, `1024x1024`)
     and one PNG `AppIcon-512@2x.png` — Xcode slices all sizes itself.
   - (b) Render every listed slot size from the master and overwrite
     each file keeping filenames and Contents.json untouched.
4. Bump `CURRENT_PROJECT_VERSION` in `iosApp/project.yml` (xcodegen —
   never edit the generated `.xcodeproj`). The icon is baked into the
   .app at archive time, so only a new binary ships it.
5. rsync the changed files to the Mac (`$MAC_HOST` from dev/.env), then
   archive + export + upload with the usual flow — keychain unlock in
   the SAME ssh session.
6. Verify inside the new xcarchive:
   `build/iosApp.xcarchive/Products/Applications/iosApp.app/` contains
   `AppIcon60x60@2x.png` etc. — it is CgBI/pngcrush format, not a plain
   PNG. Decode to view: strip the `CgBI` chunk, `zlib.decompress(idat, -15)`,
   rows are unfiltered BGRA -> swap to RGBA; or `sips` on the Mac.

## Notes / pitfalls hit in Lalumo

- Do NOT upscale the 192px composed `ic_launcher.png` — recompose from
  the 432px layers, it stays sharper.
- Keep Android's composition proportions (Lalumo's foreground fills the
  canvas to ~99% — a thin background rim remains; check the corner/edge
  pixels of the source PNGs to confirm where transparency is).
- Check `git status` before/after; propose a commit message, never
  commit yourself.
- A Roboyard release build may be running on the Mac in parallel —
  coordinate before touching `~/repos/Roboyard` build outputs.
