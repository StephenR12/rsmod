package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object EssenceMineNpcs : NpcReferences() {
    val sedridor = find("head_wizard_2op")
    val aubury = find("aubury_3op")
    val cromperty = listOf(find("cromperty_pre_diary"), find("cromperty_post_diary"))
    val brimstail = find("gnome_brimstail_2op")
    val distentor = find("guild_wizard_2op")
}

internal object EssenceMineLocs : LocReferences() {
    val portal = find("blankrunestone_exit_portal")
    val towerDown = find("wizards_tower_laddertop")
    val towerUp = find("wizards_tower_ladder")
}

internal object EssenceMineBits : VarBitReferences() {
    val portal = find("essencemine_portal")
}

internal object EssenceMineVars : VarpReferences() {
    val wizard = find("essence_mine_return_wizard")
}

internal object EssenceMineVarpBuilder : VarpBuilder() {
    init {
        build("essence_mine_return_wizard") { transmitNever = true }
    }
}

internal object EssenceWizardSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<EssenceMineTravel>("essence-wizards.toml")
    }
}

internal enum class EssenceWizard(
    val returnTile: CoordGrid,
    val diary: DiaryTask? = null,
    val members: Boolean = false,
) {
    Sedridor(CoordGrid(3104, 9571), DiaryTask.SedridorEssenceMine),
    Aubury(CoordGrid(3253, 3402), DiaryTask.AuburyEssenceMine),
    Cromperty(CoordGrid(2683, 3325), DiaryTask.CrompertyEssenceMine, true),
    Brimstail(CoordGrid(2408, 9817), DiaryTask.BrimstailEssenceMine, true),
    Distentor(CoordGrid(2592, 3088), members = true),
}

public class EssenceMineTravel
@Inject
constructor(
    private val regions: RegionRepository,
    private val registry: RegionRegistry,
    private val players: PlayerRegistry,
) : PluginScript() {
    private val template =
        RegionTemplate.create {
            for (x in 0 until 8) for (z in 0 until 8) this[x, z, 0] = ZoneKey(360 + x, 600 + z, 0)
        }
    private var current: Region? = null
    private var arrivals = 0

    override fun ScriptContext.startup() {
        onOpNpc3(EssenceMineNpcs.sedridor) { enter(it.npc, EssenceWizard.Sedridor) }
        onOpNpc4(EssenceMineNpcs.aubury) { enter(it.npc, EssenceWizard.Aubury) }
        for (npc in EssenceMineNpcs.cromperty) onOpNpc3(npc) {
            enter(it.npc, EssenceWizard.Cromperty)
        }
        onOpNpc3(EssenceMineNpcs.brimstail) { enter(it.npc, EssenceWizard.Brimstail) }
        onOpNpc3(EssenceMineNpcs.distentor) { enter(it.npc, EssenceWizard.Distentor) }
        onOpLoc1(EssenceMineLocs.portal) { leave() }
        onPlayerLogout {
            val wizard = EssenceWizard.entries.getOrNull(player.vars[EssenceMineVars.wizard] - 1)
            if (wizard != null) {
                val zone = registry[ZoneKey.from(player.coords)]
                if (zone.normalX in 360..367 && zone.normalZ in 600..607) {
                    players.relocateOnLogout(player, wizard.returnTile)
                }
                VarPlayerIntMapSetter.set(player, EssenceMineVars.wizard, 0)
            }
        }
        onOpLoc1(EssenceMineLocs.towerDown) { teleport(CoordGrid(3104, 9576)) }
        onOpLoc1(EssenceMineLocs.towerUp) { teleport(CoordGrid(3105, 3162)) }
    }

    private fun ProtectedAccess.enter(npc: Npc, wizard: EssenceWizard) {
        // TODO(quests): Rune Mysteries supplies the existing completion varp.
        if (vars[varps.runemysteries] < 6 || wizard.members && !player.members) return
        var mine = current
        if (arrivals >= 5 || mine == null || mine.slot == Region.INVALID_SLOT) {
            mine = regions.add(template)
            if (mine == null) {
                mes("The mine is currently full.")
                return
            }
            current = mine
            arrivals = 0
        }
        val landing = mine.normal[CoordGrid(2911, 4832)]
        mine.reclaimFromCycle = mapClock + 3
        arrivals++
        vars[EssenceMineVars.wizard] = wizard.ordinal + 1
        vars[EssenceMineBits.portal] = listOf(0, 1, 3, 4, 7)[random.of(5)]
        npc.say("Senventior disthine molenko!")
        teleport(landing)
        wizard.diary?.let(player::completeDiaryTask)
    }

    private fun ProtectedAccess.leave() {
        val wizard = EssenceWizard.entries.getOrNull(vars[EssenceMineVars.wizard] - 1) ?: return
        teleport(wizard.returnTile)
        vars[EssenceMineVars.wizard] = 0
    }
}
