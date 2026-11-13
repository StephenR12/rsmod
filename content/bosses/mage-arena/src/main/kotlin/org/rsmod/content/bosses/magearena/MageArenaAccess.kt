package org.rsmod.content.bosses.magearena

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MageArenaTraining
import org.rsmod.api.config.refs.*
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.combat.commons.npc.engagePlayer
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.feet
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object ArenaPeople : NpcReferences() {
    val kolodion = find("arenamage1")
    val guardian = find("magearena_guardian")
    val banker = find("magearena_banker")
    val lundail = find("magearena_runeshop")
    val mages = listOf(find("saradomin_mage"), find("guthix_mage"), find("zamorak_mage"))
}

internal object ArenaScenery : LocReferences() {
    val cellar = find("magearena_lever_to_cellar")
    val surface = find("magearena_lever_from_cellar")
    val entry = find("magearena_lever_in")
    val exit = find("magearena_lever_out")
    val pool = find("magearena_waterportal1")
    val returnPool = find("magearena_waterportal2")
    val web = find("bigweb_slashable")
    val cut = find("bigweb_slashed")
}

internal object ArenaShops : InvReferences() {
    val staves = find("magearena_staffshop")
    val runes = find("magearena_runeshop")
}

internal object ArenaPacks : ObjReferences() {
    val packs =
        listOf("fire", "water", "air", "earth", "mind", "chaos").map { find("pack_${it}rune") }
}

internal object ArenaQuestBits : VarBitReferences() {
    val lundail = find("lundail_last_claimed")
    val progress = find("ma2_progress")
    val components = listOf("saradomin", "guthix", "zamorak").map { find("ma2_${it}_component") }
}

internal object ArenaSaved : VarpReferences() {
    val runes = find("magearena_rune_claim_day")
    val rotation = find("magearena_follower_rotation")
}

internal object ArenaSaveDefinitions : VarpBuilder() {
    init {
        for (name in listOf("magearena_rune_claim_day", "magearena_follower_rotation")) build(
            name
        ) {
            permanent = true
            transmitNever = true
        }
    }
}

internal val gods = listOf("Saradomin", "Guthix", "Zamorak")
internal val capes = listOf(objs.saradomin_cape, objs.guthix_cape, objs.zamorak_cape)
internal val staves = listOf(objs.saradomin_staff, objs.guthix_staff, objs.zamorak_staff)
internal val imbued =
    listOf(objs.imbued_saradomin_cape, objs.imbued_guthix_cape, objs.imbued_zamorak_cape)
internal val remains =
    listOf(MageArenaObjs.justiciarsHand, MageArenaObjs.entsRoots, MageArenaObjs.demonsHeart)
internal val counters =
    listOf(MageArenaTraining.saradomin, MageArenaTraining.guthix, MageArenaTraining.zamorak)
private val lobby = CoordGrid(2539, 4715)

internal object ArenaSupport : NpcEditor() {
    init {
        edit(ArenaPeople.banker) { contentGroup = content.banker }
        for (mage in MageArenaBosses.kolodionForms.map { it.npc } + listOf(MageArenaNpcs.zachariah, MageArenaNpcs.derwen, MageArenaNpcs.porazdir)) edit(mage) { attackRange = 8 }
        for (mage in ArenaPeople.mages) edit(mage) {
            attackRange = 8
            hitpoints = 120
            attack = 1
            strength = 1
            defence = 1
            magic = 50
            ranged = 1
            respawnRate = 150
            param[params.attackrate] = 4
            param[params.cannon_immunity] = 1
        }
    }
}

internal object ArenaInventory : InvEditor() {
    init {
        edit(ArenaShops.staves) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (staff in staves) stock += stock(staff, 5, 100)
        }
        edit(ArenaShops.runes) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (r in
                listOf(objs.fire_rune, objs.water_rune, objs.air_rune, objs.earth_rune)) stock +=
                stock(r, 200, 50)
            for (r in listOf(objs.mind_rune, objs.body_rune)) stock += stock(r, 140, 50)
            stock += stock(objs.nature_rune, 250, 30)
            stock += stock(objs.chaos_rune, 250, 10)
            stock += stock(objs.law_rune, 250, 30)
            stock += stock(objs.cosmic_rune, 20, 100)
            stock += stock(objs.death_rune, 250, 15)
            for ((i, p) in ArenaPacks.packs.withIndex()) stock +=
                stock(p, if (i < 4) 5 else if (i == 4) 4 else 3, 40)
        }
    }
}

