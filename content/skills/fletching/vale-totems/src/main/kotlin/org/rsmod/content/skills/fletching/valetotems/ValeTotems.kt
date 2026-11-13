package org.rsmod.content.skills.fletching.valetotems

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ValeTotems
@Inject
constructor(
    private val items: ObjTypeList,
    private val rng: GameRandom,
    private val rewards: ValeRewards,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ValeNpcs.ranulph) { instruct() }
        onOpNpc1(ValeNpcs.isadora) { instruct() }
        onOpNpc3(ValeNpcs.ranulph) { rewards.shop(this) }
        for ((i, layers) in ValeLocs.layers.withIndex()) {
            for (loc in layers) onOpLoc1(loc) { work(i) }
            onOpLocU(layers.first()) { work(i, it.objType.id) }
            onOpLoc1(ValeLocs.offerings[i]) { claim(i) }
            onOpLoc3(ValeLocs.offerings[i]) {
                mes("The ents have left ${vars[ValeBits.sites[i].points]} offerings here.")
            }
            onOpNpcU(ValeNpcs.ents[i], items.getValue(31018)) {
                rewards.handInSeed(this, it.invSlot)
            }
        }
        onOpHeld1(items.getValue(31016)) {
            mes(
                "Match the totem to three nearby animal spirits and decorate it with four items fletched from the same wood. The ents will leave offerings when they visit."
            )
        }
    }

    private suspend fun ProtectedAccess.instruct() {
        if (!player.members) return
        val stage = vars[ValeBits.intro]
        if (stage >= 100) {
            rewards.shop(this)
            return
        }
        if (vars[ValeBits.sun] < 24) {
            mes("Complete Children of the Sun to help Ranulph.")
            return
        }
        if (player.stat(stats.fletching) < 20) {
            mes("You need level 20 Fletching to help restore the totem.")
            return
        }
        val site = ValeBits.sites.first()
        when {
            stage == 0 -> {
                if (
                    !choice2(
                        "Investigate the strange behaviour of the ents.",
                        true,
                        "Leave.",
                        false,
                    )
                )
                    return
                vars[ValeBits.intro] = 10
                changeSpirits(player, site)
                mes(
                    "Ranulph damages the old totem. Isadora asks you to rebuild it at the site to the north, using a knife and oak logs or better."
                )
            }
            stage < 20 && vars[site.base] != 0 -> {
                vars[ValeBits.intro] = 20
                mes("Now carve each of the three nearby spirit animals into your totem.")
            }
            stage < 20 -> mes("Use a knife and oak logs or better at the totem site north of here.")
            stage < 30 && vars[site.carved] != 0 -> {
                vars[ValeBits.intro] = 30
                mes(
                    "Decorate it with four bows, stocks, shields or staffs fletched from the same wood."
                )
            }
            stage < 30 -> mes("Carve the three animals you see near the totem.")
            stage < 40 && vars[site.decorations] >= 4 -> {
                vars[ValeBits.intro] = 40
                vars[site.points] = (vars[site.points] + 100).coerceAtMost(15000)
                mes("The ents approve. Collect their offerings beside the totem.")
            }
            stage < 40 -> mes("Complete the four decorations before returning.")
            stage < 50 -> mes("Claim the offerings beside the totem.")
            else -> {
                if (invAdd(inv, items.getValue(31016)).failure) {
                    mes("Make room for Ranulph's book.")
                    return
                }
                vars[ValeBits.intro] = 100
                for (s in ValeBits.sites) if (vars[s.visible] == 0) changeSpirits(player, s)
                mes(
                    "Ranulph gives you Sacraments of ent folk. You can now build totems throughout the valley."
                )
            }
        }
    }

    private suspend fun ProtectedAccess.work(i: Int, item: Int? = null) {
        val stage = vars[ValeBits.intro]
        if (!player.members || stage < 10 || stage < 100 && i != 0) {
            mes("Speak to Ranulph in north-western Auburnvale about the totem.")
            return
        }
        if (objs.knife !in inv && objs.fletching_knife !in inv && objs.fletching_knife !in worn) {
            mes("You need a knife to work on this totem.")
            return
        }
        val site = ValeBits.sites[i]
        if (vars[site.visible] == 0) changeSpirits(player, site)
        val wood = ValeWood.entries.getOrNull(vars[site.base] - 1)
        if (wood == null) {
            if (vars[site.points] >= 15000) {
                mes("Claim some offerings before building a new totem.")
                return
            }
            val logs =
                ValeWood.entries.filter { w ->
                    (item == null || w.log == item) && items.getValue(w.log) in inv
                }
            if (logs.isEmpty()) {
                mes("Bring oak logs or better to build a totem.")
                return
            }
            val selected =
                if (logs.size == 1) logs.first()
                else
                    logs.getOrNull(
                        menu("Choose the wood for the totem", true, logs.map { it.name })
                    ) ?: return
            if (player.stat(stats.fletching) < selected.level) {
                mes("You need level ${selected.level} Fletching to use this wood.")
                return
            }
            if (invDel(inv, items.getValue(selected.log)).failure) return
            vars[site.base] = selected.id
            vars[site.durability] = selected.ordinal + 2
            for (bit in site.tiers) vars[bit] = selected.id
            player.runEnergy = (player.runEnergy + 1500).coerceAtMost(10000)
            carveXp(selected)
            return
        }
        if (vars[site.carved] == 0) {
            if (stage < 20) {
                mes("Show Ranulph the base before carving it.")
                return
            }
            val tier = site.tiers.firstOrNull { vars[it] < 10 } ?: return
            val names = ValeSites.spirits
            val choice =
                choice5(
                    names[0],
                    0,
                    names[1],
                    1,
                    names[2],
                    2,
                    names[3],
                    3,
                    names[4],
                    4,
                    title = "Carve a spirit animal",
                )
            if (site.tiers.any { vars[it] == 10 + choice }) {
                mes("That animal is already carved into the totem.")
                return
            }
            vars[tier] = 10 + choice
            if (site.tiers.all { vars[it] >= 10 }) vars[site.carved] = 1
            carveXp(wood)
            return
        }
        if (stage < 30) {
            mes("Show Ranulph the carvings before decorating.")
            return
        }
        val candidates =
            inv.filterNotNull().filter {
                it.id in wood.ornaments && (item == null || it.id == item)
            }
        if (vars[site.decorations] >= 4) {
            mes("The totem is fully decorated.")
            return
        }
        if (candidates.isEmpty()) {
            mes("You need decorations fletched from ${wood.name.lowercase()} wood.")
            return
        }
        val matches = matching(player, site)
        for (obj in candidates.take(4 - vars[site.decorations])) {
            if (vars[site.carved] == 0 || invDel(inv, items.getValue(obj.id)).failure) break
            vars[site.decorations]++
            statAdvance(stats.fletching, wood.decorXp * (matches + 1) / 4.0)
            anim(seqs.human_fletching)
            delay(2)
        }
    }

    private fun ProtectedAccess.carveXp(wood: ValeWood) {
        val bonus = vars[ValeBits.buff] > 0
        statAdvance(stats.fletching, wood.xp * if (bonus) 1.15 else 1.0)
        if (bonus) vars[ValeBits.buff]--
        if (vars[varbits.poh_house_location] != 0)
            statAdvance(stats.construction, player.statBase(stats.construction).toDouble())
        anim(seqs.human_fletching)
    }

    private fun ProtectedAccess.claim(i: Int) {
        if (!player.members || vars[ValeBits.intro] < 40) return
        val bit = ValeBits.sites[i].points
        val count = vars[bit]
        if (count <= 0) return
        if (invAdd(inv, items.getValue(31054), count).failure) {
            mes("Make room for the offerings.")
            return
        }
        vars[bit] = 0
        if (i == 0 && vars[ValeBits.intro] in 40..49) vars[ValeBits.intro] = 50
        mes("You collect $count vale offerings.")
    }

    private fun matching(player: Player, site: SiteVars): Int {
        val correct = site.spirits.map { player.vars[it] }.toSet()
        return site.tiers.count { player.vars[it] - 9 in correct }
    }

    internal fun visit(player: Player, i: Int) {
        if (!player.members || player.vars[ValeBits.intro] < 100) return
        val s = ValeBits.sites[i]
        val wood = ValeWood.entries.getOrNull(player.vars[s.base] - 1)
        if (wood != null && player.vars[s.carved] != 0) {
            val match = matching(player, s)
            val offerings =
                wood.perVisit[player.vars[s.decorations].coerceIn(0, 4)] * (match + 1) / 4
            val buff = player.vars[s.trail] != 0
            set(
                player,
                s.points,
                (player.vars[s.points] + offerings * (if (buff) 2 else 1)).coerceAtMost(15000),
            )
            if (buff) set(player, s.trail, 0)
            val remaining = (player.vars[s.durability] - 1).coerceAtLeast(0)
            set(player, s.durability, remaining)
            if (remaining == 0)
                for (bit in s.tiers + listOf(s.carved, s.base, s.decorations)) set(player, bit, 0)
        }
        if (player.vars[s.visible] == 0 || rng.randomBoolean(4)) changeSpirits(player, s)
    }

    private fun changeSpirits(player: Player, site: SiteVars) {
        val available = (1..5).toMutableList()
        var mask = 0
        for (bit in site.spirits) {
            val animal = available.removeAt(rng.of(available.size))
            set(player, bit, animal)
            mask = mask or (1 shl (animal - 1))
        }
        set(player, site.visible, mask)
    }
}
