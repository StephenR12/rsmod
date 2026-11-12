package org.rsmod.content.other.combatachievements

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.max
import org.rsmod.api.player.events.CombatAchievementEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varbit.UnpackedVarBitType
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.UnpackedVarpType
import org.rsmod.game.type.varp.VarpTypeList

@Singleton
public class CombatAchievementService
@Inject
constructor(
    public val catalogue: CombatAchievementCatalogue,
    varps: VarpTypeList,
    varbits: VarBitTypeList,
) {
    private val completionVarps: List<UnpackedVarpType>
    private val pointsVarbit: UnpackedVarBitType = varbits.named("ca_points")
    private val tierStatus: Map<CombatAchievementTier, UnpackedVarBitType> =
        CombatAchievementTier.entries.associateWith {
            varbits.named("ca_tier_status_${it.cacheName}")
        }
    private val tierCompleted: Map<CombatAchievementTier, UnpackedVarBitType> =
        CombatAchievementTier.entries.associateWith {
            varbits.named("ca_total_tasks_completed_${it.cacheName}")
        }

    init {
        val slots =
            max(1, (catalogue.tasks.maxOfOrNull(CombatAchievement::id) ?: 0) / BITS_PER_VARP + 1)
        completionVarps = (0 until slots).map { varps.named("ca_task_completed_$it") }
    }

    public fun complete(player: Player, taskId: Int, announce: Boolean = true): Boolean {
        val task = catalogue.byId[taskId] ?: return false
        if (isCompleted(player, task)) return false
        val varp = completionVarps[task.id / BITS_PER_VARP]
        val mask = 1 shl (task.id % BITS_PER_VARP)
        VarPlayerIntMapSetter.set(player, varp, player.vars[varp] or mask)
        refresh(player)
        if (announce) {
            player.mes(
                "Congratulations, you've completed a ${task.tier.cacheName} combat task: " +
                    "<col=ff981f>${task.name}</col>."
            )
        }
        return true
    }

    public fun isCompleted(player: Player, task: CombatAchievement): Boolean {
        val varp = completionVarps.getOrNull(task.id / BITS_PER_VARP) ?: return false
        val mask = 1 shl (task.id % BITS_PER_VARP)
        return player.vars[varp] and mask != 0
    }

    public fun progress(player: Player): CombatAchievementProgress {
        val completed = catalogue.tasks.filter { isCompleted(player, it) }
        return CombatAchievementProgress(
            completed.size,
            catalogue.tasks.size,
            completed.sumOf { it.tier.points },
            CombatAchievementTier.entries.associateWith { tier ->
                completed.count { it.tier == tier }
            },
        )
    }

    /** Rebuilds all client counters and reward tier states from the persistent completion bits. */
    public fun refresh(player: Player): CombatAchievementProgress {
        val progress = progress(player)
        VarPlayerIntMapSetter.set(player, pointsVarbit, progress.points)
        for (tier in CombatAchievementTier.entries) {
            VarPlayerIntMapSetter.set(
                player,
                tierCompleted.getValue(tier),
                progress.byTier.getValue(tier),
            )
            val status =
                if (progress.points >= catalogue.threshold(tier)) COMPLETE_STATUS
                else INCOMPLETE_STATUS
            VarPlayerIntMapSetter.set(player, tierStatus.getValue(tier), status)
        }
        return progress
    }

    public fun process(event: CombatAchievementEvents.EncounterCompleted): Int {
        val candidates = catalogue.tasks.filter { task -> matches(event, task) }
        return candidates.count { complete(event.player, it.id) }
    }

    private fun matches(
        event: CombatAchievementEvents.EncounterCompleted,
        task: CombatAchievement,
    ): Boolean {
        val text = "${task.name} ${task.description}".lowercase()
        val aliases = encounterAliases(event.encounter)
        if (aliases.none(text::contains)) return false

        val requiredCount = completionCount(task.description)
        if (event.completionCount < requiredCount) return false

        val mode = event.mode?.lowercase()
        if ("entry mode" in text && mode?.contains("entry") != true) return false
        if ("hard mode" in text && mode?.contains("hard") != true) return false
        if ("challenge mode" in text && mode?.contains("challenge") != true) return false
        if ("expert mode" in text && mode?.contains("expert") != true) return false
        if (("without dying" in text || "without anyone dying" in text) && event.deaths != 0)
            return false

        val requiredRaidLevel =
            RAID_LEVEL.find(task.description)?.groupValues?.get(1)?.toIntOrNull()
        if (requiredRaidLevel != null && (event.raidLevel ?: 0) < requiredRaidLevel) return false

        val timeLimit = parseTimeLimitTicks(task.description)
        val durationTicks = event.durationTicks
        if (timeLimit != null && (durationTicks == null || durationTicks > timeLimit)) return false

        // Mechanics, equipment, prayer, team-size, and damage restrictions must be asserted by
        // the implementing encounter via TaskSatisfied; never award them from prose heuristics.
        if (RESTRICTED.any(text::contains)) return false
        return COMPLETION_WORDS.any(text::contains)
    }

    private fun completionCount(description: String): Int {
        if (description.contains("once", ignoreCase = true)) return 1
        return COUNT.find(description)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 1
    }

    private fun parseTimeLimitTicks(description: String): Int? {
        val match = TIME.find(description) ?: return null
        val minutes = match.groupValues[1].toIntOrNull() ?: 0
        val seconds = match.groupValues[2].toIntOrNull() ?: 0
        return ((minutes * 60 + seconds) * TICKS_PER_SECOND_NUMERATOR + 2) /
            TICKS_PER_SECOND_DENOMINATOR
    }

    private fun encounterAliases(encounter: String): Set<String> {
        val normalized = encounter.lowercase()
        return when (normalized) {
            "fight caves" -> setOf("fight cave", "tz-tok jad", "tztok-jad", "jad")
            "inferno" -> setOf("inferno", "tzkal-zuk", "zuk")
            "chambers of xeric" -> setOf("chambers of xeric", "xeric", "great olm", "olm")
            "theatre of blood" -> setOf("theatre of blood", "theater of blood", "verzik")
            "tombs of amascut" -> setOf("tombs of amascut", "amascut", "wardens")
            "fortis colosseum" -> setOf("fortis colosseum", "colosseum", "sol heredit")
            else -> setOf(normalized)
        }
    }

    private fun VarpTypeList.named(name: String): UnpackedVarpType =
        values.firstOrNull { it.internalName == name }
            ?: error("Combat Achievements requires varp '$name' in this cache revision.")

    private fun VarBitTypeList.named(name: String): UnpackedVarBitType =
        values.firstOrNull { it.internalName == name }
            ?: error("Combat Achievements requires varbit '$name' in this cache revision.")

    private companion object {
        private const val BITS_PER_VARP = 32
        private const val TICKS_PER_SECOND_NUMERATOR = 5
        private const val TICKS_PER_SECOND_DENOMINATOR = 3
        private const val INCOMPLETE_STATUS = 1
        private const val COMPLETE_STATUS = 2
        private val COUNT =
            Regex("(?:complete|kill|defeat).*?([0-9][0-9,]*)\\s+times?", RegexOption.IGNORE_CASE)
        private val TIME =
            Regex(
                "(?:within|under|less than)\\s+(?:(\\d+)\\s+minutes?)?(?:\\s*(\\d+)\\s+seconds?)?",
                RegexOption.IGNORE_CASE,
            )
        private val RAID_LEVEL = Regex("raid level(?: of)?\\s+([0-9]+)", RegexOption.IGNORE_CASE)
        private val COMPLETION_WORDS = listOf("complete", "kill", "defeat", "subdue")
        private val RESTRICTED =
            listOf(
                "without taking damage",
                "without taking any damage",
                "without using",
                "without equipping",
                "without losing",
                "without activating",
                "without anyone taking",
                "whilst",
                "while wearing",
                "only using",
                "special attack",
                "prayer",
                "team of",
                "or fewer players",
            )
    }
}

public data class CombatAchievementProgress(
    public val completed: Int,
    public val total: Int,
    public val points: Int,
    public val byTier: Map<CombatAchievementTier, Int>,
)
