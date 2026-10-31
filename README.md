# Cumulative Cannons

A Create Big Cannons addon for Minecraft 1.21.1 (NeoForge) that adds a cumulative jet shell.

On impact the shell does not make a normal explosion. It carves a narrow 2x2-pixel tunnel through blocks, follows the actual shot direction (including diagonal), damages entities in the jet path, and throws light-gray fragments with bleeding. The tunnel length depends on the material: dirt and wood are cheap, stone is more expensive, obsidian and netherite stop the jet quickly. The penetration budget, maximum length, damage and effect settings are configurable.

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
- Create Aeronautics (Sable) - the jet pierces contraption blocks too; lighting inside a sublevel is handled by a light bridge plus a shade fix (see below)

The exact dependency jars used for development are vendored in `libs/`.

## Features

- 2x2-pixel tunnel (1 pixel = 1/16 block), up to 25 blocks by default.
- Ray-carved tunnel that follows diagonal shots, not just one world axis.
- Explicit entrance hole: the outer pixel layer of the entry face is always punched, so diagonal shots do not leave an intact skin.
- Create Aeronautics (Sable) contraptions: blocks are turned into tiles inside the plot, lit through a light bridge, and shaded exactly once (Sable applies its own directional shading in the shader, so the baked vanilla shade is suppressed).
- Material-based penetration budget; values are data-driven.
- Armor-piercing jet damage with distance falloff.
- Entry and exit shockwaves.
- Light-gray shrapnel with up to 2 ricochets, capped fragment damage and bleeding.
- CBC HE-style particles, a 15-block exit beam and a smoke trail that fades over several seconds.
- Effects are visible to players within 200 blocks.
- Optional Explosion Overhaul integration: concussion within 5 blocks, deafness up to 10 blocks.

## Build

Use Java 21:

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew build
```

The jar is written to `build/libs/cumulative_cannons-0.4.0.jar`.

## Run a test client

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew runClient
```

The shell is currently available in the Combat creative tab. Crafting is not implemented yet.

## Configuration

The config is `config/cumulative_cannons-server.toml`. It can also be opened in game from **Mods > Cumulative Cannons > Settings**; numeric values use sliders. The button **Block penetration** opens a searchable per-block cost editor, saved to `config/cumulative_cannons-materials.json`.

Jet:

- `budget` and `max_blocks` - penetration budget and hard length limit
- `layers_per_tick` - carving speed (16 layers = one block)
- `carve_enabled`, `respect_grief`, `respect_protection` - safety toggles
- `default_cost` - fallback material cost
- `penetration_multiplier` - global multiplier for every block cost (1.0 = normal)

Effects:

- `effects_enabled`, `effect_multiplier`, `effect_limit`
- `smoke_internal_black`, `smoke_internal_white`, `smoke_external_black`, `smoke_external_white` - white/black smoke amounts (0 = off, 100 = default, 300 = triple)
- `smoke_explosion_cloud` - CBC explosion cloud amount (0 = off, 100 = default, 300 = triple)
- `camera_shake`, `camera_shake_strength`
- `impact_light`

Combat:

- `damage_enabled`
- `direct_hit_damage`, `jet_damage`
- `shockwave_radius`, `shockwave_entry_damage`, `shockwave_exit_damage`
- `shrapnel_damage`, `shrapnel_max_damage`, `shrapnel_radius`, `shrapnel_angle`
- `shrapnel_bounces`, `shrapnel_range`
- `bleeding_duration`, `bleeding_interval`, `bleeding_damage`

Material costs can be changed in `data/cumulative_cannons/cumulative_jet_materials/materials.json` for mod defaults/tags, or per block in `config/cumulative_cannons-materials.json` (also editable from the in-game **Block penetration** screen).

## Create Aeronautics (Sable)

On a contraption the struck block cannot be removed normally: it lives in a Sable plot, so the mod replaces it with LittleTiles tiles and removes only the 2x2 pixel boxes. Two compatibility layers make that visible and correct:

- **Light bridge** (`SableLightBridge`): LittleTiles receives a `BlockAndTintGetter` that maps plot-local coordinates to world coordinates for sky/block light.
- **Shade fix**: Sable disables vanilla directional shading inside sublevels (`ModelBlockRenderer.getShade` returns 1.0) and applies its own shading in the shader. LittleTiles bakes through NeoForge `QuadLighter`, which would multiply the vertex color by the vanilla shade as well, so the tiles were shaded twice and looked dark. The bridge therefore returns `1.0` from `getShade` and the shader applies the only shade.

Both are reflection/mixin based and do nothing when Sable is absent.

## Status

Version 0.4.0. The projectile, diagonal ray carving with an explicit entrance hole, per-block penetration settings, in-game config screen, smoke test blocks, damage, shrapnel, bleeding, CBC effects, optional Explosion Overhaul integration and Create Aeronautics (Sable) contraption support are implemented. Crafting, final model/texture and multiplayer polish are still to do.
