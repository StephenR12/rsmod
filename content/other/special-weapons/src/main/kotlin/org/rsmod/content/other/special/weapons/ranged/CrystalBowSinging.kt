package org.rsmod.content.other.special.weapons.ranged

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.woodcutting.CrystalSingingRecipe
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.type.obj.ObjTypeList

class BasicBowSinging @Inject constructor(private val singing: ElvenBowSinging) :
    CrystalSingingRecipe {
    override val item
        get() = objs.crystal_bow

    override suspend fun sing(access: ProtectedAccess, assisted: Boolean) =
        singing.create(access, assisted, false)
}

class EnhancedBowSinging @Inject constructor(private val singing: ElvenBowSinging) :
    CrystalSingingRecipe {
    override val item
        get() = objs.bow_of_faerdhinen

    override suspend fun sing(access: ProtectedAccess, assisted: Boolean) =
        singing.create(access, assisted, true)
}

class CorruptedBowSinging @Inject constructor(private val singing: ElvenBowSinging) :
    CrystalSingingRecipe {
    override val item
        get() = objs.bow_of_faerdhinen_c_hefin

    override fun available(player: Player, assisted: Boolean): Boolean =
        player.inv.any { it.isAnyType(objs.bow_of_faerdhinen, CrystalBowItems.faerdhinenInactive) }

    override suspend fun sing(access: ProtectedAccess, assisted: Boolean) =
        singing.corrupt(access, assisted)
}

class ElvenBowSinging @Inject constructor(private val types: ObjTypeList) {
    suspend fun create(access: ProtectedAccess, assisted: Boolean, enhanced: Boolean) =
        with(access) {
            val level = if (enhanced) 82 else 78
            val shards = if (enhanced) 100 else 40
            val price = if (assisted) shards + shards / 2 else shards
            val seed = if (enhanced) CrystalBowItems.enhancedSeed else CrystalBowItems.seed
            val weapon = if (enhanced) objs.bow_of_faerdhinen else objs.crystal_bow
            if (!eligible(this, assisted, level)) return
            val slot = inv.indexOfFirst { it?.id == seed.id }
            if (slot < 0 || inv.count(types[objs.crystal_shard]) < price) {
                mes("You need a ${types[seed].name.lowercase()} and $price crystal shards.")
                return
            }
            val input = inv[slot]
            if (!assisted) anim(seqs.prif_crystal_singing)
            delay(3)
            if (!assisted) resetAnim()
            if (inv[slot] != input || !eligible(this, assisted, level)) return
            val result =
                player.invTransaction(inv) {
                    val stock = select(inv)
                    delete(stock, seed.id, 1, slot)
                    delete(stock, objs.crystal_shard.id, price)
                    add(stock, weapon.id, 1, vars = if (enhanced) 10000 else 2500)
                }
            if (result.success && !assisted) {
                val xp = if (enhanced) 5000.0 else 2000.0
                statAdvance(stats.smithing, xp)
                statAdvance(stats.crafting, xp)
            }
        }

    suspend fun corrupt(access: ProtectedAccess, assisted: Boolean) =
        with(access) {
            if (!eligible(this, assisted, 82)) return
            val slot =
                inv.indexOfFirst {
                    it.isAnyType(objs.bow_of_faerdhinen, CrystalBowItems.faerdhinenInactive)
                }
            val bow = if (slot >= 0) inv[slot] else null
            if (bow == null) {
                mes("You need an uncorrupted bow of Faerdhinen.")
                return
            }
            val cost =
                (2000 - CrystalBowItems.paidShards(bow)).coerceAtLeast(0) +
                    (if (assisted) 1000 else 0)
            if (inv.count(types[objs.crystal_shard]) < cost) {
                mes("You need $cost crystal shards to corrupt this bow.")
                return
            }
            if (
                !choice2(
                    "Corrupt the bow.",
                    true,
                    "Cancel.",
                    false,
                    title = "Corrupt the bow for $cost crystal shards?",
                )
            )
                return
            if (inv[slot] != bow || !eligible(this, assisted, 82)) return
            val result =
                player.invTransaction(inv) {
                    val stock = select(inv)
                    delete(stock, bow.id, 1, slot)
                    if (cost > 0) delete(stock, objs.crystal_shard.id, cost)
                    add(stock, objs.bow_of_faerdhinen_c_hefin.id, 1)
                }
            if (result.success) mes("Your bow is now corrupted.")
        }

    private fun eligible(access: ProtectedAccess, assisted: Boolean, level: Int): Boolean =
        with(access) {
            if (!player.members || player.vars[varbits.song_of_the_elves] < 194) {
                mes("You need to complete Song of the Elves to sing crystal equipment.")
                return false
            }
            if (!assisted && (stat(stats.smithing) < level || stat(stats.crafting) < level)) {
                mes("You need level $level Smithing and Crafting to sing this bow.")
                return false
            }
            true
        }
}
