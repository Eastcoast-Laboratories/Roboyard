---
name: deploy
description: Deployment recipe for the roboyard.z11.de / caveshuttle.z11.de community site
triggers:
  - user
---

# Deploying the Community Site

The production community site is **not** managed by git on the server. It is uploaded with rsync via the deploy-watch script.

- Deploy script: `/var/www/roboyard.z11/deploy-watch.sh`
- Remote host: `eclabs-vm06`
- Remote path: `/var/kunden/webs/z11/roboyard.z11.de`
- The same site also serves `caveshuttle.z11.de` with different deploy paths — verify which app a file belongs to before deploying.

## Workflow

1. Test locally (local dev server / unit tests).
2. Deploy only when it works locally.
3. Test online.
4. Only then commit.

## Safety

- Never change anything on the production server without an explicit user command.
- Never run `git stash`, `git commit`, or `git revert` on the online server.
- Never delete data, drop tables, or manipulate production data without confirmation — always ask first.
