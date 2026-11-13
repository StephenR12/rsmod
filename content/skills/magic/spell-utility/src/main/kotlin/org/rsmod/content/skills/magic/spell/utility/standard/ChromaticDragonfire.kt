package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.events.NpcMeleeAlternative
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ChromaticDragons : NpcReferences() {
    val adults =
        listOf("green", "blue", "red", "black").flatMap { colour ->
            (1..5).map { variant -> find("${colour}_dragon" + if (variant == 1) "" else variant) }
        } + (1..3).map { find("black_dragon_strongholdcave_$it") } + find("lava_dragon")
}

internal object ChromaticSequences : SeqReferences() {
    val breathe = find("dragon_firebreath_attack")
}

public class ChromaticDragonfire
@Inject
constructor(
    private val resolver: StandardDragonfireDamageResolver,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in ChromaticDragons.adults) onEvent<NpcMeleeAlternative>(type.id) {
            if (random.of(6) != 0) return@onEvent
            val shield = player.worn[Wearpos.LeftHand.slot]?.id in dragonfireShieldIds
            val protected = shield || player.vars[varbits.protect_from_magic] != 0
            val resisted = !protected && !accuracy.rollMagicAccuracy(npc, player, random)
            val damage =
                resolver.rollDamage(player, DragonfireBreath.Chromatic, resisted, shield, random)
            attack =
                NpcMeleeAlternative.Attack(ChromaticSequences.breathe, HitType.Typeless, damage)
            player.mes(
                when {
                    damage == 0 -> "You are protected from the dragon fire."
                    shield -> "Your shield absorbs most of the dragon fire."
                    protected || resisted -> "You manage to resist some of the dragon fire!"
                    else -> "You're horribly burnt by the dragon fire!"
                }
            )
        }
    }
}

internal val dragonfireShieldIds = setOf(1540, 11283, 11284, 21633, 21634, 22002, 22003)
