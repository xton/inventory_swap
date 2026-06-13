# InventorySwap

A PaperMC plugin that lets players keep several separate inventory "profiles"
(e.g. one per building project) and switch between them in-game.

## How it works

1. Place a sign and write `[INV]` on the first line and a profile name on the
   second line, e.g.:

   ```
   [INV]
   castle
   ```

   As soon as you finish editing it, the plugin recognizes the sign and
   recolors it (`[INV]` turns gray and italic, the profile name turns bold
   dark green) so you can tell at a glance that it's "live".

2. Right-click the sign **or** a container (barrel, chest, etc.) that the
   sign is mounted on top of or attached to. The plugin will:
   - Stash your current inventory, armor, and offhand item into your
     currently-active profile.
   - Load the named profile's contents (creating an empty one the first time
     you switch to it).
   - Show a title card and play a sound to confirm the swap.

   Right-clicking a sign/barrel for the profile you're already on just shows
   an action-bar message instead of swapping anything.

Each player starts on a profile called `default`.

## Admin commands (`/inv`, requires `inventoryswap.admin`, default: op)

`/invswap` also works as an alias.

- `/inv list [player]` - list a player's profiles
- `/inv current [player]` - show a player's active profile
- `/inv swap <profile> [player]` - swap a player's active inventory
- `/inv create <profile> [player]` - create an empty profile
- `/inv delete <profile> [player]` - delete a profile (must not be active)
- `/inv rename <old> <new> [player]` - rename a profile

`player` defaults to the command sender when omitted.

## Building

```
./gradlew build
```

The plugin jar is written to `build/libs/`.

## Testing

### Unit / functional tests (MockBukkit)

```
./gradlew test
```

These run the plugin's logic against a simulated server (no real Minecraft
server required) and cover profile persistence, sign/barrel swap triggers,
sign styling, and the admin commands.

### Docker smoke test

```
./gradlew dockerSmokeTest
```

or directly:

```
./docker/smoke-test.sh
```

This builds the plugin, boots a real Paper 1.21 server in Docker with the
jar installed, and checks (via the server log and RCON) that the server
starts cleanly and the plugin enables without errors. Requires Docker.
