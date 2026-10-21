#!/usr/bin/env bash
# Publishes the debug APK and a version.json to the pre-release <tag>, creating it if needed, so the
# in-app updater finds them at releases/download/<tag>/. Usage: publish-build.sh <tag> <title> <notes>
# Needs GH_TOKEN, VERSION_CODE and GITHUB_SHA; PR_NUMBER is recorded in version.json when set.
set -euo pipefail

tag="$1"
title="$2"
notes="$3"

cp app/build/outputs/apk/debug/*.apk gym-tracker-dev.apk
jq -n \
  --argjson versionCode "$VERSION_CODE" \
  --arg commit "$GITHUB_SHA" \
  --arg pr "${PR_NUMBER:-}" \
  '{versionCode: $versionCode, commit: $commit} + (if $pr == "" then {} else {pr: ($pr | tonumber)} end)' \
  > version.json

git tag -f "$tag" "$GITHUB_SHA"
git push -f origin "refs/tags/$tag"
if ! gh release view "$tag" > /dev/null 2>&1; then
  gh release create "$tag" --prerelease --verify-tag --title "$title" --notes "$notes"
fi
gh release edit "$tag" --title "$title" --notes "$notes"
# The APK goes up first, so version.json never points at a build that is not there yet.
gh release upload "$tag" gym-tracker-dev.apk --clobber
gh release upload "$tag" version.json --clobber
