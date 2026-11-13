package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.content.skills.pets.SkillPetRates
import org.rsmod.content.skills.pets.SkillPets
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal data class AgilityCourseFinished(val player: Player, val course: AgilityLap) :
    UnboundEvent

internal data class AgilityArenaTicket(val player: Player) : UnboundEvent

internal data class SepulchreExit(val player: Player, val floor: SepulchreFloor) : UnboundEvent

public class SquirrelDrops
@Inject
constructor(
    private val rates: SkillPetRates,
    private val pets: SkillPets,
    @CoreRandom private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<AgilityCourseFinished> {
            val base =
                when (course) {
                    AgilityLap.Gnome -> 35609
                    AgilityLap.ShayzienBasic -> 31804
                    AgilityLap.ShayzienAdvanced -> 29738
                    AgilityLap.Pyramid -> 9901
                    AgilityLap.Penguin -> 9779
                    AgilityLap.Barbarian -> 44376
                    AgilityLap.Ape -> 37720
                    AgilityLap.WyrmBasic -> 28503
                    AgilityLap.WyrmAdvanced -> 25406
                    AgilityLap.Wild -> 34666
                    AgilityLap.Werewolf -> 32597
                    AgilityLap.Prifddinas -> 25146
                    AgilityLap.Dorgesh -> 10561
                    AgilityLap.Draynor -> 33005
                    AgilityLap.Kharid -> 26648
                    AgilityLap.Varrock -> 24410
                    AgilityLap.Canifis -> 36842
                    AgilityLap.Falador -> 26806
                    AgilityLap.Seers -> 35205
                    AgilityLap.Pollnivneach -> 33422
                    AgilityLap.Rellekka -> 31063
                    AgilityLap.Ardy -> 34440
                    AgilityLap.Skullball -> return@onEvent
                }
            rates.roll(player, stats.agility, 20659, base)
        }
        onEvent<AgilityArenaTicket> { rates.roll(player, stats.agility, 20659, 26404) }
        onEvent<SepulchreExit> {
            val denominator = listOf(35000, 16000, 8000, 4000, 2000)[floor.ordinal]
            val weight = if (player.statMap.getXP(stats.agility) >= 200_000_000) 15 else 1
            if (random.of(denominator) >= denominator - weight) pets.award(player, 20659)
        }
    }
}
