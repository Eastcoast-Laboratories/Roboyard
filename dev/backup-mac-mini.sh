#!/usr/bin/env bash
# Back up everything irreplaceable from the remote build Mac
# (Scaleway Mac mini) into a local backup directory.
#
# Re-runnable: rsync -azP resumes partial transfers, so an aborted run
# continues where it stopped. Safe to run repeatedly.
#
# Usage:
#   bash dev/backup-mac-mini.sh [backup-dir]
# Default backup dir: /backups/mac-mini-<today>
#
# What is backed up and why:
#   devtools/certs/            private keys + CSRs + certs + p12 — the ONLY
#                              truly irreplaceable part of ~/devtools
#   devtools/xcodegen-*        tiny tool, saves one download on a new Mac
#   provisioning-profiles/     *.mobileprovision (re-downloadable but cheap)
#   home/                      whole ~/ minus repos/, devtools/ and regenerable
#                              dirs — covers .zshrc, Keychains (imported
#                              certs+keys), Preferences, Application Support,
#                              Documents, Pictures, .ssh
#   mac-config/Roboyard.bundle git bundle of the Mac-side repo (master only —
#                              Mac clone has a corrupt ref deeplilnk_fix_try)
#   repos/CaveShuttle, Lalumo  rsync trees (no .git on the Mac!) minus
#                              regenerable dirs (node_modules/dist/build)
#   xcode/*.xcarchive/         exact archives uploaded to App Store —
#                              Roboyard 141, CaveShuttle 2, Lalumo 44
#                              (app, dSYMs for crash symbolication, Info)
#   xcode/ipa/                 exported .ipa files where they exist
#
# Deliberately NOT backed up (recreated by dev/macos-build-setup.sh or Xcode):
#   ~/devtools/Android (6 GB SDK), jdk-17, node-v22, *.tar.gz/*.zip installers
#   ~/Library/Developer (9.6 GB — CoreSimulator runtimes + DerivedData)
#   Library caches/system dirs (Caches, Metadata, Biome, DuetExpertCenter,
#   IntelligencePlatform, Trial, Daemon Containers, Logs), .Trash, .DS_Store,
#   .gradle/.konan/.npm/.gem/.cocoapods/.android/.swiftpm, .zsh_sessions
#   node_modules/, dist/, build/, .gradle/ in the repos
#   ~/repos/Roboyard + ~/Documents/Roboyard working trees — laptop repo at
#     /var/www/Roboyard is the source of truth (bundle kept as fallback)
#
# Host comes from dev/.env ($MAC_HOST). Source it first or export MAC_HOST.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
[ -f "$REPO_ROOT/dev/.env" ] && source "$REPO_ROOT/dev/.env"
: "${MAC_HOST:?MAC_HOST not set — create dev/.env from dev/.env.example}"

BACKUP="${1:-/backups/mac-mini-$(date +%Y-%m-%d)}"
RSYNC="rsync -azP --timeout=60"
STAGE=/var/tmp/mac-backup   # staging dir on the Mac for files not reachable via rsync directly

mkdir -p "$BACKUP"/{devtools,provisioning-profiles,home,repos,mac-config,xcode}

echo "== 1/6 devtools (certs + xcodegen only — SDKs/JDK/Node reinstall via script)"
$RSYNC "$MAC_HOST":~/devtools/certs/                     "$BACKUP/devtools/certs/"
$RSYNC "$MAC_HOST":~/devtools/xcodegen-2.43.0/           "$BACKUP/devtools/xcodegen-2.43.0/"

