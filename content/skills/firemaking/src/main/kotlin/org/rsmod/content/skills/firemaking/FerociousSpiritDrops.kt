package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.repeated
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcGemDropRolls
import org.rsmod.api.death.NpcHerbDropRolls
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Exact dated cache-233 main drop table for the mangled-bones Ferocious spirit. */
public class FerociousSpiritDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            npcs.brut_angry_spirit_bad,
            NpcDropTable(
                weighted(
                    NpcWeightedDrop(5, item(FerociousSpiritObjs.mithrilMace)),
                    NpcWeightedDrop(4, item(FerociousSpiritObjs.adamantMace)),
                    NpcWeightedDrop(2, item(FerociousSpiritObjs.mithrilHasta)),
                    NpcWeightedDrop(1, item(FerociousSpiritObjs.runeMace)),
                    NpcWeightedDrop(8, item(objs.death_rune, 5..9)),
                    NpcWeightedDrop(4, item(objs.adamant_throwing_knife, 20..20)),
                    NpcWeightedDrop(4, item(objs.adamant_dart_p, 20..20)),
                    NpcWeightedDrop(4, item(objs.rune_arrow, 10..10)),
                    NpcWeightedDrop(4, item(objs.runite_bolts, 10..10)),
                    NpcWeightedDrop(10, repeated(2, NpcHerbDropRolls.standard())),
                    NpcWeightedDrop(5, item(FerociousSpiritObjs.ranarrSeed)),
                    NpcWeightedDrop(16, item(objs.prayer_mix_1)),
                    NpcWeightedDrop(8, item(objs.prayer_mix_2)),
                    NpcWeightedDrop(4, item(objs.antifire_mix_1)),
                    NpcWeightedDrop(4, item(objs.antifire_mix_2)),
                    NpcWeightedDrop(3, item(objs.superattack_mix_2)),
                    NpcWeightedDrop(3, item(objs.super_defence_mix_2)),
                    NpcWeightedDrop(2, item(objs.super_strength_mix_1)),
                    NpcWeightedDrop(2, item(objs.super_strength_mix_2)),
                    NpcWeightedDrop(2, item(objs.superattack_mix_1)),
                    NpcWeightedDrop(2, item(objs.super_defence_mix_1)),
                    NpcWeightedDrop(1, item(objs.antipoison_supermix_2)),
                    NpcWeightedDrop(1, item(objs.fishing_mix_2)),
                    NpcWeightedDrop(2, item(objs.yew_logs, 3..3)),
                    NpcWeightedDrop(2, item(FerociousSpiritObjs.notedSteelBar, 2..2)),
                    NpcWeightedDrop(2, item(FerociousSpiritObjs.notedMithrilBar, 2..2)),
                    NpcWeightedDrop(1, item(FerociousSpiritObjs.notedAdamantiteBar)),
                    NpcWeightedDrop(1, item(FerociousSpiritObjs.notedRuniteBar)),
                    NpcWeightedDrop(7, item(FerociousSpiritObjs.notedBigBones, 10..10)),
                    NpcWeightedDrop(6, item(objs.shark, 3..3)),
                    NpcWeightedDrop(
                        8,
                        NpcGemDropRolls.standard(
                            talisman = FerociousSpiritObjs.natureTalisman,
                            hasRingOfWealth = { context ->
                                FerociousSpiritObjs.ringsOfWealth.any(context.hero.worn::contains)
                            },
                            legendsQuestComplete = { context ->
                                context.hero.vars[varps.legends_quest] >= LEGENDS_QUEST_COMPLETE
                            },
                        ),
                    ),
                )
            ),
        )
    }

    public companion object {
        public const val MAIN_ROLL_WEIGHT: Int = 128
        public const val LEGENDS_QUEST_COMPLETE: Int = 75
    }
}

/** Cache-233 object references not shared by the base content catalogue. */
public object FerociousSpiritObjs : ObjReferences() {
    public val mithrilMace = find("mithril_mace")
    public val adamantMace = find("adamant_mace")
    public val mithrilHasta = find("brut_mithril_spear")
    public val runeMace = find("rune_mace")
    public val ranarrSeed = find("ranarr_seed")
    public val notedSteelBar = find("cert_steel_bar")
    public val notedMithrilBar = find("cert_mithril_bar")
    public val notedAdamantiteBar = find("cert_adamantite_bar")
    public val notedRuniteBar = find("cert_runite_bar")
    public val notedBigBones = find("cert_big_bones")
    public val natureTalisman = find("nature_talisman")

    public val ringOfWealth1 = find("ring_of_wealth_1")
    public val ringOfWealth2 = find("ring_of_wealth_2")
    public val ringOfWealth3 = find("ring_of_wealth_3")
    public val ringOfWealth4 = find("ring_of_wealth_4")
    public val ringOfWealth5 = find("ring_of_wealth_5")
    public val ringOfWealthI = find("ring_of_wealth_i")
    public val ringOfWealthI1 = find("ring_of_wealth_i1")
    public val ringOfWealthI2 = find("ring_of_wealth_i2")
    public val ringOfWealthI3 = find("ring_of_wealth_i3")
    public val ringOfWealthI4 = find("ring_of_wealth_i4")
    public val ringOfWealthI5 = find("ring_of_wealth_i5")

    public val ringsOfWealth: List<ObjType>
        get() =
            listOf(
                objs.ring_of_wealth,
                ringOfWealth1,
                ringOfWealth2,
                ringOfWealth3,
                ringOfWealth4,
                ringOfWealth5,
                ringOfWealthI,
                ringOfWealthI1,
                ringOfWealthI2,
                ringOfWealthI3,
                ringOfWealthI4,
                ringOfWealthI5,
            )
}
