package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TeomatLocs : LocReferences() {
    val altar = find("varlamore_prayer_activity_altar")
    val bowl = find("varlamore_libation_bowl")
    val shrines = listOf(find("ralos_shrine"), find("ralos_shrine_taur01"))
}

internal object TeomatBits : VarBitReferences() {
    val wine = find("varlamore_prayer_winetype")
    val charges = find("varlamore_prayer_winequant")
}

internal object TeomatQueues : QueueReferences() {
    val offer = find("teomat_shard_offering")
}

internal object TeomatSeqs : SeqReferences() {
    val offer = find("human_pray_blessed_bone_shards_01")
}

internal data class TeomatBone(val raw: ObjType?, val blessed: ObjType, val shards: Int)

internal object TeomatItems : ObjReferences() {
    val shards = find("blessed_bone_shard")
    val splinters = find("sunfiresplinter")
    val sunfireWine = find("jug_sunfire_wine")
    val blessedWine = find("jug_wine_blessed")
    val blessedSunfireWine = find("jug_sunfire_wine_blessed")
    val bones =
        listOf(
            TeomatBone(objs.alans_bones, find("blessed_alan_bones"), 5),
            TeomatBone(objs.bones, find("blessed_bones"), 4),
            TeomatBone(objs.bat_bones, find("blessed_bat_bones"), 5),
            TeomatBone(objs.big_bones, find("blessed_big_bones"), 12),
            TeomatBone(objs.zogre_bones, find("blessed_zogre_bones"), 18),
            TeomatBone(objs.wyrmling_bones, find("blessed_babywyrm_bones"), 21),
            TeomatBone(objs.babydragon_bones, find("blessed_babydragon_bones"), 24),
            TeomatBone(objs.wyrm_bones, find("blessed_wyrm_bones"), 42),
            TeomatBone(null, find("sun_kissed_bone"), 45),
            TeomatBone(objs.wyvern_bones, find("blessed_wyvern_bones"), 58),
            TeomatBone(objs.dragon_bones, find("blessed_dragon_bones"), 58),
            TeomatBone(objs.drake_bones, find("blessed_drake_bones"), 67),
            TeomatBone(objs.fayrg_bones, find("blessed_fayrg_bones"), 67),
            TeomatBone(objs.lava_dragon_bones, find("blessed_lava_dragon_bones"), 68),
            TeomatBone(objs.raurg_bones, find("blessed_raurg_bones"), 77),
            TeomatBone(objs.hydra_bones, find("blessed_hydra_bones"), 93),
            TeomatBone(objs.dagannoth_bones, find("blessed_dagannoth_bones"), 100),
            TeomatBone(objs.ourg_bones, find("blessed_ourg_bones"), 115),
            TeomatBone(objs.superior_dragon_bones, find("blessed_dragon_bones_superior"), 121),
            TeomatBone(null, find("varlamore_bone_statuette01"), 125),
            TeomatBone(null, find("varlamore_bone_statuette02"), 125),
            TeomatBone(null, find("varlamore_bone_statuette03"), 125),
        )
}