echo "== 2/6 provisioning profiles"
$RSYNC "$MAC_HOST":~/Library/MobileDevice/Provisioning\ Profiles/*.mobileprovision "$BACKUP/provisioning-profiles/"

echo "== 3/6 home directory (settings, Keychains, Documents — no repos, no regenerable dirs)"
rc=0
$RSYNC \
  --exclude='repos/' --exclude='devtools/' \
  --exclude='Documents/Roboyard/' \
  --exclude='Library/Developer/' --exclude='Library/Caches/' \
  --exclude='Library/Metadata/' --exclude='Library/Biome/' \
  --exclude='Library/DuetExpertCenter/' --exclude='Library/IntelligencePlatform/' \
  --exclude='Library/Trial/' --exclude='Library/Daemon Containers/' \
  --exclude='Library/Logs/' \
  --exclude='.Trash/' --exclude='.DS_Store' --exclude='.zsh_sessions/' \
  --exclude='.gradle/' --exclude='.konan/' --exclude='.npm/' --exclude='.gem/' \
  --exclude='.cocoapods/' --exclude='.android/' --exclude='.swiftpm/' \
  "$MAC_HOST":~/ "$BACKUP/home/" || rc=$?
# rc 23/24 = individual files unreadable/vanished (TCC-protected system
# plists) — warn loudly but continue, the rest of the home dir is fine.
if [ "${rc:-0}" != "0" ]; then
  echo "!! home backup: rsync exited $rc — a few protected files were skipped (see above)"
fi

echo "== 4/6 git bundle (Roboyard, master branch)"
# Bundle is built on the Mac first (rsync cannot stream a bundle directly).
ssh -o ConnectTimeout=15 "$MAC_HOST" "mkdir -p $STAGE && \
  git -C ~/repos/Roboyard bundle create $STAGE/Roboyard.bundle master || true"
$RSYNC "$MAC_HOST":"$STAGE/"*.bundle                     "$BACKUP/mac-config/" || true

echo "== 5/6 repo source trees (no .git on Mac for CaveShuttle/Lalumo)"
for r in CaveShuttle Lalumo; do
  $RSYNC \
    --exclude='node_modules/' --exclude='dist/' --exclude='build/' \
    --exclude='.gradle/' --exclude='local.properties' --exclude='.tags' --exclude='bin/' \
    "$MAC_HOST":~/repos/"$r"/ "$BACKUP/repos/$r/"
done

# release artifacts on the Mac that are not in git and partly not on the
# laptop (v60 ipa/jar/apk, v53.apk) — rest of repos/Roboyard stays excluded
$RSYNC "$MAC_HOST":~/repos/Roboyard/download/ "$BACKUP/repos/Roboyard-download/"

echo "== 6/6 xcarchives + exported .ipa files (all 3 apps)"
# Build outputs come and go: build/ is deleted before each re-archive (another
# session may be rebuilding right now), and ExportOptions destination=upload
# leaves no local .ipa. A missing source is reported loudly and the copy
# already in the backup is kept untouched.
sync_if_present() {  # <label> <remote path> <local path>
  if ssh -o ConnectTimeout=15 "$MAC_HOST" "test -e $2"; then
    $RSYNC "$MAC_HOST":"$2" "$3"
  else
    echo "   !! $1: $2 missing on the Mac right now — skipped, previous backup copy kept"
  fi
}
sync_if_present "Roboyard xcarchive"    "~/repos/Roboyard/iosApp/build/iosApp.xcarchive/" "$BACKUP/xcode/iosApp.xcarchive/"
sync_if_present "CaveShuttle xcarchive" "~/repos/CaveShuttle/build/App.xcarchive/"       "$BACKUP/xcode/CaveShuttle.xcarchive/"
sync_if_present "Lalumo xcarchive"      "~/repos/Lalumo/build/App.xcarchive/"            "$BACKUP/xcode/Lalumo.xcarchive/"
mkdir -p "$BACKUP/xcode/ipa"
sync_if_present "Roboyard IPA"    "~/repos/Roboyard/iosApp/build/ipa/iosApp.ipa"  "$BACKUP/xcode/ipa/iosApp.ipa"
sync_if_present "CaveShuttle IPA" "~/repos/CaveShuttle/build/ios/CaveShuttle.ipa" "$BACKUP/xcode/ipa/CaveShuttle.ipa"
sync_if_present "Lalumo IPA"      "~/repos/Lalumo/build/ipa/App.ipa"              "$BACKUP/xcode/ipa/Lalumo.ipa"

echo "== done"
du -sh "$BACKUP" "$BACKUP"/*/ 2>/dev/null
