package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcClueDropRolls
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropRoll
import org.rsmod.api.death.NpcDropRollResult
import org.rsmod.api.death.NpcDropRolls
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcWeightedDrop
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AbyssCreatures : NpcReferences() {
    val all = listOf(find("abyssal_leech"), find("abyssal_pyramid"), find("abyssal_walker"))
}

internal object AbyssDrops : ObjReferences() {
    val notedEssence = find("cert_blankrune_high")
}

/** The three ordinary abyssal creatures share this table in both accessible rings. */
public class AbyssCreatureDrops
@Inject
constructor(
    private val registry: NpcDropTableRegistry,
    private val ground: ObjRegistry,
    private val clues: NpcClueDropRolls,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val weights = mutableListOf<NpcWeightedDrop>()
        fun add(weight: Int, type: ObjType, count: Int = 1) {
            weights += NpcWeightedDrop(weight, NpcDropRolls.item(type, count..count))
        }
        for (temple in
            listOf(
                RuneTemple.Air,
                RuneTemple.Water,
                RuneTemple.Earth,
                RuneTemple.Fire,
                RuneTemple.Mind,
                RuneTemple.Body,
            )) add(5, TempleObjects.talismans.getValue(temple))
        for (temple in listOf(RuneTemple.Cosmic, RuneTemple.Chaos, RuneTemple.Nature)) add(
            2,
            TempleObjects.talismans.getValue(temple),
        )
        add(1, TempleObjects.elementalTalisman)
        add(36, AbyssDrops.notedEssence)
        add(34, RunecraftObjs.pureEssence)
        for (count in listOf(5, 10, 15)) add(4, AbyssDrops.notedEssence, count)
        add(3, objs.binding_necklace)
        weights += NpcWeightedDrop(256 - weights.sumOf { it.weight }, NpcDropRolls.nothing())
        val table =
            NpcDropTable(
                NpcDropRolls.sequence(
                    NpcDropRolls.item(objs.ashes),
                    NpcDropRolls.weighted(weights),
                    NpcDropRolls.independent(denominator = 42, roll = NpcDropRoll(::pouch)),
                    clues.independent(
                        ClueTier.Medium,
                        objs.chicken_clue_scroll_medium,
                        denominator = 256,
                    ),
                )
            )
        for (creature in AbyssCreatures.all) registry.register(creature, table)
    }

    private fun pouch(context: NpcDropContext): NpcDropRollResult {
        val player = context.hero
        val inventory = player.inv
        val bank = player.invMap[invs.bank]
        val floor = player.observerUUID?.let(ground.objs::typesForReceiver).orEmpty()
        fun owns(pouch: EssencePouchDefinition) =
            pouch.variants.any { it in inventory || bank?.contains(it) == true || it.id in floor }
        if (owns(EssencePouchDefinitions.colossal)) return NpcDropRollResult.Miss
        val next =
            EssencePouchDefinitions.all.firstOrNull {
                it != EssencePouchDefinitions.colossal && !owns(it)
            } ?: return NpcDropRollResult.Miss
        return NpcDropRolls.item(next.normal).roll(context)
    }
}
