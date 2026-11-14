package org.rsmod.content.other.commands

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invClear
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.PlayerInterfaceUpdates
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.realm.Realm
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.type.symbols.name.NameMapping
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.api.utils.system.SafeServiceExit
import org.rsmod.game.GameUpdate
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.get
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.game.type.spot.SpotanimTypeList
import org.rsmod.game.type.stat.StatType
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.game.type.stat.UnpackedStatType
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.objtx.TransactionResult
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag
import org.rsmod.routefinder.loc.LocLayerConstants
import org.simmetrics.metrics.StringMetrics

class AdminCommands
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val playerList: PlayerList,
    private val statTypes: StatTypeList,
    private val seqTypes: SeqTypeList,
    private val spotTypes: SpotanimTypeList,
    private val locTypes: LocTypeList,
    private val npcTypes: NpcTypeList,
    private val invTypes: InvTypeList,
    private val objTypes: ObjTypeList,
    private val varpTypes: VarpTypeList,
    private val varBitTypes: VarBitTypeList,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val names: NameMapping,
    private val update: GameUpdate,
    private val realm: Realm,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    private val logger = InlineLogger()

    private val levenshteinMetric = StringMetrics.levenshtein()

    override fun ScriptContext.startup() {
        onPlayerLogin {
            player.invMap[invTypes[invs.bank]]?.let { bank -> player.syncBankTabMetadata(bank) }
        }
        onCommand("master", "Max out all stats", ::master)
        onCommand("reset", "Reset all stats", ::reset)
        onCommand("setall", "Set all skills to a level", ::setAll) {
            invalidArgs = "Use as ::setall level (ex: ::setall 70)"
        }
        onCommand("setlevel", "Set one skill to a level", ::setLevel) {
            invalidArgs = "Use as ::setlevel skill level (ex: ::setlevel attack 70)"
        }
        onCommand("mypos", "Get current coordinates", ::mypos)
        onCommand("tele", "Teleport to coordinates", ::tele) {
            invalidArgs =
                "Use as ::tele x z [level] (ex: ::tele 3222 3218 0), or legacy " +
                    "::tele level mx mz lx lz"
        }
        onCommand("telezone", "Teleport to zone key", ::teleZone) {
            invalidArgs = "Use as ::telezone zoneX zoneY level (ex: 400 400 0)"
        }
        onCommand("home", "Teleport to the realm spawn", ::home)
        onCommand("unstuck", "Teleport to the realm spawn", ::home)
        onCommand("anim", "Play animation", ::anim)
        onCommand("spot", "Play spotanim", ::spotanim) {
            invalidArgs = "Use as ::spot spotanimDebugNameOrId (ex: fx_emote_party01_active)"
        }
        onCommand("locadd", "Spawn loc", ::locAdd) {
            invalidArgs = "Use as ::locadd duration locDebugNameOrId (ex: 100 bookcase)"
        }
        onCommand("locdel", "Remove loc", ::locDel) { invalidArgs = "Use as ::locdel duration" }
        onCommand("npcadd", "Spawn npc", ::npcAdd) {
            invalidArgs = "Use as ::npcadd duration npcDebugNameOrId (ex: 100 prison_pete)"
        }
        onCommand("invadd", "Spawn obj into inv", ::invAdd)
        onCommand("give", "Spawn obj into inv by name or id", ::invAdd) {
            invalidArgs = "Use as ::give objNameOrId [count] (ex: ::give twisted bow)"
        }
        onCommand("giveleagueitems", "Give Demonic Pacts relic and Echo items", ::giveLeagueItems)
        onCommand("giverunes", "Give every rune", ::giveRunes)
        onCommand("giveammo", "Give arrows, darts, and knives", ::giveAmmo)
        onCommand("bismelee", "Give a best-in-slot melee loadout", ::giveBisMelee)
        onCommand("bisrange", "Give a best-in-slot ranged loadout", ::giveBisRange)
        onCommand("bismage", "Give a best-in-slot magic loadout", ::giveBisMage)
        onCommand("giveskilling", "Give top skilling tools and outfits", ::giveSkilling)
        onCommand("invclear", "Remove all objs from inv", ::invClear)
        onCommand("xprate", "Show or set your personal XP rate", ::xpRate) {
            invalidArgs = "Use as ::xprate [rate] (ex: ::xprate 1 or ::xprate 2.5)"
        }
        onCommand("varp", "Set varp value", ::setVarp) {
            invalidArgs = "Use as ::varp debugNameOrId value (ex: option_run 1)"
        }
        onCommand("varbit", "Set varbit value", ::setVarBit) {
            invalidArgs = "Use as ::varbit debugNameOrId value (ex: emote_hotline_bling 1)"
        }
        onCommand("reboot", "Reboots the game world, applying packed changes", ::reboot)
        onCommand("slowreboot", "Reboots the game world, with a timer", ::slowReboot)
    }

    private fun master(cheat: Cheat) = with(cheat) { player.setStatLevels(level = 99) }

    private fun reset(cheat: Cheat) = with(cheat) { player.setStatLevels(level = 1) }

    private fun setAll(cheat: Cheat) =
        with(cheat) {
            val level = args.single().toInt()
            if (!player.validateSkillLevel(level)) {
                return
            }
            player.setStatLevels(level)
            player.mes("Set all skills to level $level (respecting each skill's minimum level).")
        }

    private fun setLevel(cheat: Cheat) =
        with(cheat) {
            val level = args.last().toInt()
            if (!player.validateSkillLevel(level)) {
                return
            }
            val input = args.dropLast(1).joinToString("_")
            val stat = resolveStat(input)
            if (stat == null) {
                player.mes("There is no skill named or numbered '$input'.")
                return
            }
            val appliedLevel = player.setStatLevel(stat, level)
            player.mes("Set ${stat.displayName} to level $appliedLevel.")
        }

    private fun Player.validateSkillLevel(level: Int): Boolean {
        if (level in MIN_SKILL_LEVEL..MAX_SKILL_LEVEL) {
            return true
        }
        mes("Skill level must be between $MIN_SKILL_LEVEL and $MAX_SKILL_LEVEL.")
        return false
    }

    private fun resolveStat(input: String): UnpackedStatType? {
        val id = input.toIntOrNull()
        if (id != null) {
            return statTypes[id]
        }
        val normalized = input.normalizeTypeName()
        return statTypes.values.firstOrNull { stat ->
            stat.internalName?.normalizeTypeName() == normalized ||
                stat.displayName.normalizeTypeName() == normalized
        }
    }

    private fun String.normalizeTypeName(): String = lowercase().replace('-', '_').replace(' ', '_')

    private fun mypos(cheat: Cheat) =
        with(cheat) {
            player.mes("${player.coords}:")
            player.mes("  ${ZoneKey.from(player.coords)} - ${ZoneGrid.from(player.coords)}")
            player.mes(
                "  ${MapSquareKey.from(player.coords)} - ${MapSquareGrid.from(player.coords)}"
            )
            player.mes("  BuildArea(${player.buildArea})")
        }

    private fun tele(cheat: Cheat) =
        with(cheat) {
            val values = if (args.size == 1) args[0].split(",") else args
            val destination = parseTeleportCoords(values, player.coords.level)
            player.safeTeleport(destination)
        }

    private fun teleZone(cheat: Cheat) =
        with(cheat) {
            val args = if (args.size == 1) args[0].split(",") else args
            val zoneX = args[0].toInt()
            val zoneZ = args[1].toInt()
            val level = args[2].toInt()
            val destination = ZoneKey(zoneX, zoneZ, level).toCoords()
            player.safeTeleport(destination)
        }

    private fun home(cheat: Cheat) = with(cheat) { player.safeTeleport(realm.config.spawnCoord) }

    private fun parseTeleportCoords(args: List<String>, currentLevel: Int): CoordGrid =
        when (args.size) {
            2 -> CoordGrid(x = args[0].toInt(), z = args[1].toInt(), level = currentLevel)
            3 -> CoordGrid(x = args[0].toInt(), z = args[1].toInt(), level = args[2].toInt())
            5 ->
                CoordGrid(
                    level = args[0].toInt(),
                    mx = args[1].toInt(),
                    mz = args[2].toInt(),
                    lx = args[3].toInt(),
                    lz = args[4].toInt(),
                )
            else -> throw IndexOutOfBoundsException("Invalid teleport argument count: ${args.size}")
        }

    private fun Player.safeTeleport(requested: CoordGrid) {
        val destination = findNearestSafeTeleport(requested)
        if (destination == null) {
            mes(
                "Teleport cancelled: no walkable tile was found within " +
                    "$TELEPORT_SEARCH_RADIUS tiles of ${requested.toConventionalString()}."
            )
            return
        }
        protectedAccess.launch(this, busyText = "Close any open interfaces before teleporting.") {
            if (destination != requested) {
                mes(
                    "The requested tile has no walkable floor. Using the nearest safe tile: " +
                        destination.toConventionalString()
                )
            } else {
                mes("Teleported to ${destination.toConventionalString()}.")
            }
            teleport(destination)
        }
    }

    private fun findNearestSafeTeleport(requested: CoordGrid): CoordGrid? {
        for (radius in 0..TELEPORT_SEARCH_RADIUS) {
            for (xOffset in -radius..radius) {
                for (zOffset in -radius..radius) {
                    if (max(kotlin.math.abs(xOffset), kotlin.math.abs(zOffset)) != radius) {
                        continue
                    }
                    val x = requested.x + xOffset
                    val z = requested.z + zOffset
                    if (x !in 0 until CoordGrid.MAP_WIDTH || z !in 0 until CoordGrid.MAP_LENGTH) {
                        continue
                    }
                    val candidate = CoordGrid(x, z, requested.level)
                    if (candidate.isSafeTeleportDestination()) {
                        return candidate
                    }
                }
            }
        }
        return null
    }

    private fun CoordGrid.isSafeTeleportDestination(): Boolean =
        collision.isZoneValid(this) && collision[this] and TELEPORT_BLOCK_FLAGS == 0

    private fun anim(cheat: Cheat) =
        with(cheat) {
            val resolvedName = resolveTypeName(args.asTypeName(), names.seqs)
            val typeId = resolveArgTypeId(resolvedName, names.seqs)
            if (typeId == null) {
                player.mes("There is no seq mapped to: '$resolvedName'")
                return
            }
            val type = seqTypes[typeId]
            if (type == null) {
                player.mes("That seq does not exist: $typeId")
                return
            }
            player.anim(type)
            player.mes("Anim: '${type.internalName}' (priority=${type.priority})")
            logger.debug { "Anim: $type" }
        }

    private fun spotanim(cheat: Cheat) =
        with(cheat) {
            val (typeName, heightArg) = args.asTypeNameAndNumber(defaultNumber = 0)
            val resolvedName = resolveTypeName(typeName, names.spotanims)
            val typeId = resolveArgTypeId(resolvedName, names.spotanims)
            if (typeId == null) {
                player.mes("There is no spotanim mapped to: '$resolvedName'")
                return
            }
            val type = spotTypes[typeId]
            if (type == null) {
                player.mes("That spotanim does not exist: $typeId")
                return
            }
            val height = min(heightArg.toInt(), Short.MAX_VALUE.toInt())
            player.spotanim(type, delay = 0, height = height, slot = 0)
            player.mes("Spotanim: '${type.internalName}' (height=$height)")
            logger.debug { "Spotanim: $type" }
        }

    private fun locAdd(cheat: Cheat) =
        with(cheat) {
            val resolvedName = resolveTypeName(args[1], names.locs)
            val typeId = resolveArgTypeId(resolvedName, names.locs)
            if (typeId == null) {
                player.mes("There is no loc mapped to name: '$resolvedName'")
                return
            }
            val type = locTypes[typeId]
            if (type == null) {
                player.mes("That loc does not exist: $typeId")
                return
            }
            val duration = args[0].toInt()
            val angle = args.getOrNull(2)?.toInt() ?: LocAngle.West.id
            val shape = args.getOrNull(3)?.toInt() ?: LocShape.CentrepieceStraight.id
            val layer = LocLayerConstants.of(shape)
            val loc = LocInfo(layer, player.coords, LocEntity(type.id, shape, angle))
            locRepo.add(loc, duration)
            player.mes("Spawned loc '${type.internalName}' (duration: $duration cycles)")
            logger.debug { "Spawned loc: loc=$loc, type=$type" }
        }

    private fun locDel(cheat: Cheat) =
        with(cheat) {
            val zone = ZoneKey.from(player.coords)
            val locs = locRepo.findAll(zone).filter { it.coords == player.coords }.toList()
            if (locs.isEmpty()) {
                player.mes("No loc found on ${player.coords}")
                return
            }
            val duration = args[0].toInt()
            val shape = args.getOrNull(1)?.toIntOrNull() ?: LocShape.CentrepieceStraight.id
            val loc = locs.firstOrNull { it.shapeId == shape }
            if (loc == null) {
                player.mes("No loc with shape `${LocShape[shape]}` found on ${player.coords}")
                return
            }
            val type = locTypes[loc]
            locRepo.del(loc, duration)
            player.mes("Deleted loc `${type.internalName}` (duration: $duration cycles)")
            logger.debug { "Deleted loc: loc=$loc, type=$type" }
        }

    private fun npcAdd(cheat: Cheat) =
        with(cheat) {
            val resolvedName = resolveTypeName(args[1], names.npcs)
            val typeId = resolveArgTypeId(resolvedName, names.npcs)
            if (typeId == null) {
                player.mes("There is no npc mapped to name: '$resolvedName'")
                return
            }
            val type = npcTypes[typeId]
            if (type == null) {
                player.mes("That npc does not exist: $typeId")
                return
            }
            val duration = args[0].toInt()
            val npc = Npc(type, player.coords)
            npc.mode = NpcMode.None
            npcRepo.add(npc, duration)
            player.mes("Spawned npc `${type.internalName}` (duration: $duration cycles)")
        }

    private fun invAdd(cheat: Cheat) =
        with(cheat) {
            val (typeName, countArg) = args.asTypeNameAndNumber(defaultNumber = 1)
            val normalizedName = typeName.replace("cert_", "")
            val type = resolveObjType(normalizedName)
            if (type == null) {
                player.mes("There is no obj named or numbered '$normalizedName'.")
                return
            }
            val spawnCert = typeName.startsWith("cert_")
            val resolvedType =
                if (spawnCert && type.canCert) objTypes.getValue(type.certlink) else type
            val count = countArg.toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val objName = type.internalName ?: type.name
            val spawned = player.invAdd(player.inv, resolvedType, count, strict = false)
            if (spawned.err is TransactionResult.RestrictedDummyitem) {
                player.mes("You can't spawn this item!")
                return
            }
            player.mes("Spawned inv obj `$objName` x ${spawned.completed().formatAmount}")
        }

    private fun resolveObjType(input: String): UnpackedObjType? {
        val id = input.toIntOrNull() ?: names.objs[input]
        if (id != null) {
            return objTypes[id]
        }
        val normalized = input.normalizeTypeName()
        val exactInternal =
            objTypes.values.firstOrNull { it.internalName?.normalizeTypeName() == normalized }
        if (exactInternal != null) {
            return exactInternal
        }
        val exact =
            objTypes.values
                .filter { it.name.normalizeTypeName() == normalized }
                .minByOrNull { it.id }
        if (exact != null) {
            return exact
        }
        return findClosestObjMatch(normalized)
    }

    private fun findClosestObjMatch(input: String): UnpackedObjType? {
        val normalizedInput = input.replace("_", " ")
        var bestScore = 0.0f
        var bestMatch: UnpackedObjType? = null
        for (type in objTypes.values) {
            val candidates = listOfNotNull(type.internalName, type.name)
            for (candidate in candidates) {
                val score =
                    levenshteinMetric.compare(
                        normalizedInput,
                        candidate.normalizeTypeName().replace("_", " "),
                    )
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = type
                }
            }
        }
        return if (bestScore >= 0.5) bestMatch else null
    }

    private fun giveLeagueItems(cheat: Cheat) {
        val missing = DEMONIC_PACT_ITEMS.filter { name -> objTypes.values.none { it.internalName == name } }
        if (missing.isNotEmpty()) {
            cheat.player.mes("Demonic Pacts items require the revision-238 cache and symbols.")
            return
        }
        givePreset(cheat, "Demonic Pacts items", DEMONIC_PACT_ITEMS.map { PresetEntry(it, 1) })
    }

    private fun giveRunes(cheat: Cheat) = givePreset(cheat, "runes", RUNE_PRESET)

    private fun giveAmmo(cheat: Cheat) = givePreset(cheat, "ammunition", ammoPreset())

    private fun giveBisMelee(cheat: Cheat) = givePreset(cheat, "melee loadout", BIS_MELEE_PRESET)

    private fun giveBisRange(cheat: Cheat) = givePreset(cheat, "ranged loadout", BIS_RANGE_PRESET)

    private fun giveBisMage(cheat: Cheat) = givePreset(cheat, "magic loadout", BIS_MAGE_PRESET)

    private fun giveSkilling(cheat: Cheat) =
        givePreset(cheat, "skilling tools and outfits", SKILLING_PRESET)

    private fun ammoPreset(): List<PresetEntry> =
        objTypes.values
            .filter { AMMO_NAME_PATTERN.matches(it.name) }
            .groupBy { it.name }
            .values
            .map { types -> types.minBy { it.id } }
            .map { PresetEntry(it.id.toString(), PRESET_STACK_COUNT) } +
            EXTRA_AMMO_NAMES.map { PresetEntry(it, PRESET_STACK_COUNT) }

    private fun givePreset(cheat: Cheat, presetName: String, entries: List<PresetEntry>) =
        with(cheat) {
            val bank = player.invMap.getOrPut(invTypes[invs.bank])
            var addedCount = 0L
            var addedTypes = 0
            val skipped = mutableListOf<String>()
            for (entry in entries) {
                val type = resolveObjType(entry.name)
                if (type == null) {
                    skipped += entry.name
                    continue
                }
                val invAdd = player.invAdd(player.inv, type, entry.count, strict = false)
                val invCount = invAdd.completed()
                val remaining = entry.count - invCount
                val bankCount =
                    if (remaining > 0) {
                        player.invAdd(bank, type, remaining, strict = false).completed()
                    } else {
                        0
                    }
                val completed = invCount + bankCount
                if (completed > 0) {
                    addedTypes++
                    addedCount += completed.toLong()
                }
                if (completed < entry.count) {
                    skipped += "${type.name} x ${(entry.count - completed).formatAmount}"
                }
            }
            player.syncBankTabMetadata(bank)
            player.mes(
                "Added $presetName: ${addedCount.formatAmount} items across $addedTypes types. " +
                    "Overflow went to your bank."
            )
            if (skipped.isNotEmpty()) {
                player.mes("Could not add ${skipped.size} entries; check free bank space.")
            }
        }

    private fun Player.syncBankTabMetadata(bank: Inventory) {
        val mainTab =
            varBitTypes.values.firstOrNull { it.internalName == "bank_tab_main" } ?: return
        val customTabSize =
            (1..9).sumOf { index ->
                val tab = varBitTypes.values.firstOrNull { it.internalName == "bank_tab_$index" }
                if (tab == null) 0 else vars[tab]
            }
        val expectedMainTabSize = (bank.occupiedSpace() - customTabSize).coerceAtLeast(0)
        if (vars[mainTab] == expectedMainTabSize) {
            return
        }
        VarPlayerIntMapSetter.set(this, mainTab, expectedMainTabSize)
        resyncVar(mainTab)
    }

    private fun invClear(cheat: Cheat) = with(cheat) { player.invClear(player.inv) }

    private fun xpRate(cheat: Cheat) =
        with(cheat) {
            if (args.isNotEmpty()) {
                val rate = args.single().toDouble()
                if (!rate.isFinite() || rate !in MIN_XP_RATE..MAX_XP_RATE) {
                    player.mes("XP rate must be between ${MIN_XP_RATE}x and ${MAX_XP_RATE}x.")
                    return
                }
                player.xpRate = rate
            }
            val effective = player.xpRate * player.globalXpRate
            player.mes(
                "XP rate: personal=${player.xpRate}x, global=${player.globalXpRate}x, " +
                    "effective=${effective}x."
            )
        }

    private fun setVarp(cheat: Cheat) =
        with(cheat) {
            val resolvedName = resolveTypeName(args[0], names.varps)
            val typeId = resolveArgTypeId(resolvedName, names.varps)
            if (typeId == null) {
                player.mes("There is no varp mapped to name: '$resolvedName'")
                return
            }
            val type = varpTypes[typeId]
            if (type == null) {
                player.mes("That varp does not exist: $typeId")
                return
            }
            val value = args[1].toInt()
            player.vars.backing[type.id] = value
            player.resyncVar(type)
            player.mes("Set varp '${type.internalName}' to value: ${player.vars[type]}")
        }

    private fun setVarBit(cheat: Cheat) =
        with(cheat) {
            val resolvedName = resolveTypeName(args[0], names.varbits)
            val typeId = resolveArgTypeId(resolvedName, names.varbits)
            if (typeId == null) {
                player.mes("There is no varbit mapped to name: '$resolvedName'")
                return
            }
            val type = varBitTypes[typeId]
            if (type == null) {
                player.mes("That varbit does not exist: $typeId")
                return
            }
            val value = args[1].toInt()
            VarPlayerIntMapSetter.set(player, type, value)
            player.mes("Set varbit '${type.internalName}' to value: ${player.vars[type]}")
        }

    @OptIn(InternalApi::class)
    private fun Player.setStatLevels(level: Int) {
        for (stat in statTypes.values) {
            setStatLevel(stat, level)
        }
    }

    @OptIn(InternalApi::class)
    private fun Player.setStatLevel(stat: UnpackedStatType, level: Int): Int {
        val targetLevel = max(stat.minLevel, level)
        val targetXp = PlayerSkillXPTable.getXPFromLevel(targetLevel)
        val baseLevel = statMap.getBaseLevel(stat)
        if (baseLevel > targetLevel) {
            statRevert(stat, targetLevel, targetXp)
            return targetLevel
        }
        val xpDelta = targetXp - statMap.getXP(stat)
        statMap.setCurrentLevel(stat, targetLevel.toByte())
        statAdvance(stat, xpDelta.toDouble(), rate = 1.0)
        return targetLevel
    }

    // There is, by design, no helper function to decrease stat xp, as xp reduction is not a
    // standard operation in normal gameplay.
    @OptIn(InternalApi::class)
    private fun Player.statRevert(stat: StatType, targetLevel: Int, targetXp: Int) {
        statMap.setCurrentLevel(stat, statMap.getBaseLevel(stat))
        val levelDelta = stat(stat) - targetLevel
        require(levelDelta > 0) { "This function can only be used to reduce stat levels." }
        statMap.setXP(stat, targetXp)
        statMap.setBaseLevel(stat, targetLevel.toByte())
        statSub(stat, constant = levelDelta, percent = 0)
        appearance.combatLevel = PlayerSkillXP.calculateCombatLevel(this)
        PlayerInterfaceUpdates.updateCombatLevel(this)
    }

    private fun reboot(cheat: Cheat) {
        logger.info { "Reboot initiated by '${cheat.player.username}'." }
        SafeServiceExit.terminate()
    }

    private fun slowReboot(cheat: Cheat) =
        with(cheat) {
            val cycles = min(args[0].toInt(), 65535)
            if (cycles <= 0) {
                update.clear()
                return@with
            }
            update.startCountdown(cycles)
            for (p in playerList) {
                MiscOutput.updateRebootTimer(p, cycles)
            }
        }

    private fun resolveArgTypeId(arg: String, names: Map<String, Int>): Int? {
        val argAsInt = arg.toIntOrNull()
        if (argAsInt != null) {
            return argAsInt
        }
        val sanitized = arg.replace("-", "_")
        return names[sanitized]
    }

    private fun resolveTypeName(name: String, names: Map<String, Int>): String =
        when {
            name in names -> name
            name.toIntOrNull() != null -> name
            else -> findClosestNameMatch(name, names.keys) ?: name
        }

    private fun List<String>.asTypeNameAndNumber(defaultNumber: Number): Pair<String, String> =
        if (size > 1 && last().toLongOrNull() != null) {
            dropLast(1).joinToString("_") to last()
        } else {
            joinToString("_") to defaultNumber.toString()
        }

    private fun List<String>.asTypeName(): String = joinToString("_")

    private fun findClosestNameMatch(input: String, names: Iterable<String>): String? {
        val normalizedInput = input.replace("_", " ")

        var bestMatchScore = 0.0f
        var bestMatchName: String? = null
        for (name in names) {
            val score = levenshteinMetric.compare(normalizedInput, name.replace("_", " "))
            if (score > bestMatchScore) {
                bestMatchScore = score
                bestMatchName = name
            }
        }

        return if (bestMatchScore >= 0.5) bestMatchName else null
    }

    private companion object {
        val AMMO_NAME_PATTERN =
            Regex(
                "^(Bronze|Iron|Steel|Black|Mithril|Adamant|Rune|Amethyst|Dragon) " +
                    "(arrow|dart|knife)( ?\\(p(?:\\+|\\+\\+)?\\))?$"
            )

        val EXTRA_AMMO_NAMES =
            listOf(
                "slayer_broad_arrows",
                "ice_arrow",
                "ogre_arrow",
                "ogre_headless_arrow",
                "atlatl_dart",
            )

        val RUNE_PRESET =
            listOf(
                    "airrune",
                    "waterrune",
                    "earthrune",
                    "firerune",
                    "mindrune",
                    "bodyrune",
                    "cosmicrune",
                    "chaosrune",
                    "naturerune",
                    "lawrune",
                    "deathrune",
                    "bloodrune",
                    "soulrune",
                    "astralrune",
                    "wrathrune",
                    "mistrune",
                    "dustrune",
                    "mudrune",
                    "smokerune",
                    "steamrune",
                    "lavarune",
                    "sunfirerune",
                    "aetherrune",
                )
                .map { PresetEntry(it, PRESET_STACK_COUNT) }

        val BIS_MELEE_PRESET =
            listOf(
                    "torva_helm",
                    "torva_chest",
                    "torva_legs",
                    "Infernal cape",
                    "Amulet of rancour",
                    "Ferocious gloves",
                    "Avernic treads (max)",
                    "Ultor ring",
                    "zeah_blessing_elite",
                    "Scythe of vitur",
                    "Avernic defender",
                )
                .map(::PresetEntry)

        val BIS_RANGE_PRESET =
            listOf(
                    "masori_mask_fortified",
                    "masori_body_fortified",
                    "masori_chaps_fortified",
                    "dizanas_quiver_charged",
                    "zenyte_necklace_enchanted",
                    "Zaryte vambraces",
                    "Avernic treads (max)",
                    "Venator ring",
                    "Twisted bow",
                    "Zaryte crossbow",
                    "Twisted buckler",
                    "Dragon arrow",
                    "Dragon bolts (e)",
                )
                .map { name ->
                    PresetEntry(name, if (name.startsWith("Dragon ")) PRESET_STACK_COUNT else 1)
                }

        val BIS_MAGE_PRESET =
            listOf(
                    "Ancestral hat",
                    "Ancestral robe top",
                    "Ancestral robe bottom",
                    "ma2_saradomin_cape",
                    "Occult necklace",
                    "zenyte_bracelet_enchanted",
                    "Avernic treads (max)",
                    "Magus ring",
                    "Tumeken's shadow",
                    "elidinis_ward_fortified",
                    "Kodai wand",
                    "Saturated heart",
                )
                .map(::PresetEntry)

        val SKILLING_PRESET =
            listOf(
                    "Crystal pickaxe",
                    "Crystal axe",
                    "Crystal harpoon",
                    "Infernal pickaxe",
                    "Infernal axe",
                    "Infernal harpoon",
                    "Imcando hammer",
                    "eyeglo_crystal_saw",
                    "Magic butterfly net",
                    "fairy_enchanted_secateurs",
                    "bottomless_compost_bucket_filled",
                    "Herb sack",
                    "Seed box",
                    "Coal bag",
                    "Gem bag",
                    "Plank sack",
                    "Fish barrel",
                    "Tackle box",
                    "Dodgy necklace",
                    "hunting_silent_gloves",
                    "gauntlets_of_cooking",
                    "gauntlets_of_goldsmithing",
                    "Ice gloves",
                    "celestial_ring_charged",
                    "ring_of_endurance",
                    "Graceful hood",
                    "Graceful cape",
                    "Graceful top",
                    "Graceful legs",
                    "Graceful gloves",
                    "Graceful boots",
                    "Prospector helmet",
                    "Prospector jacket",
                    "Prospector legs",
                    "Prospector boots",
                    "Lumberjack hat",
                    "Lumberjack top",
                    "Lumberjack legs",
                    "Lumberjack boots",
                    "Spirit angler headband",
                    "Spirit angler top",
                    "Spirit angler waders",
                    "Spirit angler boots",
                    "Pyromancer hood",
                    "Pyromancer garb",
                    "Pyromancer robe",
                    "Pyromancer boots",
                    "Rogue mask",
                    "Rogue top",
                    "Rogue trousers",
                    "Rogue gloves",
                    "Rogue boots",
                    "Hat of the eye",
                    "Robe top of the eye",
                    "Robe bottoms of the eye",
                    "Boots of the eye",
                    "Carpenter's helmet",
                    "Carpenter's shirt",
                    "Carpenter's trousers",
                    "Carpenter's boots",
                    "Smiths tunic",
                    "Smiths trousers",
                    "Smiths gloves (i)",
                    "Smiths boots",
                    "Zealot's helm",
                    "Zealot's robe top",
                    "Zealot's robe bottom",
                    "Zealot's boots",
                )
                .map { name -> PresetEntry(name, if (name == "Dodgy necklace") 100 else 1) }

        const val MIN_SKILL_LEVEL: Int = 1
        const val MAX_SKILL_LEVEL: Int = 99
        const val MIN_XP_RATE: Double = 0.0
        const val MAX_XP_RATE: Double = 1_000.0
        const val PRESET_STACK_COUNT: Int = 100_000
        const val TELEPORT_SEARCH_RADIUS: Int = 16
        val TELEPORT_BLOCK_FLAGS: Int =
            CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR
    }

    private data class PresetEntry(val name: String, val count: Int = 1)
}
