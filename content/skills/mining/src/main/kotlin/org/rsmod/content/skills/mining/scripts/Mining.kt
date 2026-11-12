package org.rsmod.content.skills.mining.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.config.locParam
import org.rsmod.api.config.locXpParam
import org.rsmod.api.config.objParam
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.back
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.front
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.other.coalbag.CoalBagStorage
import org.rsmod.content.other.gembag.GemBagStorage
import org.rsmod.content.skills.mining.CrystalPickaxeBankPreference
import org.rsmod.content.skills.mining.GemRockRewards
import org.rsmod.content.skills.mining.InfernalPickaxeLifecycle
import org.rsmod.content.skills.mining.MiningGemPreTable
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.configs.MiningParams
import org.rsmod.content.skills.mining.configs.MiningVarbits
import org.rsmod.events.UnboundEvent
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Mining
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val locRepo: LocRepository,
    private val playerRepo: PlayerRepository,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
    private val infernalPickaxe: InfernalPickaxeLifecycle,
    private val crystalPickaxeBankPreference: CrystalPickaxeBankPreference,
    private val coalBag: CoalBagStorage,
    private val gemBag: GemBagStorage,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(MiningObjs.dragonPickaxe, objs.smouldering_stone) {
            infernalPickaxe.create(this, it.firstSlot, it.secondSlot)
        }
        for (cosmetic in infernalPickaxe.cosmeticDragonPickaxes) {
            onOpHeldU(cosmetic, objs.smouldering_stone) {
                infernalPickaxe.rejectCosmeticConversion(this, it.firstSlot, it.secondSlot)
            }
        }
        for (ornament in infernalPickaxe.ornamentPairs) {
            onOpHeldU(ornament.plain, objs.trailblazer_tool_ornament_kit) {
                infernalPickaxe.applyOrnament(this, it.firstSlot, it.secondSlot, ornament)
            }
        }
        for (style in infernalPickaxe.ornamentStyles) {
            for (ornamented in listOf(style.trailblazer, style.reloaded)) {
                onOpHeld1(ornamented) {
                    infernalPickaxe.wieldOrnamented(this, it.inventory, it.slot)
                }
                onOpHeld3(ornamented) {
                    infernalPickaxe.revertOrnament(this, it.slot, ornamented, style.plain)
                }
                onOpHeld4(ornamented) {
                    infernalPickaxe.changeStyle(this, it.inventory, it.slot, style)
                }
                onOpWorn3(ornamented) {
                    infernalPickaxe.changeStyle(this, player.worn, it.slot, style)
                }
            }
        }
        onProtectedEvent<BankedObjEvents.ConfigureCharges>(MiningObjs.crystalPickaxe.id) {
            crystalPickaxeBankPreference.configureCharges(this)
        }
        onProtectedEvent<BankedObjEvents.ConfigureCharges>(MiningObjs.crystalPickaxeInactive.id) {
            crystalPickaxeBankPreference.configureCharges(this)
        }
        onOpLoc1(content.ore) { startMining(it.loc, it.type) }
        onOpLoc3(content.ore) { mine(it.loc, it.type) }
        onOpLocU(content.ore, content.mining_pickaxe) { startMining(it.loc, it.type) }
        onOpLoc1(MiningLocs.denseRunestone) { startDenseRunestoneMining(it.loc) }
        onOpLoc3(MiningLocs.denseRunestone) { mineDenseRunestone(it.loc) }
        onOpLoc1(MiningLocs.depletedRunestone) { mes("The runestone has been mined to depletion.") }
    }

    private fun ProtectedAccess.startDenseRunestoneMining(rock: BoundLocInfo) {
        val pickaxe = canMineDenseRunestone() ?: return
        anim(objTypes[pickaxe].pickaxeAnim)
        actionDelay = mapClock + DENSE_RUNESTONE_TICKS
        skillAnimDelay = mapClock + 4
        opLoc3(rock)
    }

    private fun ProtectedAccess.mineDenseRunestone(rock: BoundLocInfo) {
        val pickaxe = canMineDenseRunestone(reportFullInventory = false)
        if (pickaxe == null) {
            resetAnim()
            return
        }
        if (skillAnimDelay <= mapClock) {
            anim(objTypes[pickaxe].pickaxeAnim)
            skillAnimDelay = mapClock + 4
        }
        if (actionDelay > mapClock) {
            opLoc3(rock)
            return
        }

        val diaryDouble =
            player.vars[varbits.kourend_diary_medium_complete] != 0 && random.of(20) == 0
        val count = if (diaryDouble && player.inv.freeSpace() >= 2) 2 else 1
        val result = invAdd(inv, MiningObjs.denseEssenceBlock, count)
        check(result.success) { "Dense-runestone reward failed after capacity validation: $result" }
        statAdvance(stats.mining, DENSE_MINING_XP * xpMods.get(player, stats.mining))
        statAdvance(stats.crafting, DENSE_CRAFTING_XP * xpMods.get(player, stats.crafting))
        mes("You chip some dense essence from the runestone.")

        val persists = statRandom(stats.mining, DENSE_PERSIST_LOW, DENSE_PERSIST_HIGH, 0)
        if (!persists) {
            depleteDenseRunestone(rock)
            resetAnim()
            return
        }

        actionDelay = mapClock + DENSE_RUNESTONE_TICKS
        opLoc3(rock)
    }

    private fun ProtectedAccess.canMineDenseRunestone(
        reportFullInventory: Boolean = true
    ): InvObj? {
        if (!player.members) {
            mes("You need to be on a members' world to mine this runestone.")
            return null
        }
        if (player.miningLvl < DENSE_LEVEL) {
            mes("You need a Mining level of $DENSE_LEVEL to mine this runestone.")
            return null
        }
        if (player.craftingLvl < DENSE_LEVEL) {
            mes("You need a Crafting level of $DENSE_LEVEL to mine this runestone.")
            return null
        }
        if (MiningObjs.chisel !in player.inv) {
            mes("You need a chisel to mine this runestone.")
            return null
        }
        if (player.inv.isFull()) {
            if (reportFullInventory) {
                mes("Your inventory is too full to hold any more dense essence blocks.")
            }
            return null
        }
        val pickaxe = findCarriedPickaxe(player, objTypes)
        if (pickaxe == null) {
            noPickaxeMessage()
            return null
        }
        return pickaxe
    }

    private fun ProtectedAccess.depleteDenseRunestone(rock: BoundLocInfo) {
        when (rock.id) {
            MiningLocs.denseRunestoneOne.id -> {
                vars[MiningVarbits.denseRunestoneOne] = 1
                vars[MiningVarbits.denseRunestoneTwo] = 0
            }
            MiningLocs.denseRunestoneTwo.id -> {
                vars[MiningVarbits.denseRunestoneOne] = 0
                vars[MiningVarbits.denseRunestoneTwo] = 1
            }
            else -> error("Unexpected dense-runestone base loc: ${rock.id}")
        }
    }

    private fun ProtectedAccess.startMining(rock: BoundLocInfo, type: UnpackedLocType) {
        if (!canMine(type)) {
            return
        }
        val pickaxe = findPickaxe(player, objTypes) ?: return noPickaxeMessage()
        anim(objTypes[pickaxe].pickaxeAnim)
        spam("You swing your pick at the rock.")
        actionDelay = mapClock + nextRollDelay(objTypes[pickaxe])
        skillAnimDelay = mapClock + 4
        opLoc3(rock)
    }

    private fun ProtectedAccess.mine(rock: BoundLocInfo, type: UnpackedLocType) {
        if (!canMine(type)) {
            resetAnim()
            return
        }
        val pickaxe = findPickaxe(player, objTypes)
        if (pickaxe == null) {
            resetAnim()
            noPickaxeMessage()
            return
        }

        if (skillAnimDelay <= mapClock) {
            anim(objTypes[pickaxe].pickaxeAnim)
            skillAnimDelay = mapClock + 4
        }

        if (actionDelay <= mapClock) {
            if (rollOrdinaryRockGem(type)) {
                actionDelay = mapClock + nextRollDelay(objTypes[pickaxe])
                opLoc3(rock)
                return
            }
            val bounds = successBounds(player, type)
            val success = statRandom(stats.mining, bounds.first, bounds.last, invisibleLvls)
            actionDelay = mapClock + nextRollDelay(objTypes[pickaxe])
            if (success) {
                val gemReward =
                    if (type.gemRock) GemRockRewards.at(random.of(GemRockRewards.totalWeight))
                    else null
                val product = gemReward?.obj ?: type.product
                val xp = type.xp * xpMods.get(player, stats.mining)
                if (gemReward != null) {
                    mes("You just mined ${gemReward.article} ${gemReward.displayName}!")
                    soundSynth(synths.found_gem)
                } else {
                    mes("You manage to mine some ${objTypes[product].name.lowercase()}.")
                }
                statAdvance(stats.mining, xp)
                val storedInCoalBag = coalBag.storeMinedCoal(player, product)
                if (storedInCoalBag == 0 && gemBag.storeAcquired(player, product) > 0) {
                    mes(GemBagStorage.AUTO_STORE_MESSAGE)
                } else if (storedInCoalBag == 0) {
                    invAdd(inv, product)
                }
                if (hasMiningCape() && product.isMiningCapeOre() && random.of(20) == 0) {
                    val bonusStored = coalBag.storeMinedCoal(player, product)
                    if (bonusStored == 0) {
                        invAdd(inv, product)
                    }
                    mes("Your cape allows you to mine an additional ore.")
                }
                publish(MinedOre(player, rock, product))

                locRepo.change(rock, type.depleted, type.respawnTime)
                resetAnim()
                sendLocalOverlayLoc(rock, type, type.respawnTime)
                return
            }
        }
        opLoc3(rock)
    }

    private fun ProtectedAccess.rollOrdinaryRockGem(type: UnpackedLocType): Boolean {
        if (
            type.gemRock ||
                random.of(gemPreRollDenominator(player)) >= MiningGemPreTable.TOTAL_WEIGHT
        ) {
            return false
        }
        val reward = MiningGemPreTable.at(random.of(MiningGemPreTable.TOTAL_WEIGHT))
        val product = reward.obj
        if (product != null) {
            mes("You just found ${reward.article} ${reward.displayName}!")
            soundSynth(synths.found_gem)
            if (gemBag.storeAcquired(player, product) > 0) {
                mes(GemBagStorage.AUTO_STORE_MESSAGE)
            } else {
                invAdd(inv, product)
            }
        }
        return true
    }

    private fun ProtectedAccess.hasMiningCape(): Boolean =
        player.back?.id == objs.mining_skillcape.id ||
            player.back?.id == objs.mining_skillcape_t.id ||
            player.back?.id == objs.max_cape_worn.id

    private fun ObjType.isMiningCapeOre(): Boolean =
        id == objs.copper_ore.id ||
            id == objs.tin_ore.id ||
            id == objs.iron_ore.id ||
            id == objs.coal.id ||
            id == objs.silver_ore.id ||
            id == objs.gold_ore.id ||
            id == objs.mithril_ore.id ||
            id == objs.adamantite_ore.id

    private fun ProtectedAccess.canMine(type: UnpackedLocType): Boolean {
        if (player.miningLvl < type.level) {
            mes("You need a Mining level of ${type.level} to mine this rock.")
            return false
        }
        val coalCanRedirect = !type.gemRock && coalBag.canStoreMinedCoal(player, type.product)
        if (inv.isFull() && !coalCanRedirect) {
            if (type.gemRock) {
                mes("Your inventory is too full to hold any more gems.")
            } else {
                val product = objTypes[type.product]
                mes("Your inventory is too full to hold any more ${product.name.lowercase()}.")
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.noPickaxeMessage() {
        mes("You need a pickaxe to mine this rock.")
        mes("You do not have a pickaxe which you have the Mining level to use.")
    }

    private fun ProtectedAccess.nextRollDelay(pickaxe: UnpackedObjType): Int {
        val fastChance = pickaxe.pickaxeFastIntervalChance
        return if (fastChance > 0 && random.randomBoolean(fastChance)) {
            pickaxe.pickaxeInterval - 1
        } else {
            pickaxe.pickaxeInterval
        }
    }

    private fun sendLocalOverlayLoc(rock: BoundLocInfo, type: UnpackedLocType, respawn: Int) {
        val players = playerRepo.findAll(ZoneKey.from(rock.coords), zoneRadius = 3)
        for (player in players) {
            ClientScripts.addOverlayTimerLoc(
                player = player,
                coords = rock.coords,
                loc = type,
                shape = rock.shape,
                timer = Constants.overlay_timer_mining,
                ticks = respawn,
                colour = 16765184,
            )
        }
    }

    data class MinedOre(val player: Player, val rock: BoundLocInfo, val product: ObjType) :
        UnboundEvent

    companion object {
        val UnpackedObjType.pickaxeLevel: Int by objParam(params.levelrequire)
        val UnpackedObjType.pickaxeAnim: SeqType by objParam(params.skill_anim)
        val UnpackedObjType.pickaxeInterval: Int by objParam(MiningParams.pickaxeInterval)
        val UnpackedObjType.pickaxeFastIntervalChance: Int by
            objParam(MiningParams.pickaxeFastIntervalChance)
        val UnpackedObjType.pickaxeSoteRequired: Boolean by
            objParam(MiningParams.pickaxeSoteRequired)

        val UnpackedLocType.level: Int by locParam(params.levelrequire)
        val UnpackedLocType.product: ObjType by locParam(params.skill_productitem)
        val UnpackedLocType.gemRock: Boolean by locParam(MiningParams.gemRock)
        val UnpackedLocType.xp: Double by locXpParam(params.skill_xp)
        val UnpackedLocType.depleted: LocType by locParam(params.next_loc_stage)
        val UnpackedLocType.respawnTime: Int by locParam(params.respawn_time)
        val UnpackedLocType.successLow: Int by locParam(MiningParams.successLow)
        val UnpackedLocType.successHigh: Int by locParam(MiningParams.successHigh)

        private val chargedGloryIds =
            setOf(
                objs.amulet_of_glory_1.id,
                objs.amulet_of_glory_2.id,
                objs.amulet_of_glory_3.id,
                objs.amulet_of_glory_4.id,
                objs.amulet_of_glory_5.id,
                objs.amulet_of_glory_6.id,
                objs.amulet_of_glory_t_1.id,
                objs.amulet_of_glory_t_2.id,
                objs.amulet_of_glory_t_3.id,
                objs.amulet_of_glory_t_4.id,
                objs.amulet_of_glory_t_5.id,
                objs.amulet_of_glory_t_6.id,
                objs.amulet_of_eternal_glory.id,
            )

        public fun successBounds(player: Player, rock: UnpackedLocType): IntRange {
            val multiplier = if (rock.gemRock && player.hasChargedGlory()) 3 else 1
            return rock.successLow * multiplier..rock.successHigh * multiplier
        }

        public fun gemPreRollDenominator(player: Player): Int =
            if (player.hasChargedGlory()) {
                MiningGemPreTable.CHARGED_GLORY_DENOMINATOR
            } else {
                MiningGemPreTable.NORMAL_DENOMINATOR
            }

        private fun Player.hasChargedGlory(): Boolean = front?.id in chargedGloryIds

        fun findPickaxe(player: Player, objTypes: ObjTypeList): InvObj? {
            val worn = player.righthand?.takeIf { objTypes[it].isUsablePickaxe(player) }
            val carried =
                player.inv
                    .filterNotNull { objTypes[it].isUsablePickaxe(player) }
                    .minWithOrNull { first, second ->
                        objTypes[first].compareMiningSpeed(objTypes[second])
                    }
            if (worn != null && carried != null) {
                return if (objTypes[worn].compareMiningSpeed(objTypes[carried]) <= 0) {
                    worn
                } else {
                    carried
                }
            }
            return worn ?: carried
        }

        fun findCarriedPickaxe(player: Player, objTypes: ObjTypeList): InvObj? =
            player.inv
                .filterNotNull { objTypes[it].isUsablePickaxe(player) }
                .minWithOrNull { first, second ->
                    objTypes[first].compareMiningSpeed(objTypes[second])
                }

        private fun UnpackedObjType.isUsablePickaxe(player: Player): Boolean =
            isContentType(content.mining_pickaxe) &&
                player.miningLvl >= pickaxeLevel &&
                (!members || player.members) &&
                (!pickaxeSoteRequired ||
                    player.vars[varbits.song_of_the_elves] >= SONG_OF_THE_ELVES_COMPLETE)

        private fun UnpackedObjType.compareMiningSpeed(other: UnpackedObjType): Int {
            val firstDenominator = pickaxeFastIntervalChance.coerceAtLeast(1)
            val secondDenominator = other.pickaxeFastIntervalChance.coerceAtLeast(1)
            val firstNumerator =
                pickaxeInterval * firstDenominator - if (pickaxeFastIntervalChance > 0) 1 else 0
            val secondNumerator =
                other.pickaxeInterval * secondDenominator -
                    if (other.pickaxeFastIntervalChance > 0) 1 else 0
            return (firstNumerator * secondDenominator).compareTo(
                secondNumerator * firstDenominator
            )
        }

        private const val SONG_OF_THE_ELVES_COMPLETE: Int = 194
        const val DENSE_LEVEL: Int = 38
        const val DENSE_RUNESTONE_TICKS: Int = 9
        const val DENSE_PERSIST_LOW: Int = 210
        const val DENSE_PERSIST_HIGH: Int = 230
        const val DENSE_MINING_XP: Double = 12.0
        const val DENSE_CRAFTING_XP: Double = 8.0
    }
}
