package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.entity.Player
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpTypeList

/**
 * The vars come from cache 233.4 quest_progress_get (4024). End states come from quest table
 * column 19.
 */
class SlayerQuestAccess
@Inject
constructor(rows: DbRowTypeList, varps: VarpTypeList, varbits: VarBitTypeList) {
    private val questRows = rows
    private val abyssAccess = progress(43) { it.vars[varps.getValue(492)] }
    private val muspah = progress(2338) { it.vars[varbits.getValue(14722)] }
    private val dt2 = progress(2343) { it.vars[varbits.getValue(14862)] }
    private val towerRoof = varbits.getValue(2468)
    private val kourendBoat = varbits.getValue(5619)

    private class Progress(val value: (Player) -> Int, val end: Int) {
        fun complete(player: Player): Boolean = value(player) >= end
    }

    private val waterfall = progress(158) { it.vars[varps.getValue(65)] }
    private val song = progress(137) { it.vars[varbits.getValue(9016)] }
    private val exiles = progress(55) { it.vars[varbits.getValue(9459)] }
    private val isles = progress(56) { it.vars[varbits.getValue(3311)] }
    private val sins = progress(134) { it.vars[varbits.getValue(7255)] }
    private val priestInPeril = progress(111) { it.vars[varps.getValue(302)] }
    private val fairytaleII = progress(47) { it.vars[varbits.getValue(2326)] }
    private val dragonSlayer = progress(31) { it.vars[varps.getValue(176)] }
    private val dragonSlayerII = progress(32) { it.vars[varbits.getValue(6104)] }
    private val lostCity = progress(86) { it.vars[varps.getValue(147)] }
    private val porcine = progress(110) { it.vars[varbits.getValue(10582)] }
    private val legends = progress(85) { it.vars[varps.getValue(139)] }
    private val horrorFromTheDeep = progress(74) { it.vars[varbits.getValue(34)] }
    private val desertTreasure = progress(27) { it.vars[varbits.getValue(358)] }
    private val regicide = progress(119) { it.vars[varps.getValue(328)] }
    private val mourningII = progress(100) { it.vars[varbits.getValue(1103)] }
    private val skippy = progress(135) { it.vars[varbits.getValue(1344)] }
    private val rumDeal = progress(124) { it.vars[varps.getValue(600)] }
    private val royalTrouble = progress(123) { it.vars[varbits.getValue(2140)] }
    private val elementalWorkshop = progress(38) { it.vars[varbits.getValue(13718)] }
    private val ernest = progress(44) { it.vars[varps.getValue(32)] }
    private val cabinFever = progress(12) { it.vars[varps.getValue(655)] }
    private val lunarDiplomacy = progress(88) { it.vars[varbits.getValue(2448)] }
    private val olaf = progress(106) { it.vars[varbits.getValue(3534)] }
    private val contact = progress(16) { it.vars[varbits.getValue(3274)] }
    private val hauntedMine = progress(70) { it.vars[varps.getValue(382)] }
    private val deathToDorgeshuun = progress(24) { it.vars[varbits.getValue(2258)] }
    private val deathPlateau = progress(23) { it.vars[varps.getValue(314)] }
    private val barbarianTraining = progress(3451) { it.vars[varbits.getValue(9613)] }
    private val boneVoyage = progress(11) { it.vars[varbits.getValue(5795)] }
    private val pathOfGlouphrie = progress(3425) { it.vars[varbits.getValue(15288)] }
    private val perilousMoons = progress(3514) { it.vars[varbits.getValue(9819)] }
    private val custodia = progress(5190) { it.vars[varbits.getValue(16632)] }

    private fun progress(row: Int, read: (Player) -> Int) =
        Progress(read, questRows.getValue(row).data.getValue(19).single() as Int)

    fun canReceiveBoss(player: Player, boss: Int): Boolean =
        when (boss) {
            in 1..4 -> deathPlateau.complete(player)
            8 -> dragonSlayer.complete(player)
            16 -> regicide.complete(player)
            17,
            31 -> priestInPeril.complete(player)
            21 -> abyssAccess.complete(player) || fairytaleII.value(player) > 0
            22 -> priestInPeril.complete(player) && player.vars[towerRoof] != 0
            23 -> dragonSlayerII.complete(player)
            25 -> player.vars[kourendBoat] > 0
            26 -> muspah.complete(player)
            in 27..30 -> dt2.complete(player)
            else -> true
        }

    fun canEnterArea(player: Player, area: Int): Boolean =
        when (area) {
            8 -> waterfall.complete(player)
            9 -> priestInPeril.complete(player)
            10 ->
                deathPlateau.complete(player) &&
                    maxOf(player.statBase(stats.agility), player.statBase(stats.strength)) >= 60
            14 -> horrorFromTheDeep.value(player) > 0
            20 -> desertTreasure.value(player) > 0
            25 -> isles.value(player) > 0
            27,
            29 -> dragonSlayerII.complete(player)
            28 -> mourningII.value(player) > 0
            30 -> barbarianTraining.value(player) > 0
            32 -> boneVoyage.complete(player)
            35 -> olaf.value(player) > 0
            36,
            37 -> lostCity.complete(player)
            42 -> song.complete(player)
            43 -> exiles.value(player) > 0
            44 -> sins.value(player) > 0
            45,
            46 -> priestInPeril.complete(player)
            49 -> pathOfGlouphrie.complete(player)
            50 -> perilousMoons.value(player) > 0
            52 -> custodia.complete(player)
            else -> true
        }

    fun canReceiveWild(player: Player, family: Int): Boolean =
        when (family) {
            42 -> priestInPeril.complete(player)
            48 -> priestInPeril.complete(player) && hasGodWarsAccess(player)
            49 -> desertTreasure.complete(player)
            89 -> deathPlateau.complete(player) && hasGodWarsAccess(player)
            94 -> hasGodWarsAccess(player)
            else -> true
        }

    private fun hasGodWarsAccess(player: Player): Boolean =
        maxOf(player.statBase(stats.agility), player.statBase(stats.strength)) >= 60

    fun canReceive(player: Player, family: Int): Boolean =
        when (family) {
            23,
            33,
            34,
            38,
            39,
            40,
            41,
            46,
            48,
            64,
            124 -> priestInPeril.complete(player)
            42 -> priestInPeril.complete(player) || fairytaleII.complete(player)
            24,
            25,
            26,
            27,
            58,
            59,
            60,
            127 -> dragonSlayer.value(player) > 0
            32 -> legends.complete(player)
            35 -> horrorFromTheDeep.complete(player)
            49 -> desertTreasure.value(player) > 0
            55,
            74 -> lostCity.complete(player)
            56 -> regicide.complete(player)
            66 -> mourningII.value(player) > 0
            67 -> skippy.complete(player)
            69 -> rumDeal.value(player) > 0
            71 -> royalTrouble.complete(player)
            72 -> elementalWorkshop.complete(player)
            73 -> ernest.complete(player)
            80,
            81 -> cabinFever.complete(player)
            83 -> lunarDiplomacy.value(player) > 0
            84 -> olaf.complete(player)
            85 -> contact.complete(player)
            86 -> hauntedMine.complete(player)
            87 -> deathToDorgeshuun.complete(player)
            88,
            93 -> barbarianTraining.complete(player)
            89,
            94 -> deathPlateau.complete(player)
            106 -> boneVoyage.complete(player) && elementalWorkshop.complete(player)
            108,
            109 -> dragonSlayerII.complete(player)
            121 -> porcine.complete(player)
            122 -> pathOfGlouphrie.complete(player)
            123 -> perilousMoons.complete(player)
            126 -> custodia.complete(player)
            else -> true
        }
}
