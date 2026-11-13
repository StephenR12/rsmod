package org.rsmod.content.skills.mining.motherlode

import jakarta.inject.Inject
import org.rsmod.content.skills.pets.SkillPetRates
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.hat
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.other.gembag.GemBagStorage
import org.rsmod.content.skills.mining.MiningGemPreTable
import org.rsmod.content.skills.mining.ProspectorOutfits
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeFastIntervalChance
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeInterval
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MotherlodeMine
@Inject
constructor(private val petRates: SkillPetRates,
    private val storage: MotherlodeStorage,
    private val world: MotherlodeWorld,
    private val locs: LocRepository,
    private val types: ObjTypeList,
    private val xp: XpModifiers,
    private val gems: GemBagStorage,
    private val percy: MotherlodePercy,
    private val crystalPickaxe: org.rsmod.content.skills.mining.CrystalPickaxeLifecycle,
    private val celestial: org.rsmod.content.skills.mining.CelestialMiningRing,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (vein in MlmLocs.veins) onOpLoc1(vein) { mine(this, it.loc) }
        for (rock in MlmLocs.rockfalls) onOpLoc1(rock) { rockfall(this, it.loc) }
        onOpLoc1(MlmLocs.broken) { repair(this, it.loc) }
        onOpLocU(MlmLocs.broken, objs.hammer) { repair(this, it.loc) }
        onOpLoc1(MlmLocs.hopper) {
            if (it.loc.coords == CoordGrid(3755, 5677) && vars[MlmVars.unlocks] and 2 == 0)
                mes("Unlock the Super Hopper from Prospector Percy first.")
            else storage.deposit(this)
        }
        onOpLocU(MlmLocs.hopper, MlmObjs.dirt) {
            if (it.loc.coords != CoordGrid(3755, 5677) || vars[MlmVars.unlocks] and 2 != 0)
                storage.deposit(this)
        }
        for (sack in MlmLocs.sacks) onOpLoc1(sack) { storage.collect(this) }
        onPlayerSoftTimer(MlmTimers.processing) { storage.process(player, world.flowing()) }
        onPlayerLogin { storage.login(player) }
        onOpNpc1(MlmNpcs.percy) { percy.talk(this) }
        onOpNpc3(MlmNpcs.percy) { percy.shop(this) }
        onOpLoc1(MlmLocs.entry) {
            if (player.members) {
                telejump(CoordGrid(3728, 5691))
                player.softTimer(MlmTimers.processing, 1)
                storage.refresh(player)
            }
        }
        onOpLoc1(MlmLocs.exit) {
            telejump(CoordGrid(3060, 9764))
            storage.refresh(player)
        }
        onOpLoc1(MlmLocs.guildEntry) {
            if (player.members) {
                telejump(CoordGrid(3718, 5678))
                player.softTimer(MlmTimers.processing, 1)
                storage.refresh(player)
            }
        }
        onOpLoc1(MlmLocs.guildExit) {
            if (!player.members) return@onOpLoc1
            if (player.stat(stats.mining) < 60) {
                mes("You need a Mining level of 60 to enter the Mining Guild.")
                return@onOpLoc1
            }
            telejump(CoordGrid(3055, 9744))
            storage.refresh(player)
            if (player.hat?.id in listOf(ProspectorOutfits.hat.id, ProspectorOutfits.goldenHat.id))
                player.completeDiaryTask(DiaryTask.EnterMiningGuildProspector)
        }
        for (ladder in MlmLocs.ladders) onOpLoc1(ladder) {
            if (
                !player.members ||
                    vars[MlmVars.unlocks] and 1 == 0 ||
                    player.statBase(stats.mining) < 57
            )
                mes("You need 57 unboosted Mining and Percy's permission to use the upper level.")
            else telejump(CoordGrid(3755, 5674))
        }
        for (top in MlmLocs.tops) onOpLoc1(top) { telejump(CoordGrid(3755, 5673)) }
        onOpLoc1(MlmLocs.crate) { supply(this, it.loc) }
        onOpLoc1(MlmLocs.bank) { ifOpenMainSidePair(interfaces.bank_main, interfaces.bank_side) }
        onOpLoc1(MlmLocs.shortcut) {
            val c = it.loc.coords
            val medium = vars[DiaryRegion.Falador.tier(DiaryDifficulty.Medium).complete] != 0
            if (player.stat(stats.agility) < 54 || c.z > 5660 && !medium) {
                mes("You need 54 Agility and, for the northern tunnel, the medium Falador Diary.")
                return@onOpLoc1
            }
            telejump(
                when {
                    c.z < 5660 -> if (c.z < 5644) CoordGrid(3745, 5646) else CoordGrid(3744, 5641)
                    c.x < 3762 -> CoordGrid(3765, 5671)
                    else -> CoordGrid(3759, 5670)
                }
            )
        }
    }

    public suspend fun mine(access: ProtectedAccess, vein: BoundLocInfo): Unit =
        with(access) {
            val upper = vein.coords in MlmArea.upper
            fun valid(): Boolean =
                player.members &&
                    player.statBase(stats.mining) >= 30 &&
                    (!upper ||
                        (player.statBase(stats.mining) >= 57 &&
                            vars[MlmVars.unlocks] and 1 != 0)) &&
                    storage.count(player) < storage.capacity(player) &&
                    isWithinDistance(vein, 1) &&
                    locs.findExact(vein.coords, typesLoc(vein)) != null
            if (!valid()) {
                mes(
                    "You need the required Mining level, access and room in your sack to mine here."
                )
                return
            }
            while (valid()) {
                val pick =
                    Mining.findPickaxe(player, types)
                        ?: run {
                            mes("You need a pickaxe you can use.")
                            resetAnim()
                            return
                        }
                if (inv.isFull()) {
                    mes("You don't have enough inventory space.")
                    resetAnim()
                    return
                }
                faceLoc(vein)
                anim(types[pick].pickaxeAnim)
                val tool = types[pick]
                val fast = tool.pickaxeFastIntervalChance
                gatheringDelay(
                    tool.pickaxeInterval - (if (fast > 0 && random.randomBoolean(fast)) 1 else 0)
                )
                if (!valid() || Mining.findPickaxe(player, types) != pick) {
                    resetAnim()
                    return
                }
                // Reuse the ordinary gem pre-table; its exact MLM-specific placement remains
                // unverified.
                if (
                    random.of(Mining.gemPreRollDenominator(player)) < MiningGemPreTable.TOTAL_WEIGHT
                ) {
                    val gem = MiningGemPreTable.at(random.of(MiningGemPreTable.TOTAL_WEIGHT)).obj
                    if (gem != null && gems.storeAcquired(player, gem) == 0) invAdd(inv, gem)
                    continue
                }
                if (!statRandom(stats.mining, 60, 105, celestial.invisibleBoost(player))) continue
                val firstFree = inv.indexOfFirst { it == null }
                val result = MlmRewards.choose(this)
                val tx =
                    player.invTransaction(inv) {
                        add(select(inv), MlmObjs.dirt.id, 1, vars = result)
                    }
                if (!tx.success) {
                    resetAnim()
                    return
                }
                petRates.roll(player, stats.mining, 13321, 247_200, firstFree)
                crystalPickaxe.consumeCharge(this, pick)
                statAdvance(stats.mining, 60.0 * xp.get(player, stats.mining))
                world.onPayDirt(vein) { random.of(if (upper) 60..67 else 39..45) }
            }
            resetAnim()
        }

    private fun typesLoc(loc: BoundLocInfo) = MlmLocs.veins.first { it.id == loc.id }

    public suspend fun repair(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            fun hammer(): Boolean =
                objs.hammer in inv ||
                    objs.imcando_hammer in inv ||
                    objs.imcando_hammer_offhand in inv ||
                    objs.imcando_hammer in player.worn ||
                    objs.imcando_hammer_offhand in player.worn
            if (!player.members || loc.coords !in MlmArea.struts || !hammer()) {
                mes("You need a hammer to repair the strut.")
                return false
            }
            while (isWithinDistance(loc, 1) && locs.findExact(loc.coords, MlmLocs.broken) != null) {
                if (!hammer()) return false
                faceLoc(loc)
                gatheringDelay(1)
                if (
                    !hammer() ||
                        !isWithinDistance(loc, 1) ||
                        locs.findExact(loc.coords, MlmLocs.broken) == null
                )
                    return false
                val smith = player.stat(stats.smithing)
                // Mod Ash's dated12%..27% endpoints; linear interpolation is the explicit repair
                // model.
                val chance = 0.12 + 0.15 * (smith - 1).coerceIn(0, 98) / 98.0
                if (random.randomDouble() >= chance) continue
                if (!world.repair(loc, random.of(2..4))) return false
                statAdvance(stats.smithing, 1.5 * smith * xp.get(player, stats.smithing))
                player.completeDiaryTask(DiaryTask.RepairMotherlodeStrut)
                return true
            }
            false
        }

    public suspend fun rockfall(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            if (!player.members || !MlmArea.contains(loc.coords) || !isWithinDistance(loc, 1))
                return false
            val pick = Mining.findPickaxe(player, types) ?: return false
            faceLoc(loc)
            anim(types[pick].pickaxeAnim)
            gatheringDelay(4) // Rockfall-specific timing/respawn cadence are an explicit model.
            if (
                !isWithinDistance(loc, 1) ||
                    Mining.findPickaxe(player, types) == null ||
                    locs.findExact(loc.coords, MlmLocs.rockfalls.first { it.id == loc.id }) == null
            )
                return false
            val removed = locs.del(loc, 30)
            if (removed) statAdvance(stats.mining, 10.0 * xp.get(player, stats.mining))
            resetAnim()
            removed
        }

    public fun supply(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            val index = CRATES.indexOf(loc.coords)
            if (!player.members || index < 0) return false
            // Three crates carry each tool; the precise allocation among the six native crates is
            // unverified.
            invAdd(inv, if (index % 2 == 0) objs.hammer else MiningObjs.bronzePickaxe).success
        }

    public companion object {
        public val CRATES: List<CoordGrid> =
            listOf(
                CoordGrid(3752, 5664),
                CoordGrid(3752, 5674),
                CoordGrid(3753, 5659),
                CoordGrid(3755, 5665),
                CoordGrid(3756, 5668),
                CoordGrid(3758, 5671),
            )
    }
}
