package org.rsmod.content.skills.hunter.moths

import org.rsmod.api.player.events.HunterCapture
import org.rsmod.content.skills.herblore.PotionConsumptionRepository
import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object ButterflyNpcs : NpcReferences() {
    val ruby = find("butterfly_ruby")
    val sapphire = find("butterfly_glacialis")
    val snowy = find("butterfly_snowy")
    val black = find("butterfly_warlock")
}
public object ButterflyObjs : ObjReferences() {
    val ruby = find("butterfly_jar_ruby")
    val sapphire = find("butterfly_jar_glacialis")
    val snowy = find("butterfly_jar_snowy")
    val black = find("butterfly_jar_warlock")
}
public enum class ButterflyEffect { Attack, Defence, Hitpoints, Strength }
public data class ButterflySpecies(val npc: NpcType, val jar: ObjType, val level: Int, val xp: Double, val effect: ButterflyEffect)
/** Exact dated requirements/rewards/effects; catch odds are explicitly a playable approximation. */
public object LegacyButterflyTable {
    val all = listOf(
        ButterflySpecies(ButterflyNpcs.ruby, ButterflyObjs.ruby, 15, 24.0, ButterflyEffect.Attack),
        ButterflySpecies(ButterflyNpcs.sapphire, ButterflyObjs.sapphire, 25, 34.0, ButterflyEffect.Defence),
        ButterflySpecies(ButterflyNpcs.snowy, ButterflyObjs.snowy, 35, 44.0, ButterflyEffect.Hitpoints),
        ButterflySpecies(ButterflyNpcs.black, ButterflyObjs.black, 45, 54.0, ButterflyEffect.Strength),
    )
    /** No pre-cutoff source gives server odds. This estimate must not be presented as parity. */
    public fun approximateChance(level: Int, species: ButterflySpecies, enhanced: Boolean): Double =
        (0.50 + (level.coerceIn(species.level, 99) - species.level) * 0.005 + if (enhanced) 0.05 else 0.0).coerceAtMost(0.95)
}

public class LegacyButterflyHunting @Inject constructor(private val npcs: NpcRepository, private val random: GameRandom, private val potions: PotionConsumptionRepository) {
    public suspend fun catch(access: ProtectedAccess, npc: Npc, species: ButterflySpecies) = with(access) {
        if (!player.members) return
        val level = player.stat(stats.hunter)
        if (level < species.level) { mes("You need a Hunter level of ${species.level} to catch this butterfly."); return }
        val net = player.righthand?.id
        val hasNet = net == MothObjs.butterflyNet.id || net == MothObjs.magicButterflyNet.id
        if (!hasNet && level < species.level + 10) { mes("You need to wield a butterfly net to catch this butterfly."); return }
        faceEntitySquare(npc)
        anim(MothSeqs.catch)
        delay(MothHunting.CATCH_TICKS)
        if (!npc.isValidTarget() || !isWithinDistance(npc, 1) || player.righthand?.id != net || player.stat(stats.hunter) < species.level + (if (hasNet) 0 else 10)) return
        if (random.randomDouble() >= LegacyButterflyTable.approximateChance(level, species, !hasNet || net == MothObjs.magicButterflyNet.id)) return
        // Like moths, an available empty jar is optional; barehanded catches release immediately.
        val jarred = hasNet && MothObjs.emptyJar in player.inv
        if (jarred) {
            val result = player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, MothObjs.emptyJar.id, 1)
                add(inventory, species.jar.id, 1)
            }
            if (!result.success) return
        } else effect(this, species.effect)
        statAdvance(stats.hunter, species.xp)
        publish(HunterCapture(player, species.npc.internalNameValue))
        if (species.effect == ButterflyEffect.Hitpoints) player.completeDiaryTask(DiaryTask.CatchSnowyKnight)
        npcs.despawn(npc, npc.type.respawnRate)
    }

    public fun release(access: ProtectedAccess, slot: Int, species: ButterflySpecies) = with(access) {
        if (!player.members || player.inv[slot]?.id != species.jar.id || !potions.isReady(player)) return
        val result = player.invTransaction(player.inv) {
            val inventory = select(player.inv)
            delete(inventory, species.jar.id, 1, slot)
            add(inventory, MothObjs.emptyJar.id, 1, slot)
        }
        if (!result.success) return
        potions.consume(player, 3)
        anim(MothSeqs.openJar)
        effect(this, species.effect)
    }
    private fun effect(access: ProtectedAccess, effect: ButterflyEffect) = with(access) {
        HunterMixEffect.fromButterfly(effect).apply(player, mixed = false)
    }
}
public class LegacyButterflyScript @Inject constructor(private val hunting: LegacyButterflyHunting) : PluginScript() {
    override fun ScriptContext.startup() {
        for (species in LegacyButterflyTable.all) {
            onOpNpc1(species.npc) { hunting.catch(this, it.npc, species) }
            onOpHeld4(species.jar) { hunting.release(this, it.slot, species) }
        }
    }
}
internal object LegacyButterflySpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<LegacyButterflyScript>("legacy-butterflies.toml") }
}
