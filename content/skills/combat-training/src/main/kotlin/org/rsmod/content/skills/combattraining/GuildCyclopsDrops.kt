package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.clue.Cache233ClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.*
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.firstSelected
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.nothing
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.player.ring
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CyclopsItems : ObjReferences() {
    val blackKnife = find("black_knife")
    val steelChainbody = find("steel_chainbody")
    val iron2hSword = find("iron_2h_sword")
    val ironChainbody = find("iron_chainbody")
    val steelDagger = find("steel_dagger")
    val steelMace = find("steel_mace")
    val steelSword = find("steel_sword")
    val steelBattleaxe = find("steel_battleaxe")
    val steel2hSword = find("steel_2h_sword")
    val steelLongsword = find("steel_longsword")
    val steelMedHelm = find("steel_med_helm")
    val black2hSword = find("black_2h_sword")
    val mithrilDagger = find("mithril_dagger")
    val mithrilLongsword = find("mithril_longsword")
    val adamantMace = find("adamant_mace")
    val blackSword = find("black_sword")
    val blackLongsword = find("black_longsword")
    val blackDagger = find("black_dagger")
    val adamant2hSword = find("adamant_2h_sword")
    val coins = find("coins")
    val mithrilDart = find("mithril_dart")
    val adamantDagger = find("adamant_dagger")
    val blackFullHelm = find("black_full_helm")
    val blackMace = find("black_mace")
    val mithrilScimitar = find("mithril_scimitar")
    val mithrilKiteshield = find("mithril_kiteshield")
    val steelPlatebody = find("steel_platebody")
    val mithrilPlatelegs = find("mithril_platelegs")
    val runeMedHelm = find("rune_med_helm")
    val adamantSqShield = find("adamant_sq_shield")
    val runeFullHelm = find("rune_full_helm")
    val lobster = find("lobster")
    val limpwurtSeed = find("limpwurt_seed")
    val strawberrySeed = find("strawberry_seed")
    val marrentillSeed = find("marrentill_seed")
    val jangerberrySeed = find("jangerberry_bush_seed")
    val tarrominSeed = find("tarromin_seed")
    val wildbloodSeed = find("wildblood_hop_seed")
    val watermelonSeed = find("watermelon_seed")
    val harralanderSeed = find("harralander_seed")
    val snapeGrassSeed = find("snape_grass_seed")
    val ranarrSeed = find("ranarr_seed")
    val whiteberrySeed = find("whiteberry_bush_seed")
    val mushroomSpore = find("mushroom_seed")
    val toadflaxSeed = find("toadflax_seed")
    val belladonnaSeed = find("belladonna_seed")
    val iritSeed = find("irit_seed")
    val poisonIvySeed = find("poisonivy_bush_seed")
    val avantoeSeed = find("avantoe_seed")
    val cactusSeed = find("cactus_seed")
    val kwuarmSeed = find("kwuarm_seed")
    val potatoCactusSeed = find("potato_cactus_seed")
    val snapdragonSeed = find("snapdragon_seed")
    val cadantineSeed = find("cadantine_seed")
    val lantadymeSeed = find("lantadyme_seed")
    val dwarfWeedSeed = find("dwarf_weed_seed")
    val torstolSeed = find("torstol_seed")
    val natureRune = find("naturerune")
    val adamantJavelin = find("adamant_javelin")
    val deathRune = find("deathrune")
    val lawRune = find("lawrune")
    val runeArrow = find("rune_arrow")
    val steelArrow = find("steel_arrow")
    val rune2hSword = find("rune_2h_sword")
    val runeBattleaxe = find("rune_battleaxe")
    val runeSqShield = find("rune_sq_shield")
    val dragonMedHelm = find("dragon_med_helm")
    val runeKiteshield = find("rune_kiteshield")
    val loopHalfOfKey = find("keyhalf2")
    val toothHalfOfKey = find("keyhalf1")
    val runiteBar = find("runite_bar")
    val dragonstone = find("dragonstone")
    val silverOre = find("cert_silver_ore")
    val natureTalisman = find("nature_talisman")
    val chaosTalisman = find("chaos_talisman")
}

class GuildCyclopsDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val clues: ClueInventory,
    private val clueSteps: Cache233ClueSteps,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((basement, types) in GuildEnemies.cyclops.groupBy { it.id in 2137..2142 }) {
            val main =
                if (basement)
                    firstSelected(
                        independent(denominator = 100, roll = defender(true)),
                        basementTable(),
                    )
                else roofTable()
            val tier = if (basement) 2 else 1
            registry.register(
                types,
                NpcDropTable(
                    conditional(
                        { it.hero.guildCyclopsRoom == tier },
                        sequence(
                            main,
                            item(objs.big_bones),
                            NpcRareBoneDropRolls.longOrCurved(),
                            hardClue(if (basement) 256 else 512),
                        ),
                    )
                ),
            )
        }
    }

    private fun defender(basement: Boolean) = NpcDropRoll { context ->
        val tier = if (basement) 7 else context.hero.guildDefenderTier.coerceIn(0, 6)
        item(GuildTrainingItems.daggers[tier]).roll(context)
    }

    private fun hardClue(denominator: Int) = NpcDropRoll { context ->
        val state = clues.state(context.hero, ClueTier.Hard)
        if (
            !context.hero.members ||
                (!state.xMarksComplete && !state.canReceive) ||
                context.random.of(denominator) != 0
        )
            return@NpcDropRoll NpcDropRollResult.Miss
        val reward =
            clues.reward(state, ClueTier.Hard) {
                clueSteps.select(
                    ClueTier.Hard,
                    context.random.of(clueSteps.steps(ClueTier.Hard).size),
                )
            }
        val drop =
            reward.obj?.let {
                NpcDrop(InvObj(it), context.coords, context.hero, reward.groundDuration)
            }
        NpcDropRollResult(true, listOfNotNull(drop), listOfNotNull(reward.message))
    }

    private fun roofTable() =
        weighted(
            NpcWeightedDrop(2, defender(false)),
            NpcWeightedDrop(7, nothing()),
            NpcWeightedDrop(16, item(CyclopsItems.blackKnife, 4..13)),
            NpcWeightedDrop(2, item(CyclopsItems.steelChainbody)),
            NpcWeightedDrop(2, item(CyclopsItems.iron2hSword)),
            NpcWeightedDrop(2, item(CyclopsItems.ironChainbody)),
            NpcWeightedDrop(2, item(CyclopsItems.steelDagger)),
            NpcWeightedDrop(2, item(CyclopsItems.steelMace)),
            NpcWeightedDrop(2, item(CyclopsItems.steelSword)),
            NpcWeightedDrop(2, item(CyclopsItems.steelBattleaxe)),
            NpcWeightedDrop(2, item(CyclopsItems.steel2hSword)),
            NpcWeightedDrop(2, item(CyclopsItems.steelLongsword)),
            NpcWeightedDrop(2, item(CyclopsItems.steelMedHelm)),
            NpcWeightedDrop(1, item(CyclopsItems.black2hSword)),
            NpcWeightedDrop(1, item(CyclopsItems.mithrilDagger)),
            NpcWeightedDrop(1, item(CyclopsItems.mithrilLongsword)),
            NpcWeightedDrop(1, item(CyclopsItems.adamantMace)),
            NpcWeightedDrop(1, item(CyclopsItems.blackSword)),
            NpcWeightedDrop(1, item(CyclopsItems.blackLongsword)),
            NpcWeightedDrop(1, item(CyclopsItems.blackDagger)),
            NpcWeightedDrop(1, item(CyclopsItems.adamant2hSword)),
            NpcWeightedDrop(31, item(CyclopsItems.coins, 3..102)),
            NpcWeightedDrop(10, item(CyclopsItems.coins, 5..204)),
            NpcWeightedDrop(3, NpcHerbDropRolls.standard()),
            NpcWeightedDrop(1, seeds()),
            NpcWeightedDrop(2, standardCombatGems(CyclopsItems.natureTalisman)),
        )

    private fun basementTable() =
        weighted(
            NpcWeightedDrop(4, item(CyclopsItems.adamant2hSword)),
            NpcWeightedDrop(6, item(CyclopsItems.mithrilDart, 12..12)),
            NpcWeightedDrop(5, item(CyclopsItems.adamantDagger)),
            NpcWeightedDrop(5, item(CyclopsItems.blackFullHelm)),
            NpcWeightedDrop(5, item(CyclopsItems.blackMace)),
            NpcWeightedDrop(5, item(CyclopsItems.mithrilScimitar)),
            NpcWeightedDrop(5, item(CyclopsItems.mithrilKiteshield)),
            NpcWeightedDrop(5, item(CyclopsItems.steelPlatebody)),
            NpcWeightedDrop(7, item(CyclopsItems.steelChainbody)),
            NpcWeightedDrop(4, item(CyclopsItems.blackKnife, 22..22)),
            NpcWeightedDrop(4, item(CyclopsItems.mithrilPlatelegs)),
            NpcWeightedDrop(5, item(CyclopsItems.blackDagger)),
            NpcWeightedDrop(3, item(CyclopsItems.runeMedHelm)),
            NpcWeightedDrop(1, item(CyclopsItems.adamantSqShield)),
            NpcWeightedDrop(1, item(CyclopsItems.runeFullHelm)),
            NpcWeightedDrop(11, item(CyclopsItems.coins, 96..96)),
            NpcWeightedDrop(8, item(CyclopsItems.coins, 350..449)),
            NpcWeightedDrop(7, item(CyclopsItems.lobster)),
            NpcWeightedDrop(4, NpcHerbDropRolls.standard()),
            NpcWeightedDrop(3, seeds()),
            NpcWeightedDrop(2, rareTable()),
        )

    private fun seeds() =
        weighted(
            NpcWeightedDrop(2, nothing()),
            NpcWeightedDrop(137, item(CyclopsItems.limpwurtSeed)),
            NpcWeightedDrop(131, item(CyclopsItems.strawberrySeed)),
            NpcWeightedDrop(125, item(CyclopsItems.marrentillSeed)),
            NpcWeightedDrop(92, item(CyclopsItems.jangerberrySeed)),
            NpcWeightedDrop(85, item(CyclopsItems.tarrominSeed)),
            NpcWeightedDrop(83, item(CyclopsItems.wildbloodSeed)),
            NpcWeightedDrop(63, item(CyclopsItems.watermelonSeed)),
            NpcWeightedDrop(56, item(CyclopsItems.harralanderSeed)),
            NpcWeightedDrop(40, item(CyclopsItems.snapeGrassSeed)),
            NpcWeightedDrop(39, item(CyclopsItems.ranarrSeed)),
            NpcWeightedDrop(34, item(CyclopsItems.whiteberrySeed)),
            NpcWeightedDrop(29, item(CyclopsItems.mushroomSpore)),
            NpcWeightedDrop(27, item(CyclopsItems.toadflaxSeed)),
            NpcWeightedDrop(18, item(CyclopsItems.belladonnaSeed)),
            NpcWeightedDrop(18, item(CyclopsItems.iritSeed)),
            NpcWeightedDrop(13, item(CyclopsItems.poisonIvySeed)),
            NpcWeightedDrop(12, item(CyclopsItems.avantoeSeed)),
            NpcWeightedDrop(12, item(CyclopsItems.cactusSeed)),
            NpcWeightedDrop(9, item(CyclopsItems.kwuarmSeed)),
            NpcWeightedDrop(8, item(CyclopsItems.potatoCactusSeed)),
            NpcWeightedDrop(5, item(CyclopsItems.snapdragonSeed)),
            NpcWeightedDrop(4, item(CyclopsItems.cadantineSeed)),
            NpcWeightedDrop(3, item(CyclopsItems.lantadymeSeed)),
            NpcWeightedDrop(2, item(CyclopsItems.dwarfWeedSeed)),
            NpcWeightedDrop(1, item(CyclopsItems.torstolSeed)),
        )

    private fun rareTable(): NpcDropRoll {
        val megaItems =
            listOf(
                NpcWeightedDrop(8, item(NpcGemDropObjs.runeSpear)),
                NpcWeightedDrop(4, item(NpcGemDropObjs.shieldLeftHalf)),
                NpcWeightedDrop(3, item(NpcGemDropObjs.dragonSpear)),
            )
        val regular = weighted(megaItems + NpcWeightedDrop(113, nothing()))
        val lucky = weighted(megaItems)
        return weighted(
            NpcWeightedDrop(3, item(CyclopsItems.natureRune, 67..67)),
            NpcWeightedDrop(2, item(CyclopsItems.adamantJavelin, 20..20)),
            NpcWeightedDrop(2, item(CyclopsItems.deathRune, 45..45)),
            NpcWeightedDrop(2, item(CyclopsItems.lawRune, 45..45)),
            NpcWeightedDrop(2, item(CyclopsItems.runeArrow, 42..42)),
            NpcWeightedDrop(2, item(CyclopsItems.steelArrow, 150..150)),
            NpcWeightedDrop(3, item(CyclopsItems.rune2hSword)),
            NpcWeightedDrop(3, item(CyclopsItems.runeBattleaxe)),
            NpcWeightedDrop(2, item(CyclopsItems.runeSqShield)),
            NpcWeightedDrop(1, item(CyclopsItems.dragonMedHelm)),
            NpcWeightedDrop(1, item(CyclopsItems.runeKiteshield)),
            NpcWeightedDrop(21, item(CyclopsItems.coins, 3000..3000)),
            NpcWeightedDrop(20, item(CyclopsItems.loopHalfOfKey)),
            NpcWeightedDrop(20, item(CyclopsItems.toothHalfOfKey)),
            NpcWeightedDrop(5, item(CyclopsItems.runiteBar)),
            NpcWeightedDrop(2, item(CyclopsItems.dragonstone)),
            NpcWeightedDrop(2, item(CyclopsItems.silverOre, 100..100)),
            NpcWeightedDrop(20, standardCombatGems(CyclopsItems.chaosTalisman)),
            NpcWeightedDrop(
                15,
                NpcDropRoll { ctx -> (if (hasWealth(ctx)) lucky else regular).roll(ctx) },
            ),
        )
    }
}

internal fun hasWealth(context: NpcDropContext): Boolean {
    val id = context.hero.ring?.id ?: return false
    return id == 2572 || id == 12785 || (id in 11980..11988 && id % 2 == 0) || id in 20786..20790
}

internal fun standardCombatGems(talisman: ObjType) =
    NpcGemDropRolls.standard(talisman, ::hasWealth) { it.hero.vars[varps.legends_quest] >= 75 }
