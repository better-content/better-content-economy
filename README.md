# Better Content Economy

Forge 1.20.1 mod owning the pack's coin-purse policy and merchant currency boundary.

- The existing `better_content_economy:coin_purse` Curios storage remains seven coin-only slots and is shown as an attached 2x4 panel in the survival inventory (the eighth cell is intentionally inactive).
- Ground-picked direct Create Deco coins route into the purse before ordinary player inventory. Coin stacks and non-coin items are unaffected.
- Every themed wandering trader receives one guaranteed, one-use offer of two ordinary villager spawn eggs for eight copper coins, in addition to its themed catalogue.
- Offers exposed by non-vanilla `AbstractVillager` merchants replace exact emerald stacks in either cost position or the result with the same number of Create Deco copper coins. Other items and all offer metadata are preserved.
- `createdeco` and Curios are mandatory runtime dependencies because they define the currency and persistent purse storage respectively.
