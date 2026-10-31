# BastiChallenges (Paper 26.3)

Konfigurierbares Challenge-Plugin: Timer, globaler Randomizer, Instant Force Item / Force Mob,
Monster Army Battle (Sammeln -> 3 Wellen bauen -> Zeitkampf), `/result`-Leaderboard, Arena-Boxen.

## Bauen
- Java 25, Gradle
- `gradle build` (Gradle 9.1+, kein Wrapper enthalten) -> `build/libs/BastiChallenges-1.0.0.jar` nach `plugins/`
- Ohne lokale Installation: Projekt in ein GitHub-Repo pushen, der Workflow baut die JAR (Actions -> Artifact `BastiChallenges-jar`)
- Paper-API-Version steht in `build.gradle.kts` (`26.3.build.143-beta`). Falls die Build-Nummer
  nicht mehr aufloest, aktuelle Nummer von https://docs.papermc.io nehmen.

## Befehle (Alias /basti, /ch)
- `/challenge timer <start|pause|resume|stop|reset|settime <zeit>|direction <up|down>>`
- `/challenge mode <forceitem|forcemob|armybattle|forceskill> <on|off>`
- `/challenge randomizer <on|off>` | `<modus> <on|off>` | `seed [zahl]`
- `/challenge settings` (GUI)
- `/challenge arena <setspawn|buildbox [teleport]|clear>`
- `/challenge army <collect|endcollect|build|startfight [force]|stop|status>`
- `/result [forceitem|forcemob|army]`, `/forceitem skip`, `/forcemob skip`

## Army-Battle-Ablauf
1. `/challenge arena setspawn` (Origin der Arenen, Boxen wachsen in +X)
2. `/challenge mode armybattle on`, dann `/challenge army collect` (Countdown = `army-battle.collect-seconds`)
3. Nach Ablauf (oder `/challenge army endcollect`) oeffnet sich das Wellen-GUI (`/challenge army build`)
4. `/challenge army startfight` -> jeder kaempft in eigener Arena gegen die 3 Wellen seines Gegners
5. Schnellste Gesamtzeit (inkl. Todes-Strafe) gewinnt, Ergebnis per `/result army`
