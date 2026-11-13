package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.ranged.RangingCapeVars
import org.rsmod.api.config.refs.*
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.events.ObjTradeEvents
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CapeTrainers : NpcReferences() {
    val harlan = find("aide_tutor_melee")
    val tafani = find("duel_nurse3")
    val aaron = find("ranging_guild_armour_salesman")
}

internal object ArcheryShops : InvReferences() {
    val regular = find("ranging_guild_armourshop")
    val cape = find("ranging_guild_armourshop_skillcape")
    val trimmed = find("ranging_guild_armourshop_skillcape_trimmed")
}

internal object ArcheryShopStock : InvEditor() {
    init {
        for ((shop, cape) in
            listOf(
                ArcheryShops.regular to null,
                ArcheryShops.cape to objs.ranged_skillcape,
                ArcheryShops.trimmed to objs.ranged_skillcape_t,
            )) {
            edit(shop) {
                scope = InvScope.Shared
                stack = InvStackType.Always
                autoSize = true
                restock = true
                stock += stock(objs.leather_body, 10, 50)
                stock += stock(objs.hardleather_body, 10, 60)
                stock += stock(objs.studded_body, 10, 70)
                stock += stock(objs.leather_chaps, 20, 50)
                stock += stock(objs.studded_chaps, 15, 80)
                stock += stock(objs.leather_coif, 10, 50)
                stock += stock(objs.leather_cowl, 10, 70)
                stock += stock(objs.leather_vambraces, 10, 150)
                if (cape != null) stock += stock(cape, 1, 1)
            }
        }
    }
}

public class CombatSkillcapes
@Inject
constructor(private val shops: Shops, private val ground: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CapeTrainers.harlan) {
            SkillcapeSale.offer(this, stats.defence, objs.defence_hood)
        }
        onOpNpc1(CapeTrainers.tafani) {
            SkillcapeSale.offer(this, stats.hitpoints, objs.hitpoints_hood)
        }
        onOpNpc3(CapeTrainers.tafani) {
            if (player.stat(stats.hitpoints) < player.statBase(stats.hitpoints))
                statRestore(stats.hitpoints)
            mes("Surgeon General Tafani checks your wounds.")
        }
        onOpNpc1(CapeTrainers.aaron) { SkillcapeSale.offer(this, stats.ranged, objs.ranged_hood) }
        onOpNpc3(CapeTrainers.aaron) {
            if (!player.members) return@onOpNpc3
            val stock =
                when {
                    player.statBase(stats.ranged) < 99 -> ArcheryShops.regular
                    SkillcapeSale.isTrimmed(player) -> ArcheryShops.trimmed
                    else -> ArcheryShops.cape
                }
            shops.open(player, "Aaron's Archery Appendages.", stock, 50.0, 100.0, 1.0)
        }
        onEvent<ObjTradeEvents.Purchased> {
            if (
                (shop.id == ArcheryShops.cape.id && obj.id == objs.ranged_skillcape.id) ||
                    (shop.id == ArcheryShops.trimmed.id && obj.id == objs.ranged_skillcape_t.id)
            ) {
                player.invAddOrDrop(ground, objs.ranged_hood, count)
            }
        }
        for (cape in listOf(objs.strength_skillcape, objs.strength_skillcape_t)) {
            onOpHeld3(cape) { teleportToGuild() }
            onOpWorn3(cape) { teleportToGuild() }
        }
        for (cape in listOf(objs.ranged_skillcape, objs.ranged_skillcape_t, objs.max_cape_inv)) {
            onOpHeldU(cape, AvaItems.head) {
                if (!player.members) return@onOpHeldU
                if (vars[RangingCapeVars.head] != 0) {
                    mes("Your capes already have the improved ammunition recovery effect.")
                } else if (cape in inv && player.invDel(inv, AvaItems.head, 1).success) {
                    vars[RangingCapeVars.head] = 1
                    mes(
                        "Vorkath's head permanently improves the ammunition recovery of your ranging and max capes."
                    )
                }
            }
        }
    }

    public suspend fun teleportToWarriorsGuild(access: ProtectedAccess): Unit = with(access) { teleportToGuild() }

    private suspend fun ProtectedAccess.teleportToGuild() {
        val wildBase = if (coords.z >= 9920) 9920 else 3520
        if (
            !player.members ||
                player.isTeleportBlocked() ||
                (vars[varbits.inside_wilderness] != 0 && (coords.z - wildBase) / 8 + 1 > 20)
        ) {
            mes("A magical force prevents the teleport.")
            return
        }
        stopAction()
        anim(seqs.teleport_casting)
        spotanim(spotanims.teleport_casting)
        soundSynth(synths.teleport_all)
        delay(4)
        val destination = CoordGrid(2865, 3546)
        teleport(destination)
        publish(PlayerTeleportEvent.Start(player, destination))
    }
}