public class MageArenaAccess
@Inject
constructor(
    private val controller: MageArenaController,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val items: ObjTypeList,
    private val shops: Shops,
    private val exposures: AiPlayerInteractions,
    private val locs: LocRepository,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
    private val bus: EventBus,
) : PluginScript() {
    override fun ScriptContext.startup() {
        spawn(ArenaPeople.kolodion, CoordGrid(2541, 4715))
        spawn(ArenaPeople.guardian, CoordGrid(2508, 4696))
        spawn(ArenaPeople.banker, CoordGrid(2534, 4714))
        spawn(ArenaPeople.lundail, CoordGrid(2533, 4717))
        val spots =
            listOf(
                3098 to 3925,
                3098 to 3942,
                3100 to 3927,
                3100 to 3940,
                3102 to 3929,
                3102 to 3938,
                3110 to 3934,
                3113 to 3934,
                3116 to 3934,
            )
        for ((i, spot) in spots.withIndex()) spawn(
            ArenaPeople.mages[i % 3],
            CoordGrid(spot.first, spot.second),
        )
        onOpLoc1(ArenaScenery.web) {
            val weapons = listOfNotNull(player.righthand) + inv.filterNotNull()
            cutWeb(it.loc, weapons.map { w -> items[w] }.maxByOrNull(::slashChance))
        }
        onOpLocU(ArenaScenery.web) { cutWeb(it.loc, it.objType) }
        onOpLoc1(ArenaScenery.cellar) { travel(CoordGrid(2539, 4712)) }
        onOpLoc1(ArenaScenery.surface) { travel(CoordGrid(3090, 3956)) }
        onOpLoc1(ArenaScenery.entry) { travel(CoordGrid(3105, 3951)) }
        onOpLoc1(ArenaScenery.exit) {
            if (travel(CoordGrid(3105, 3956))) controller.abandon(player)
        }
        onOpLoc1(ArenaScenery.pool) {
            if (vars[varps.magearena] < MageArenaTraining.KOLODION_DEFEATED)
                mes("You must defeat Kolodion before entering the pool.")
            else travel(CoordGrid(2509, 4689))
        }
        onOpLoc1(ArenaScenery.returnPool) { travel(CoordGrid(2541, 4718)) }
        onOpNpc1(ArenaPeople.kolodion) {
            if (!player.members) return@onOpNpc1
            val stage = vars[varps.magearena]
            if (stage < 6) {
                if (player.statBase(stats.magic) < 60) {
                    mes("You need level 60 Magic to challenge Kolodion.")
                    return@onOpNpc1
                }
                if (!choice2("Challenge Kolodion.", true, "Not now.", false)) return@onOpNpc1
                if (!travel(MageArenaBosses.mageArena.translate(2, 0))) return@onOpNpc1
                if (stage == 0) vars[varps.magearena] = 1
                controller.challenge(player).engagePlayer(player, exposures)
            } else if (stage < 8)
                mes(
                    "Enter the sparkling pool, pray at a statue and show the chamber guardian your cape."
                )
            else {
                mes(
                    gods.indices.joinToString(", ") { i ->
                        "${gods[i]}: ${vars[counters[i]]}/100 casts"
                    }
                )
                continueSecondArena()
            }
        }
        onEvent<NpcKillEvents.Credited> {
            if (
                npc.visType.id != MageArenaNpcs.kolodionDemon.id ||
                    controller.challenger(npc) != player
            )
                return@onEvent
            VarPlayerIntMapSetter.set(player, varps.magearena, 6)
            controller.remove(npc)
            PathingEntityCommon.teleport(player, collision, lobby)
            bus.publish(PlayerTeleportEvent.Start(player, lobby))
            player.mes("Kolodion admits defeat. Enter the sparkling pool to choose a god.")
        }
        onEvent<PlayerTeleportEvent.Start> {
            if (!MageArenaTraining.inside(destination)) controller.abandon(player)
        }
        onPlayerLogout { controller.abandon(player) }
        onEvent<PlayerDeathEvent.Completed> { controller.abandon(player) }
        onOpNpc1(ArenaPeople.guardian) {
            if (!player.members) return@onOpNpc1
            if (vars[varps.magearena] >= 8) {
                staffShop()
                return@onOpNpc1
            }
            val options = capes.indices.filter { capes[it] in inv || capes[it] in worn }
            if (vars[varps.magearena] < 7 || options.isEmpty()) {
                mes("Pray at a statue and bring me the god cape.")
                return@onOpNpc1
            }
            val i =
                if (options.size == 1) options.first()
                else
                    options.getOrNull(menu("Choose a staff", true, options.map { gods[it] }))
                        ?: return@onOpNpc1
            if (invAdd(inv, staves[i]).success) {
                vars[varps.magearena] = 8
                mes("The chamber guardian gives you your first god staff.")
            }
        }
        onOpNpc3(ArenaPeople.guardian) { staffShop() }
        for ((i, cape) in capes.withIndex()) onOpNpcU(ArenaPeople.kolodion, cape) {
            imbue(i, it.invSlot)
        }
        onOpNpc1(ArenaPeople.lundail) { runeShop() }
        onOpNpc3(ArenaPeople.lundail) { runeShop() }
        onOpNpc4(ArenaPeople.lundail) { claimRunes() }
    }

    private fun spawn(t: NpcType, c: CoordGrid) {
        if (npcs.findAll(c).none { it.id == t.id }) npcs.add(Npc(types[t], c), Int.MAX_VALUE)
    }

    private fun slashChance(t: UnpackedObjType): Int {
        if (t.id == objs.knife.id) return 50
        if (t.id in 13108..13111) return 100
        if (t.wearpos1 != Wearpos.RightHand.slot) return 0
        val bonus = t.param(params.attack_slash)
        val minimum =
            if (t.weaponCategory == org.rsmod.game.type.obj.WeaponCategory.SlashSword.id) 50 else 20
        return if (bonus > 0) bonus.coerceIn(minimum, 100) else 0
    }

    private fun ProtectedAccess.cutWeb(web: BoundLocInfo, tool: UnpackedObjType?) {
        val chance = if (player.feet?.id == 29806) 100 else tool?.let(::slashChance) ?: 0
        if (chance == 0) {
            mes("You need a knife or a slash weapon to cut the web.")
            return
        }
        if (random.of(100) >= chance) {
            mes("You fail to cut through it.")
            return
        }
        locs.change(web, ArenaScenery.cut, 100)
        mes("You slash the web apart.")
    }

    private fun ProtectedAccess.travel(dest: CoordGrid): Boolean {
        if (!player.members || player.isTeleportBlocked()) {
            mes("A magical force prevents the teleport.")
            return false
        }
        teleport(dest)
        publish(PlayerTeleportEvent.Start(player, dest))
        return true
    }

    private fun ProtectedAccess.staffShop() {
        if (player.members && vars[varps.magearena] >= 8)
            shops.open(player, "Mage Arena Staffs", ArenaShops.staves, 0.0, 100.0, 2.0)
        else mes("Claim your first god staff from the chamber guardian.")
    }

    private fun ProtectedAccess.runeShop() {
        if (player.members)
            shops.open(
                player,
                "Lundail's Arena-side Rune Shop.",
                ArenaShops.runes,
                60.0,
                100.0,
                0.1,
            )
    }

    private fun ProtectedAccess.claimRunes() {
        val reward =
            DiaryDifficulty.entries.indexOfLast {
                vars[DiaryRegion.Wilderness.tier(it).reward] != 0
            }
        if (!player.members || reward < 0) {
            mes("Complete the easy Wilderness Diary to claim runes.")
            return
        }
        val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toEpochDay().toInt()
        if (vars[ArenaSaved.runes] == today) {
            mes("You have already claimed today's runes.")
            return
        }
        val runes =
            listOf(
                objs.mind_rune,
                objs.body_rune,
                objs.cosmic_rune,
                objs.chaos_rune,
                objs.nature_rune,
                objs.law_rune,
                objs.death_rune,
            )
        if (invAdd(inv, runes[random.of(runes.size)], listOf(40, 80, 120, 200)[reward]).success) {
            vars[ArenaSaved.runes] = today
            vars[ArenaQuestBits.lundail] = 1
        }
    }

    private suspend fun ProtectedAccess.continueSecondArena() {
        if (player.statBase(stats.magic) < 75 || counters.any { vars[it] < 100 }) return
        val stage = vars[ArenaQuestBits.progress]
        if (stage < 2) {
            if (
                choice2("Track the three god followers.", true, "Not now.", false) &&
                    (MageArenaObjs.enchantedSymbol in inv ||
                        invAdd(inv, MageArenaObjs.enchantedSymbol).success)
            )
                vars[ArenaQuestBits.progress] = 2
            return
        }
        if (stage == 2) {
            for ((i, r) in remains.withIndex()) if (
                vars[ArenaQuestBits.components[i]] == 0 &&
                    r in inv &&
                    invDel(inv, r, invTotal(inv, r)).success
            )
                vars[ArenaQuestBits.components[i]] = 1
            if (ArenaQuestBits.components.all { vars[it] != 0 }) vars[ArenaQuestBits.progress] = 3
            if (
                vars[ArenaQuestBits.progress] == 2 &&
                    MageArenaObjs.enchantedSymbol !in inv &&
                    MageArenaObjs.enchantedSymbol !in bank
            )
                invAdd(inv, MageArenaObjs.enchantedSymbol)
        }
        if (
            stage >= 3 &&
                MageArenaObjs.enchantedSymbol !in inv &&
                MageArenaObjs.enchantedSymbol !in bank
        )
            invAdd(inv, MageArenaObjs.enchantedSymbol)
        if (vars[ArenaQuestBits.progress] >= 3) mes("Use a god cape on me to have it imbued.")
    }

    private fun ProtectedAccess.imbue(i: Int, slot: Int) {
        if (!player.members || vars[ArenaQuestBits.progress] < 3 || inv[slot]?.id != capes[i].id)
            return
        val first = vars[ArenaQuestBits.progress] == 3
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                if (!first) delete(bag, remains[i].id, 1)
                delete(bag, capes[i].id, 1, slot = slot)
                add(bag, imbued[i].id, 1, slot = slot)
            }
        if (result.success) {
            vars[ArenaQuestBits.progress] = 4
            mes("Kolodion imbues your cape.")
        } else mes("Bring another matching god follower's remains to imbue this cape.")
    }
}
