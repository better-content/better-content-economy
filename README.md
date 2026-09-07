# Better Content Economy

Forge 1.20.1 mod owning the pack's coin-purse policy and merchant currency boundary.

- The existing `better_content_economy:coin_purse` Curios storage remains seven coin-only slots and is shown as an attached 2x4 panel in the survival inventory (the eighth cell is intentionally inactive).
- Ground-picked direct Create Deco coins route into the purse before ordinary player inventory. Coin stacks and non-coin items are unaffected.
- Every themed wandering trader receives one guaranteed, one-use offer of two ordinary villager spawn eggs for eight copper coins, in addition to its themed catalogue.
- Offers exposed by non-vanilla `AbstractVillager` merchants replace exact emerald stacks in either cost position or the result with the same number of Create Deco copper coins. Other items and all offer metadata are preserved.
- `createdeco` and Curios are mandatory runtime dependencies because they define the currency and persistent purse storage respectively.

## External merchant audit

The installed Rats Plague Doctor catalogue uses emeralds across its item-for-emerald,
emerald-for-item, and two-input offers. Supplementaries' Red Merchant JSON catalogue
also uses emerald primary costs. Both entities extend `AbstractVillager`, so the shared
normalization boundary covers current offers and offers loaded from saved entity data
without compile-time dependencies on either mod. Trading Post sees those already-
normalized merchant offers.

Supplementaries' vanilla-villager and wandering-trader additions are superseded by the
authoritative Better Content catalogues. Wares tables are already coin-denominated.
The audited Bumblezone Queen, Ice and Fire Myrmex, and Occultism spirit catalogues use
their own non-emerald currencies and therefore remain unchanged.