public class TeomatPrayer
@Inject
constructor(private val locs: LocRepository, private val restoration: PrayerAltarRestoration) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val blessings =
            TeomatItems.bones.mapNotNull { bone -> bone.raw?.let { it to bone.blessed } }.toMap() +
                mapOf(
                    objs.jug_of_wine to TeomatItems.blessedWine,
                    TeomatItems.sunfireWine to TeomatItems.blessedSunfireWine,
                )
        onOpLoc1(TeomatLocs.altar) { bless(blessings) }
        for ((input, output) in blessings) onOpLocU(TeomatLocs.altar, input) {
            if (inv[it.invSlot]?.id == input.id) bless(mapOf(input to output))
        }
        for (bone in TeomatItems.bones) {
            onOpHeld1(bone.blessed) { chisel(this, it.slot, bone) }
            onOpHeldU(bone.blessed, objs.chisel) { chisel(this, it.firstSlot, bone) }
        }
        onOpHeldU(objs.jug_of_wine, TeomatItems.splinters) {
            if (
                !player.members ||
                    inv[it.firstSlot]?.id != objs.jug_of_wine.id ||
                    objs.pestle_and_mortar !in inv
            )
                return@onOpHeldU
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, TeomatItems.splinters.id, 2)
                delete(bag, objs.jug_of_wine.id, 1, slot = it.firstSlot)
                add(bag, TeomatItems.sunfireWine.id, 1, slot = it.firstSlot)
            }
        }
        onOpLoc1(TeomatLocs.bowl) { offer(it.loc) }
        onOpLoc2(TeomatLocs.bowl) {
            mes("There are ${vars[TeomatBits.charges]} charges remaining in the bowl.")
        }
        onPlayerQueueWithArgs<BoundLocInfo>(TeomatQueues.offer) {
            if (
                isWithinDistance(it.args, 1) &&
                    locs.findAll(it.args.coords).any { loc -> loc.entity == it.args.entity }
            )
                offer(it.args)
        }
        for (shrine in TeomatLocs.shrines) onOpLoc1(shrine) {
            if (player.members) restoration.restore(this, it.loc)
        }
    }

    private fun ProtectedAccess.bless(recipes: Map<ObjType, ObjType>) {
        if (!canPray()) return
        val byId = recipes.mapKeys { it.key.id }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                for (slot in inv.indices) {
                    val input = inv[slot] ?: continue
                    val output = byId[input.id] ?: continue
                    delete(bag, input.id, 1, slot = slot)
                    add(bag, output.id, 1, slot = slot)
                }
            }
        if (result.success && inv.any { obj -> recipes.values.any { it.id == obj?.id } })
            mes("Ralos blesses your offerings.")
    }

    internal fun chisel(access: ProtectedAccess, slot: Int, bone: TeomatBone): Unit =
        with(access) {
            if (!player.members || inv[slot]?.id != bone.blessed.id) return
            if (objs.chisel !in inv) {
                mes("You need a chisel to break this down.")
                return
            }
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, bone.blessed.id, 1, slot = slot)
                    add(bag, TeomatItems.shards.id, bone.shards)
                }
            if (result.success) statAdvance(stats.crafting, 5.0)
        }

    private fun ProtectedAccess.canPray(): Boolean {
        if (!player.members) return false
        if (statBase(stats.prayer) >= 30) return true
        mes("You need a Prayer level of 30 to do that.")
        return false
    }

    private fun ProtectedAccess.offer(bowl: BoundLocInfo) {
        if (!canPray() || actionDelay > mapClock) return
        clearWeakQueue(TeomatQueues.offer)
        if (vars[TeomatBits.charges] == 0) {
            val slot =
                inv.indexOfFirst {
                    it?.id == TeomatItems.blessedWine.id ||
                        it?.id == TeomatItems.blessedSunfireWine.id
                }
            if (slot < 0) {
                mes("You need a jug of blessed wine to fill the libation bowl.")
                return
            }
            val wine = requireNotNull(inv[slot]).id
            val poured =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, wine, 1, slot = slot)
                    add(bag, objs.jug_empty.id, 1, slot = slot)
                }
            if (!poured.success) return
            vars[TeomatBits.wine] = if (wine == TeomatItems.blessedSunfireWine.id) 2 else 1
            vars[TeomatBits.charges] = 400
        }
        if (stat(stats.prayer) < 2) {
            mes("You need at least 2 Prayer points to sacrifice bone shards.")
            return
        }
        val count = minOf(100, vars[TeomatBits.charges], invTotal(inv, TeomatItems.shards))
        if (count == 0) {
            mes("You need blessed bone shards to make an offering.")
            return
        }
        val consumed = (0 until count).count { !zealotPreservesOffering() }
        if (consumed > 0 && !invDel(inv, TeomatItems.shards, count = consumed).success) return
        val xp = if (vars[TeomatBits.wine] == 2) 6.0 else 5.0
        vars[TeomatBits.charges] -= count
        if (vars[TeomatBits.charges] == 0) vars[TeomatBits.wine] = 0
        statSub(stats.prayer, 2, 0)
        statAdvance(stats.prayer, count * xp)
        faceLoc(bowl)
        anim(TeomatSeqs.offer)
        actionDelay = mapClock + 1
        if (TeomatItems.shards in inv && stat(stats.prayer) >= 2)
            weakQueue(TeomatQueues.offer, 4, bowl)
    }
}
