# Cumulative Cannons

A Create Big Cannons addon for Minecraft 1.21.1 (NeoForge) that adds a cumulative jet shell.

On impact the shell does not make a normal explosion. It carves a narrow 2x2-pixel tunnel through blocks and leaves a short-lived smoke trail. The tunnel length depends on the material: dirt and wood are cheap, stone is more expensive, obsidian and netherite stop the jet quickly. The penetration budget, maximum length and effect settings are configurable.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.235
- Create 6.0.10
- Create Big Cannons 5.11.7
- LittleTiles 1.6.0-pre234
- CreativeCore 2.13.51
- Ritchie's Projectile Library 2.1.2

The exact dependency jars used for development are vendored in `libs/`.

## Build

Use Java 21:

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew build
```

The jar is written to `build/libs/cumulative_cannons-0.1.0.jar`.

## Run a test client

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew runClient
```

The shell is currently available in the Combat creative tab. Crafting is not implemented yet.

## Configuration

The server config is `config/cumulative_cannons-server.toml`. It controls:

- `budget` and `max_blocks` - jet length and penetration
- `layers_per_tick` - carving speed
- `carve_enabled`, `respect_grief`, `respect_protection` - safety toggles
- `effect_multiplier`, `effect_limit` - particle density
- `camera_shake` and `camera_shake_strength`
- `impact_light`

Material costs can be changed in `data/cumulative_cannons/cumulative_jet_materials/materials.json`.

## Status

Work in progress. The projectile, carving, penetration budget and CBC-based impact/exit effects are implemented. Crafting, final model/texture and multiplayer polish are still to do.
