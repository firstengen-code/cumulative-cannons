# Cumulative Cannons

A Create Big Cannons addon for Minecraft 1.21.1 (NeoForge) that adds a cumulative jet shell.

On impact the shell does not make a normal explosion. It carves a narrow 2x2-pixel tunnel through blocks, damages entities in the jet path, and throws black fragments with bleeding. The tunnel length depends on the material: dirt and wood are cheap, stone is more expensive, obsidian and netherite stop the jet quickly. The penetration budget, maximum length, damage and effect settings are configurable.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.235
- Create 6.0.10
- Create Big Cannons 5.11.7
- LittleTiles 1.6.0-pre234
- CreativeCore 2.13.51
- Ritchie's Projectile Library 2.1.2

Optional:

- Explosion Overhaul 0.2.3.0 - concussion and deafness near the impact
- GeckoLib 4.9.2 and Cloth Config 15.0.140 - required by Explosion Overhaul

The exact dependency jars used for development are vendored in `libs/`.

## Features

- 2x2-pixel tunnel (1 pixel = 1/16 block), up to 25 blocks by default.
- Material-based penetration budget; values are data-driven.
- Armor-piercing jet damage with distance falloff.
- Entry and exit shockwaves.
- Black shrapnel with up to 2 ricochets, capped fragment damage and bleeding.
- CBC HE-style particles, a 15-block exit beam and a smoke trail that fades over several seconds.
- Effects are visible to players within 200 blocks.
- Optional Explosion Overhaul integration: concussion within 5 blocks, deafness up to 10 blocks.

## Build

Use Java 21:

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew build
```

The jar is written to `build/libs/cumulative_cannons-0.2.0.jar`.

## Run a test client

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew runClient
```

The shell is currently available in the Combat creative tab. Crafting is not implemented yet.

## Configuration

The server config is `config/cumulative_cannons-server.toml`.

Jet:

- `budget` and `max_blocks` - penetration budget and hard length limit
- `layers_per_tick` - carving speed
- `carve_enabled`, `respect_grief`, `respect_protection` - safety toggles
- `default_cost` - fallback material cost

Effects:

- `effects_enabled`, `effect_multiplier`, `effect_limit`
- `camera_shake`, `camera_shake_strength`
- `impact_light`

Combat:

- `damage_enabled`
- `direct_hit_damage`, `jet_damage`
- `shockwave_radius`, `shockwave_entry_damage`, `shockwave_exit_damage`
- `shrapnel_damage`, `shrapnel_max_damage`, `shrapnel_radius`, `shrapnel_angle`
- `shrapnel_bounces`, `shrapnel_range`
- `bleeding_duration`, `bleeding_interval`, `bleeding_damage`

Material costs can be changed in `data/cumulative_cannons/cumulative_jet_materials/materials.json`.

## Status

Version 0.2.0. The projectile, carving, penetration, damage, shrapnel, bleeding, CBC effects and optional Explosion Overhaul integration are implemented. Crafting, final model/texture and multiplayer polish are still to do.
