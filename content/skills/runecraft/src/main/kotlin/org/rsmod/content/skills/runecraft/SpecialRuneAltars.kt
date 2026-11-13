package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.skills.pets.RiftGuardian
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class SpecialRuneAltars
@Inject
constructor(private val guardian: RiftGuardian, private val modifiers: RunecraftOutputModifiers) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(RunecraftLocs.ouraniaAltar) { craftOurania(this) }
        for (shrine in RunecraftLocs.ralosShrines) {
            onOpLocU(shrine, objs.fire_rune) {
                if (inv[it.invSlot]?.id == objs.fire_rune.id) craftSunfire(this)
            }
        }
    }

    internal fun craftOurania(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            val pure = invTotal(inv, RunecraftObjs.pureEssence)
            val daeyalt = invTotal(inv, RunecraftObjs.daeyaltEssence)
            if (pure + daeyalt == 0) {
                mes("You need pure or daeyalt essence to craft runes here.")
                return
            }
            val level = stat(stats.runecrafting)
            val diary = vars[varbits.ardougne_diary_medium_complete] != 0
            val base = IntArray(OuraniaRuneTable.runes.size)
            val extra = IntArray(base.size)
            var xp = 0.0
            repeat(pure + daeyalt) { i ->
                val index = OuraniaRuneTable.roll(level, random.of(10_000))
                val rune = OuraniaRuneTable.runes[index]
                base[index]++
                if (diary && random.of(1_000) < rune.diaryChancePerThousand) extra[index]++
                xp += rune.xp * 1.7 * if (i < pure) 1.0 else 1.5
            }
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    if (pure > 0) delete(bag, RunecraftObjs.pureEssence.id, pure)
                    if (daeyalt > 0) delete(bag, RunecraftObjs.daeyaltEssence.id, daeyalt)
                    for ((index, rune) in OuraniaRuneTable.runes.withIndex()) {
                        if (base[index] == 0) continue
                        val total =
                            base[index] +
                                extra[index] +
                                modifiers.raimentsBonus(access, base[index])
                        add(bag, rune.obj.id, total)
                    }
                }
            if (result.success) {
                guardian.craft(player, "ourania", pure + daeyalt)
                finish(xp, "You bind the essence into an assortment of runes.")
            }
        }

    internal fun craftSunfire(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            val level = stat(stats.runecrafting)
            if (level < 33) {
                mes("You need a Runecraft level of 33 to craft sunfire runes.")
                return
            }
            val available =
                minOf(invTotal(inv, objs.fire_rune), invTotal(inv, RunecraftObjs.sunfireSplinters))
            val pure = minOf(available, invTotal(inv, RunecraftObjs.pureEssence))
            val daeyalt = minOf(available - pure, invTotal(inv, RunecraftObjs.daeyaltEssence))
            val used = pure + daeyalt
            if (used == 0) {
                mes("You need fire runes, sunfire splinters and pure or daeyalt essence.")
                return
            }
            val extracts =
                modifiers.applicableExtractCount(this, used, RunecraftObjs.twistedExtract)
            val multiplier =
                when {
                    level >= 98 -> 3
                    level >= 49 -> 2
                    else -> 1
                }
            val base = used * multiplier
            val total = base + modifiers.raimentsBonus(this, base) + 60 * extracts
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, objs.fire_rune.id, used)
                    delete(bag, RunecraftObjs.sunfireSplinters.id, used)
                    if (pure > 0) delete(bag, RunecraftObjs.pureEssence.id, pure)
                    if (daeyalt > 0) delete(bag, RunecraftObjs.daeyaltEssence.id, daeyalt)
                    if (extracts > 0) delete(bag, RunecraftObjs.twistedExtract.id, extracts)
                    add(bag, RunecraftObjs.sunfireRune.id, total)
                }
            if (result.success) {
                guardian.craft(player, "fire", used)
                finish(9.0 * pure + 13.5 * daeyalt, "You bind the essence into sunfire runes.")
            }
        }

    private fun ProtectedAccess.finish(xp: Double, message: String) {
        anim(RunecraftSeqs.craft)
        spotanim(RunecraftSpotanims.craft)
        statAdvance(stats.runecrafting, xp)
        mes(message)
    }
}
