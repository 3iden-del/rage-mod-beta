# RAGE Mod (Fabric 1.21.11)

Client-side Fabric mod for the RAGE clan. Black / dark red theme.

## Features
- Player marks: **Target / Friend / Annoyance / Enemy**, plus **Show** (highlight) and **Ignore**
- **Colored hitboxes**, **tracers**, **nick tags** and **tab list names** by mark (hitboxes/tracers show through walls)
- **Radial menu** on middle click on a player (Target, Friend, Annoyance, Info, Stats, Show, Ignore, Remove)
- **Stats dashboard**: kills / deaths / K/D and K/D-over-time graph (read from kill messages in chat)
- **Players** screen with search, **War** mode, **Clan** members screen, **Config** screen (44 options)
- **Killer highlight**: whoever kills you is highlighted for a while
- **Anti-AFK**, **Auto join** (repeats a join command), **Kill keybind** (sends a command)
- **Mod users online**, **private chat** between mod users, **Ask for help** to everyone with the mod
- **Whitelist**: nobody can use the mod unless their UUID is in your `whitelist.json`

## 1. Whitelist (required)
1. Make a **public** GitHub repo (for example `rage-whitelist`) and upload `whitelist.example.json` as `whitelist.json`.
2. Open the file on GitHub, click **Raw**, copy the link.
3. Paste that link into `WHITELIST_URL` in `src/main/java/com/ragemod/core/WhitelistGate.java`.
4. To add someone: they press **Right Shift** in game, the mod copies their UUID to the clipboard and prints it in chat.
   They send it to you, you add `{ "uuid": "...", "name": "...", "role": "member" }` to `whitelist.json` and commit.
   They get access within ~10 minutes. Delete the entry to remove them. `"enabled": false` locks everyone.
5. While testing, set `ENFORCE = false` in `WhitelistGate.java` to skip the whitelist.

## 2. Build the jar
Push this project to a GitHub repo. The included workflow (`.github/workflows/build.yml`) builds it.
Open the **Actions** tab, open the latest run, download **Artifacts**, and use `ragemod-1.0.0.jar`
(not the `-sources` jar). Put it in `.minecraft/mods` with Fabric Loader and Fabric API for 1.21.11.

Or build locally with JDK 21+: `./gradlew build` (jar ends up in `build/libs`).

## 3. Relay server (for mod users / private chat / help / war sync)
The relay is in `relay/`. It needs Node 18+.
```
cd relay
npm install
WHITELIST_URL="https://raw.githubusercontent.com/YOU/REPO/main/whitelist.json" node server.js
```
Host it somewhere always-on (a small VPS, Fly.io, Render, Railway). Put TLS in front so the address is `wss://...`
and set `"relay": "wss://your-host"` in `whitelist.json`. Players never have to type the URL.
Without a relay the rest of the mod still works; only the network features are offline.

## Keybinds (Options > Controls > RAGE Mod)
Right Shift = menu, Middle mouse = radial menu. Everything else is unbound by default: hitboxes, tracers,
anti-AFK, auto join, kill, ask for help, stats, players, mod users, config.

## Notes
- Kill tracking reads chat lines like `X was slain by Y`. If your server words kills differently, tell me the exact lines.
- Auto join repeats a command (set `autoJoinCommand`, or it repeats the last command you typed) until you join a new world.
- Config and data are saved in `.minecraft/config/ragemod/`.
