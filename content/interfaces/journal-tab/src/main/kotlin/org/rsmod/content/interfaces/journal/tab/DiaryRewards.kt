package org.rsmod.content.interfaces.journal.tab

import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.syncDiaryTier
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

data class DiaryLamp(val item: ObjType, val minimumLevel: Int, val xp: Double)

object DiaryRewardItems : ObjReferences() {
    private val equipmentNames =
        mapOf(
            DiaryRegion.Ardougne to "ardy_cape",
            DiaryRegion.Desert to "desert_amulet",
            DiaryRegion.Falador to "falador_shield",
            DiaryRegion.Fremennik to "fremennik_boots",
            DiaryRegion.Kandarin to "seers_headband",
            DiaryRegion.Karamja to "atjun_gloves",
            DiaryRegion.Kourend to "zeah_blessing",
            DiaryRegion.Lumbridge to "lumbridge_ring",
            DiaryRegion.Morytania to "morytania_legs",
            DiaryRegion.Varrock to "varrock_armour",
            DiaryRegion.Western to "western_banner",
            DiaryRegion.Wilderness to "wilderness_sword",
        )
    val equipment =
        equipmentNames.mapValues { (region, prefix) ->
            DiaryDifficulty.entries.map { tier ->
                val suffix =
                    if (region == DiaryRegion.Karamja && tier == DiaryDifficulty.Medium) "med"
                    else tier.name.lowercase()
                find("${prefix}_$suffix")
            }
        }
    val museumLamp = DiaryLamp(find("vm_reward_digsite"), 10, 500.0)
    val ordinary =
        listOf(
            DiaryLamp(find("diary_easy_lamp"), 30, 2500.0),
            DiaryLamp(find("diary_med_lamp"), 40, 7500.0),
            DiaryLamp(find("diary_hard_lamp"), 50, 15000.0),
            DiaryLamp(find("diary_elite_lamp"), 70, 50000.0),
        )
    val karamja =
        listOf(
            DiaryLamp(find("atjun_easy_lamp"), 1, 1000.0),
            DiaryLamp(find("atjun_med_lamp"), 30, 5000.0),
            DiaryLamp(find("atjun_hard_lamp"), 40, 10000.0),
            ordinary.last(),
        )
}

object DiaryRewards {
    fun claim(access: ProtectedAccess, region: DiaryRegion): Boolean =
        with(access) {
            if (!player.members) {
                mes("Achievement Diary rewards are available on members' worlds.")
                return false
            }
            val tier = DiaryDifficulty.entries.firstOrNull { vars[region.tier(it).reward] == 0 }
            if (tier == null) {
                if (replaceLostEquipment(access, region, DiaryDifficulty.Elite)) return true
                mes("You have already claimed all four tiers of this diary.")
                return false
            }
            player.syncDiaryTier(region, tier)
            val state = region.tier(tier)
            if (vars[state.complete] == 0) {
                val previous = DiaryDifficulty.entries.getOrNull(tier.ordinal - 1)
                if (previous != null && replaceLostEquipment(access, region, previous)) return true
                mes(
                    "Complete the ${tier.name.lowercase()} tasks in the ${region.name} diary to claim your next reward."
                )
                return false
            }

            val equipment = DiaryRewardItems.equipment.getValue(region)
            val lowerTiers = equipment.take(tier.ordinal).map { it.id }.toSet()
            val upgrade = equipment[tier.ordinal]
            val lamps =
                if (region == DiaryRegion.Karamja) DiaryRewardItems.karamja
                else DiaryRewardItems.ordinary
            val inventories = listOf(inv, worn, bank)
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    var upgraded = false
                    for (source in inventories) {
                        val slots = source.indices.filter { source[it]?.id in lowerTiers }
                        if (slots.isEmpty()) continue
                        val target = if (source === inv) bag else select(source)
                        for (slot in slots) {
                            val previous = requireNotNull(source[slot])
                            delete(target, previous.id, previous.count, slot = slot)
                            add(target, upgrade.id, previous.count, slot = slot)
                            upgraded = true
                        }
                    }
                    if (!upgraded) add(bag, upgrade.id, 1)
                    add(bag, lamps[tier.ordinal].item.id, 1)
                }
            if (!result.success) {
                mes("Make room in your inventory for your diary rewards.")
                return false
            }
            vars[state.reward] = 1
            player.rebuildAppearance()
            mes("You have claimed your ${tier.name.lowercase()} ${region.name} diary rewards.")
            true
        }

    private fun replaceLostEquipment(
        access: ProtectedAccess,
        region: DiaryRegion,
        tier: DiaryDifficulty,
    ): Boolean =
        with(access) {
            val item = DiaryRewardItems.equipment.getValue(region)[tier.ordinal]
            if (item in inv || item in worn || item in bank) return false
            if (!invAdd(inv, item).success) return false
            mes(
                "You receive a replacement for your ${tier.name.lowercase()} ${region.name} diary equipment."
            )
            true
        }
}

class DiaryLamps : PluginScript() {
    override fun ScriptContext.startup() {
        for (lamp in (DiaryRewardItems.ordinary + DiaryRewardItems.karamja + DiaryRewardItems.museumLamp).distinct()) {
            onOpHeld1(lamp.item) { rub(lamp, it.slot) }
        }
    }

    private suspend fun ProtectedAccess.rub(lamp: DiaryLamp, slot: Int) {
        if (!player.members || inv[slot]?.id != lamp.item.id) return
        vars[varps.if1] = lamp.minimumLevel
        ifOpenMainModal(DiaryInterfaces.xpReward)
        ifSetEvents(DiaryComponents.skillChoices, choices.indices, IfEvent.PauseButton)
        val choice = awaitMainModalSelection(DiaryComponents.skillChoices)
        ifClose()
        val stat = choices.getOrNull(choice) ?: return
        if (statBase(stat) < lamp.minimumLevel) {
            mes("You need level ${lamp.minimumLevel} in that skill to use this lamp.")
            return
        }
        // TODO(quests): quest scripts should advance the same saved unlocks used by clientscript
        // 3809.
        if (
            stat == stats.herblore && vars[varps.druidic_ritual] < 4 ||
                stat == stats.runecrafting && vars[varps.runemysteries] < 6 ||
                stat == stats.construction && vars[varbits.poh_house_location] == 0
        ) {
            mes("You haven't unlocked that skill yet.")
            return
        }
        if (inv[slot]?.id == lamp.item.id && invDel(inv, lamp.item, slot = slot).success) {
            statAdvance(stat, lamp.xp)
        }
    }

    private companion object {
        // xpreward confirmation resumes the zero-based child of universe. Cache enum 681
        // maps child + 1 to the true stat id, independently of the visible button order.
        val choices =
            listOf(
                stats.attack,
                stats.strength,
                stats.ranged,
                stats.magic,
                stats.defence,
                stats.hitpoints,
                stats.prayer,
                stats.agility,
                stats.herblore,
                stats.thieving,
                stats.crafting,
                stats.runecrafting,
                stats.mining,
                stats.smithing,
                stats.fishing,
                stats.cooking,
                stats.firemaking,
                stats.woodcutting,
                stats.fletching,
                stats.slayer,
                stats.farming,
                stats.construction,
                stats.hunter,
            )
    }
}
