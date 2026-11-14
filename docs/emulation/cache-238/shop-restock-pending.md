# Shop restock rates pending

The 2026-06-10 OSRS Wiki shop revisions list quantities and items for these shops but omit the restock interval for the items below. Their stock records use `restockCycles = 0` to keep the items available at the published initial quantity while disabling automatic replenishment after purchase. Other items in the same shops retain their published positive intervals. `ShopRestockProcess` skips nonpositive intervals before the modulo calculation.

| Shop | Items with unpublished restock rate |
| --- | --- |
| Moonrise Wines | All eight wines |
| Conara's Jewels | Sapphire, Emerald, Ruby |
| Salius' Armour Shop | Mithril platebody, Adamant platebody |
| Spike's Spikes | Bronze mace, Iron mace, Steel mace, Mithril mace, Adamant mace, Rune mace |
| Stonecutter Supplies | Condensed gold |
| Durrik's Goods | Rune warhammer |
| Elder Blunn's Spear and Shield Stall | Steel spear, Mithril spear, Adamant spear, Steel kiteshield, Mithril kiteshield, Adamant kiteshield |
| Elder Coco's Crafting Stall | Sapphire, Emerald, Ruby, Diamond |

The source revision ID for each shop is recorded in `shops.tsv`. Replace a zero interval only when a historical source confirms the interval.
