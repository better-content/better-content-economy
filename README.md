# Better Content Economy

Forge 1.20.1 mod making Malum spirits the pack's player-effort currency and village identity system.

- Commerce uses sacred, wicked, arcane, aerial, aqueous, earthen, and infernal spirits. Eldritch and umbral spirits retain their special Malum roles and never appear in trade.
- A living entity releases spirits only when the kill is credited to a player, that player's projectile or spell, or an owned creature. Spawner-origin mobs, villagers, traders, guards, golems, and players release none.
- Existing Malum mappings retain their authored drops. Any otherwise-unmapped hostile deterministically releases two ordinary spirits. Both paths spawn Malum's floating, homing `SpiritItemEntity`; no harvesting tool is required.
- Seven villager professions each have a matching coloured workstation, robe, utility behaviour, and 35-offer catalogue paid only in the matching spirit at costs of 1–8.
- Seven wandering trader identities each have a matching robe, 13 fixed goods, and one one-use offer of two matching spirits for two eggs already assigned to the corresponding profession.
- Scheduled wandering traders pitch a theme-coloured cloth awning on a tall camp post, stay near their stall, and leave when no offers remain. Pick up and re-place the linked post to send its trader to a new camp.
- Plague Doctors draw eight unique oddities from a 42-item mixed-spirit cabinet. Each doctor keeps its stock for one day, then rolls a fresh catalogue at dawn; missing optional-mod goods are skipped safely.
- `data/better_content_economy/economy/spirit_economy_policy.json` is the single loaded and validated authority for acquisition rules, 245 villager rows, 91 wandering goods, 42 Plague Doctor oddities, and retired item IDs.
- Create Deco coins and coin stacks, emerald-priced merchant offers, the old purse/wallet, and superseded Malum harvesting equipment are removed, inert, or hidden. Recipe reload also rejects non-kill recipes whose output is one of the seven commerce spirits.
- Malum's native Spirit Pouch is the specialist storage surface. Its pack recipe costs exactly three leather and two string.

Run `./gradlew verifyFull stageRuntimeJar` before committing or pushing.

## Trader camp screenshots

The non-shipping visual harness runs a dedicated server and a real Forge client through the production block entity renderer. Start `./gradlew runVisualServer --no-daemon`, then start `./gradlew runVisualClient --no-daemon` under Xvfb. In the server console, run:

```text
campvisual prepare <player>
campvisual capture <player> camps-overview
campvisual view <player> profile
campvisual capture <player> awning-profile
campvisual view <player> detail
campvisual capture <player> awning-detail
```

Captures are written to `run-visual-client/screenshots/`. Run `./gradlew verifyVisualHarness` to check the expected files, then inspect the images for cloth shape, clipping, lighting, and theme colors. The harness does not use synthetic player input.
