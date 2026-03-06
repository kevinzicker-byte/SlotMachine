# SlotMachine

Custom slot machine plugin scaffold for Paper servers.

## Build

```bash
mvn clean package
```

Output jar:
- `target/SlotMachine-1.0.0.jar`

## Features in this scaffold
- /gamble GUI with three bet tiers
- animated 3x3 reel spin with middle payline
- jackpot pot, milestone broadcasts, and reset-to-base pot
- happy hour timer and manual controls
- player leaderboard and faction leaderboard (reflection hook for SaberFactions/Factions)
- DecentHolograms updater through server commands
- YAML-backed storage

## Notes
- Designed around your existing Skript feature set, but moved to a Java plugin structure.
- DecentHolograms holograms can be placed with:
  - `/gamble holo winners`
  - `/gamble holo factions`
- If SaberFactions/Factions is not present, faction leaderboard falls back gracefully.
