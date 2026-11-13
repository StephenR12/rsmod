package org.rsmod.content.bosses.wintertodt

import jakarta.inject.Inject
import kotlin.math.floor
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public data class WinterMaterial(
    val obj: ObjType,
    val min: Int,
    val max: Int,
    val low: Int,
    val high: Int,
)

/** Sequential independent skill-quality rolls from dated calculator14860131. */
public object WinterMaterials : ObjReferences() {
    private val rows =
        checkNotNull(javaClass.getResourceAsStream("materials.tsv"))
            .bufferedReader()
            .use { it.readLines() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.split('\t') }
    private val refs = rows.map { it[1] }.distinct().associateWith { find(it) }
    public val skills =
        listOf(
            stats.crafting,
            stats.herblore,
            stats.farming,
            stats.woodcutting,
            stats.mining,
            stats.fishing,
        )
    public val tables =
        listOf("crafting", "herblore", "farming", "woodcutting", "mining", "fishing").map { skill ->
            rows
                .filter { it[0] == skill }
                .map {
                    WinterMaterial(
                        refs.getValue(it[1]),
                        it[2].toInt(),
                        it[3].toInt(),
                        it[4].toInt(),
                        it[5].toInt(),
                    )
                }
        }

    public fun threshold(level: Int, low: Int, high: Int): Int =
        (floor(low + (high - low) * (level.coerceIn(1, 99) - 1) / 98.0).toInt() + 1).coerceIn(
            0,
            256,
        )
}

public object WinterWarmClothing : ObjReferences() {
    public val all =
        checkNotNull(javaClass.getResourceAsStream("warm-clothing.tsv"))
            .bufferedReader()
            .use { it.readLines() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .distinct()
            .map { find(it) }
}

/** Exact unique cascade and six material subtables from Reward Cart14997422. */
public class WintertodtLoot
@Inject
constructor(private val pets:org.rsmod.content.skills.pets.SkillPets, private val types: ObjTypeList, private val random: GameRandom) {
    public fun roll(player: Player, extra: Boolean = false): InvObj {
        fun owned(obj: ObjType): Int = player.invMap.values.sumOf { it.count(types[obj]) }
        if (!extra) {
            val phoenixOwned = pets.ownsItemFamily(player,20693)
            if (!phoenixOwned && random.of(5000) == 0) return InvObj(WinterObjs.phoenix)
        }
        if (random.of(10000) == 0) return InvObj(objs.dragon_axe)
        if (!extra) {
            if (random.of(1000) == 0) return InvObj(WinterObjs.tome)
            if (random.of(150) == 0)
                return InvObj(
                    if (owned(WinterObjs.gloves) >= 3) WinterObjs.magicSeed else WinterObjs.gloves
                )
            if (random.of(150) == 0)
                return if (owned(WinterObjs.torch) + owned(WinterObjs.offhand) >= 3)
                    InvObj(WinterObjs.torstolSeed, random.of(2, 3))
                else InvObj(WinterObjs.torch)
            if (random.of(150) == 0) return InvObj(WinterObjs.pyro.minBy { owned(it) })
            if (random.of(45) == 0) return InvObj(WinterObjs.pages, random.of(7, 29))
        }
        val category = random.of(25)
        if (category in 18..22) return InvObj(objs.coins, random.of(2000, 4999))
        if (category == 23) return noted(WinterObjs.dynamite, random.of(3, 5))
        if (category == 24) return noted(WinterObjs.saltpetre, random.of(3, 5))
        val tableIndex = category / 3
        val level = player.statBase(WinterMaterials.skills[tableIndex])
        val material =
            WinterMaterials.tables[tableIndex].first {
                random.of(256) < WinterMaterials.threshold(level, it.low, it.high)
            }
        val count = random.of(material.min, material.max)
        return if (WinterMaterials.skills[tableIndex] == stats.farming) InvObj(material.obj, count)
        else noted(material.obj, count)
    }

    private fun noted(obj: ObjType, count: Int): InvObj {
        val note = types[obj].certlink
        check(note > 0) { "Wintertodt material must have a certificate: $obj" }
        return InvObj(types.getValue(note), count)
    }
}
