---
name: deploy
description: Deployment recipe for the roboyard.z11.de / caveshuttle.z11.de community site
triggers:
  - user
---

# Deploying the Community Site

The production community site is **not** managed by git on the server. It is uploaded with rsync via the deploy-watch script.

- Deploy script, remote host and remote path are stored in `dev/.env` (gitignored, template `dev/.env.example`) as `DEPLOY_SCRIPT`, `DEPLOY_HOST` and `DEPLOY_REMOTE_PATH`. Ask the user if the file is missing.
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
