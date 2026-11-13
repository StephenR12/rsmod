package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.SkillDiaryAreas
import org.rsmod.api.player.diary.advanceKaramjaDiaryCounter
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.diary.interruptDiaryProductionSequence
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.GroundItemTaken
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.config.constants
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.builders.map.obj.MapObjSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GatheringObjs : ObjReferences() {
    val grass = find("snape_grass")
    val eggs = find("red_spiders_eggs")
    val seaweed = find("seaweed")
    val nightshade = find("nightshade")
    val toad = find("swamp_toad")
    val cactus = find("cactus_potato")
    val leaf = find("palm_leaf")
    val flax = find("flax")
    val banana = find("banana")
}
internal object GatheringLocs : LocReferences() {
    val palm = find("kharazi_bamboo_tree_base_leafy")
    val shaken = find("kharazi_bamboo_tree_inac")
    val flax = find("flax")
    val organ = find("kr_churchorgan")
    val bananas = listOf("full","four","three","two","one","empty").map { find("bananatree$it") }
}
internal object GatheringSeqs : SeqReferences() { val organ = find("human_use_chapel_organ") }
internal object GatheringVars : VarpReferences() { val counts = find("diary_gathering_counts") }
internal object GatheringVarBuild : VarpBuilder() {
    init { build("diary_gathering_counts") { transmitNever = true } }
}
internal object GatheringSpawns : MapObjSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<RegionalGathering>("regional-objects.toml") }
}
public class RegionalGathering @Inject constructor(private val locs: LocRepository, private val ground: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GroundItemTaken> {
            if (!player.members) return@onEvent
            val c = obj.coords
            when (obj.type) {
                GatheringObjs.seaweed.id -> if (SkillDiaryAreas.karamja(c)) player.advanceKaramjaDiaryCounter(DiaryTask.CollectKaramjaSeaweed)
                GatheringObjs.leaf.id -> if (SkillDiaryAreas.karamja(c)) player.advanceKaramjaDiaryCounter(DiaryTask.CollectPalmLeaves)
                GatheringObjs.grass.id -> if (c.level == 0 && c.x in 2495..2559 && c.z in 3712..3768)
                    player.gatherFive(DiaryTask.CollectWaterbirthGrass,0)
                GatheringObjs.eggs.id -> if (player.vars[varbits.inside_wilderness] != 0)
                    player.gatherFive(DiaryTask.CollectWildSpiderEggs,3)
                GatheringObjs.cactus.id -> if (c.x in 3456..3519 && c.z in 9472..9535 && c.level in listOf(0,2))
                    player.gatherFive(DiaryTask.GatherHiveCacti,6)
                GatheringObjs.toad.id -> if (c.level == 0 && c.x in 2405..2431 && c.z in 3507..3521)
                    player.completeDiaryTask(DiaryTask.CollectSwampToad)
                GatheringObjs.nightshade.id -> if (c.x in 2560..2607 && c.z in 9544..9591)
                    player.completeDiaryTask(DiaryTask.GatherSkavidNightshade)
            }
        }
        onOpLoc1(GatheringLocs.palm) {
            if (!player.members) return@onOpLoc1
            locs.change(it.loc,GatheringLocs.shaken,58)
            ground.add(GatheringObjs.leaf,coords,constants.lootdrop_duration, player)
        }
        onOpLoc1(GatheringLocs.flax) {
            if (!player.members || !invAdd(inv,GatheringObjs.flax).success) return@onOpLoc1
            if (random.of(16) < 3) locs.del(it.loc,10)
            if (it.loc.x in 2735..2751 && it.loc.z in 3436..3455)
                player.gatherFive(DiaryTask.PickSeersFlax,9)
        }
        for ((index,tree) in GatheringLocs.bananas.dropLast(1).withIndex()) onOpLoc1(tree) {
            if (!invAdd(inv,GatheringObjs.banana).success) return@onOpLoc1
            locs.change(it.loc,GatheringLocs.bananas[index + 1], 500)
            if (it.loc.x in 2895..2939 && it.loc.z in 3151..3182)
                player.advanceKaramjaDiaryCounter(DiaryTask.PickPlantationBananas)
        }
        onOpLoc1(GatheringLocs.organ) {
            anim(GatheringSeqs.organ)
            mes("You play the church organ.")
            player.completeDiaryTask(DiaryTask.PlaySeersOrgan)
        }
    }
    private fun Player.gatherFive(task: DiaryTask, shift: Int) {
        if (!members || hasCompletedDiaryTask(task)) return
        interruptDiaryProductionSequence(task)
        val current = vars[GatheringVars.counts]
        val count = ((current ushr shift) and 7) + 1
        val field = 7 shl shift
        VarPlayerIntMapSetter.set(this,GatheringVars.counts,(current and field.inv()) or (count shl shift))
        if (count >= 5) completeDiaryTask(task)
    }
}
