#!/usr/bin/env bash
# Usage: publish_logs.sh <dir-with-files>  — force-pushes the files as the single commit of branch ci-logs
set -u
SRC="$1"
rm -rf /tmp/ci-logs-push && mkdir -p /tmp/ci-logs-push && cp -r "$SRC"/. /tmp/ci-logs-push/
cd /tmp/ci-logs-push
git init -q && git checkout -q -b ci-logs
git config user.name "ci-bot" && git config user.email "ci-bot@users.noreply.github.com"
git add -A && git commit -qm "logs for ${GITHUB_SHA:-local} run ${GITHUB_RUN_ID:-0}"
git push -q -f "https://x-access-token:${GITHUB_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" ci-logs
