package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.inWildernessGodWars
import org.rsmod.api.combat.commons.npc.slayerFamily
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryKillSequence
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.diary.recordDiaryKill
import org.rsmod.api.player.hat
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CombatDiaryKills @Inject constructor(private val objTypes: ObjTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcKillEvents.Credited> {
            if (!player.members) return@onEvent
            recordSequence(player, npc)
            task(player, npc, hit)?.let(player::completeDiaryTask)
        }
    }

    private fun recordSequence(player: Player, npc: Npc) {
        val c = npc.coords
        val id = npc.visType.id
        when (id) {
            100,
            102 -> player.recordDiaryKill(DiaryKillSequence.RockCrabs)
            in 2265..2267 -> player.recordDiaryKill(DiaryKillSequence.Kings, id - 2265)
            2205 -> player.recordDiaryKill(DiaryKillSequence.Generals, 0)
            2215 -> player.recordDiaryKill(DiaryKillSequence.Generals, 1)
            3129 -> player.recordDiaryKill(DiaryKillSequence.Generals, 2)
            3162 -> player.recordDiaryKill(DiaryKillSequence.Generals, 3)
            1365,
            1366,
            1369,
            1370 ->
                if (c.within(2688..2743, 9864..9918)) {
                    val element =
                        when (id) {
                            1365 -> 0
                            1366 -> 1
                            1369 -> 2
                            else -> 3
                        }
                    player.recordDiaryKill(DiaryKillSequence.Elementals, element)
                }
            6618 -> player.recordDiaryKill(DiaryKillSequence.WildernessHard, 0)
            6619 -> player.recordDiaryKill(DiaryKillSequence.WildernessHard, 1)
            6615 -> player.recordDiaryKill(DiaryKillSequence.WildernessHard, 2)
            6609,
            11992 -> player.recordDiaryKill(DiaryKillSequence.WildernessElite, 0)
            6610,
            11998 -> player.recordDiaryKill(DiaryKillSequence.WildernessElite, 1)
            6611,
            6612,
            11993,
            11994 -> player.recordDiaryKill(DiaryKillSequence.WildernessElite, 2)
        }
    }

    private fun task(player: Player, npc: Npc, hit: Hit?): DiaryTask? {
        val weapon = hit?.righthandType(objTypes)?.id
        if (
            npc.visType.slayerFamily == 56 &&
                hit?.type == HitType.Ranged &&
                hit.secondaryObj !in setOf(2, 21728) &&
                weapon in crystalBows
        ) {
            return DiaryTask.CrystalBowElf
        }
        val c = npc.coords
        return when (npc.visType.id) {
            499 -> DiaryTask.ThermonuclearSmokeDevil
            in 8067..8069 ->
                DiaryTask.ShayzienCryptZombie.takeIf {
                    c.x in 1472..1535 && c.z in 9920..9983 && c.level in 1..3
                }
            3603 -> DiaryTask.Swordchick
            1267,
            1268 -> DiaryTask.DesertVulture
            in 458..463,
            12003,
            12004 -> DiaryTask.DesertLizard
            965 -> DiaryTask.KalphiteQueen
            423,
            7404 ->
                DiaryTask.DustDevilInSlayerHelmet.takeIf {
                    c.within(3160..3275, 9330..9410) && hasSlayerHelmet(player)
                }
            795,
            796,
            800,
            801 ->
                DiaryTask.LocustRiderWithKeris.takeIf {
                    hit?.type == HitType.Melee && weapon in keris
                }
            1838,
            1839 -> DiaryTask.FaladorParkDuck.takeIf { c.within(2984..3001, 3376..3393) }
            2592 -> DiaryTask.MudskipperMogre.takeIf { c.within(2972..3016, 3093..3129) }
            5779 -> DiaryTask.GiantMole
            in 465..468 ->
                DiaryTask.AsgarnianSkeletalWyvern.takeIf { c.within(3008..3090, 9520..9599) }
            in 265..269 ->
                DiaryTask.HeroesGuildBlueDragon.takeIf { c.within(2896..2920, 9894..9920) }
            4501 -> DiaryTask.BrineRat
            in 2075..2084 ->
                DiaryTask.BaxtorianFireGiant.takeIf { c.within(2554..2593, 9874..9910) }
            3449 -> DiaryTask.ShadowDungeonHound.takeIf { c.within(2680..2760, 5044..5120) }
            2919 -> DiaryTask.MithrilDragon
            2094 -> DiaryTask.PotholeJogre.takeIf { c.within(2816..2879, 9472..9535) }
            3125,
            3126 -> DiaryTask.FightCaveKetZek
            509 -> DiaryTask.KharaziDeathWing.takeIf { c.within(2784..2824, 9280..9323) }
            270,
            272,
            274 -> DiaryTask.BrimhavenMetalDragon.takeIf { c.within(2620..2753, 9400..9508) }
            5935,
            7206 -> DiaryTask.SandCrab
            in 6914..6919,
            8563 -> DiaryTask.Lizardman
            8565 -> DiaryTask.TempleShaman.takeIf { c.within(1279..1342, 10079..10112, 1) }
            8610,
            8611,
            10398,
            10399 -> DiaryTask.KaruulmWyrm.takeIf { c.within(1239..1290, 10132..10212) }
            7286 -> DiaryTask.Skotizo
            8609,
            10402 -> DiaryTask.KaruulmHydra.takeIf { c.within(1286..1343, 10214..10287) }
            481 -> DiaryTask.SwampCaveBug.takeIf { c.within(3136..3244, 9530..9599) }
            in 38..40,
            in 55..58 -> DiaryTask.DraynorSewerZombie.takeIf { c.within(3072..3135, 9632..9687) }
            414,
            7390 -> DiaryTask.TowerBanshee.takeIf { c.within(3400..3455, 3524..3583) }
            289 -> DiaryTask.Ghoul
            6473,
            6474 -> DiaryTask.TerrorDog
            626 -> DiaryTask.BraindeathFeverSpider
            in 1047..1051,
            7401 -> DiaryTask.CaveHorror
            415,
            7410 ->
                DiaryTask.TowerAbyssalDemon.takeIf {
                    c.within(3392..3455, 3520..3583, 2) || c.within(3392..3455, 9920..9983, 3)
                }
            in 2064..2066 ->
                DiaryTask.EnclosureTerrorbird.takeIf { c.within(2367..2390, 3421..3448) }
            in 2042..2044 -> DiaryTask.Zulrah
            6604 -> DiaryTask.WildMammoth.takeIf { c.within(2944..3391, 3520..3999) }
            2840 -> DiaryTask.EdgevilleEarthWarrior.takeIf { c.within(3110..3135, 9960..10004) }
            in 260..264,
            in 7868..7870 ->
                DiaryTask.WildGreenDragon.takeIf {
                    c.within(2944..3391, 3520..3999) || c.within(3380..3435, 10046..10142)
                }
            6608,
            7864 ->
                DiaryTask.WildAnkou.takeIf {
                    c.within(2944..3391, 3520..3999) || c.within(3264..3455, 10048..10239)
                }
            3138 -> DiaryTask.WildGodWarsBloodveld.takeIf { c.inWildernessGodWars() }
            6593,
            12607,
            12608 -> DiaryTask.LavaDragon
            2054 -> DiaryTask.ChaosElemental
            2210,
            2243,
            3159,
            3166 -> DiaryTask.WildGodWarsWarrior.takeIf { c.inWildernessGodWars() }
            2212,
            2244,
            3161,
            3168,
            11292 ->
                when {
                    c.inWildernessGodWars() -> DiaryTask.WildGodWarsMage
                    c.within(2816..2975, 5184..5375, c.level) -> DiaryTask.GodWarsMage
                    else -> null
                }
            8621,
            8622 -> DiaryTask.KaruulmHydra // Alchemical Hydra's final form inside its instance.
            else -> null
        }
    }

    private fun hasSlayerHelmet(player: Player): Boolean {
        val hat = objTypes.getOrNull(player.hat) ?: return false
        return hat.param(params.slayer_helm) != 0 || hat.param(params.slayer_helm_imbued) != 0
    }

    private fun CoordGrid.within(xs: IntRange, zs: IntRange, floor: Int = 0): Boolean =
        level == floor && x in xs && z in zs

    private companion object {
        val crystalBows =
            setOf(4212, 23983, 24123, 25865, 25867) + (4214..4223) + (25884..25896 step 2)
        val keris = (10581..10584).toSet() + setOf(25979, 25981, 27287, 27291, 30891)
    }
}
