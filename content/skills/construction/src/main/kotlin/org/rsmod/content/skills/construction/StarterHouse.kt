package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.CultivatedSpiritTrees
import org.rsmod.api.player.events.HouseFairyRingDestination
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.construction.CrystalSaw.Companion.charges
import org.rsmod.content.skills.construction.CrystalSaw.Companion.useCharge
import org.rsmod.content.skills.magic.spell.utility.standard.HouseLocation
import org.rsmod.content.skills.magic.spell.utility.standard.PlayerOwnedHousePresenceRepository
import org.rsmod.content.skills.magic.spell.utility.standard.PlayerOwnedHouseRepository
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.region.Region
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.HashedNpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GardenTools : ObjReferences() {
    val wateringCans = (0..8).map { find("watering_can_$it") }
}

internal object GardenAnimations : SeqReferences() {
    val water = find("farming_watering")
}

internal object HouseVarps : VarpReferences() {
    val furniture = find("poh_starter_parlour_furniture")
}

internal object HouseVarpBuilder : VarpBuilder() {
    init {
        build("poh_starter_parlour_furniture") { transmitNever = true }
    }
}

internal object HouseTravelBits : VarBitReferences() {
    val locked = find("poh_house_locked")
    val ironman = find("ironman")
    val style = find("poh_house_style")
    val kourendVisited = find("zeah_playerhasvisited")
    val varlamoreVisited = find("varlamore_visited")
}

private enum class HouseStyle(
    val label: String,
    val level: Int,
    val price: Int,
    val template: Int = -1,
) {
    Wood("Basic wood", 1, 5000),
    Stone("Basic stone", 10, 5000),
    Whitewashed("Whitewashed stone", 20, 7500),
    Fremennik("Fremennik-style wood", 30, 10000),
    Tropical("Tropical wood", 40, 15000),
    Fancy("Fancy stone", 50, 25000),
    Hosidius("Hosidius-style stone", 1, 5000, 8),
}

private enum class HouseSite(
    val location: HouseLocation,
    val portal: String,
    val level: Int,
    val fee: Int,
) {
    Rimmington(HouseLocation.Rimmington, "poh_rimmington_portal", 1, 5000),
    Taverley(HouseLocation.Taverley, "poh_taverly_portal", 10, 5000),
    Pollnivneach(HouseLocation.Pollnivneach, "poh_pollnivneach_portal", 20, 7500),
    Hosidius(HouseLocation.Hosidius, "poh_kourend_portal", 25, 8750),
    Rellekka(HouseLocation.Rellekka, "poh_rellekka_portal", 30, 10000),
    Aldarin(HouseLocation.Aldarin, "poh_aldarin_portal", 35, 12500),
    Brimhaven(HouseLocation.Brimhaven, "poh_brimhaven_portal", 40, 15000),
    Yanille(HouseLocation.Yanille, "poh_yanille_portal", 50, 25000),
    Prifddinas(HouseLocation.Prifddinas, "poh_prifddinas_portal", 70, 50000),
}

@Singleton
public class StarterHouse
@Inject
internal constructor(
    private val regions: RegionRepository,
    private val registry: RegionRegistry,
    private val locs: LocRepository,
    private val locTypes: LocTypeList,
    private val objTypes: ObjTypeList,
    private val layouts: HouseRoomRepository,
    private val furniture: HouseFurniture,
    private val costumes: CostumeCatalogue,
    private val presence: PlayerOwnedHousePresenceRepository,
    private val clock: MapClock,
    private val players: PlayerRegistry,
    private val online: org.rsmod.game.entity.PlayerList,
    private val collision: org.rsmod.routefinder.collision.CollisionFlagMap,
    private val petRocks: HousePetRocks,
    private val menagerie:MenagerieResidents,
) {
    private data class Home(
        val region: Region,
        val building: Boolean,
        val style: Int,
        val hotspots: Map<Int, List<LocInfo>>,
        val groundPlane:Int,
    ) {
        fun origin(cell: Int): CoordGrid =
            region.southWest.translate((cell and 15) * 8, ((cell shr 4) and 15) * 8, groundPlane + (cell shr 8))

        fun cell(coords: CoordGrid): Int =
            ((coords.x - region.southWest.x) / 8) + 16 * ((coords.z - region.southWest.z) / 8) + 256 * (coords.level-groundPlane)

        fun stairLanding(room:HouseRoom):CoordGrid {
            // Straight flights permit approach from only one end of the native 2x2 stairs.
            val local=when {
                room.variant(1) in listOf(3,5) -> org.rsmod.map.zone.ZoneGrid(2,3)
                room.stairDirection<0 -> org.rsmod.map.zone.ZoneGrid(3,5)
                else -> org.rsmod.map.zone.ZoneGrid(3,2)
            }
            val offset=org.rsmod.game.region.util.RegionRotations.translateCoords(room.rotation,local)
            return origin(room.cell).translate(offset)
        }

        val entrance: CoordGrid
            get() = origin(HouseRoom.ENTRANCE_CELL).translate(4, 2)
    }

    private data class Visit(val host: Player, val home: Home, val outside: CoordGrid)
    private val visits = WeakHashMap<Player, Visit>()
    private val homes = WeakHashMap<Player, Home>()
    private val byName by lazy { locTypes.values.associateBy { it.internalName } }

    public fun purchase(access: ProtectedAccess) {
        val player = access.player
        if (!player.members || player.vars[varbits.poh_house_location] != 0) return
        if (!access.invTakeFee(1000)) {
            access.mes("You need 1,000 coins to buy a house.")
            return
        }
        VarPlayerIntMapSetter.set(
            player,
            varbits.poh_house_location,
            HouseLocation.Rimmington.cacheValue,
        )
        access.mes("You now own a house. You can enter it through the portal in Rimmington.")
    }

    public fun prepare(player: Player, building: Boolean): CoordGrid? {
        if(building && hasGuests(player))return null
        if (!player.members || player.vars[varbits.poh_house_location] == 0) return null
        val style = player.vars[HouseTravelBits.style].coerceIn(0, 11)
        val existing = homes[player]
        if (
            existing != null &&
                existing.region.slot != Region.INVALID_SLOT &&
                existing.building == building &&
                existing.style == style
        ) {
            existing.region.reclaimFromCycle = clock + ARRIVAL_RESERVATION_TICKS
            return existing.entrance
        }
        val layout = layouts.layout(player)
        val home = allocate(layout, building, style) ?: return null
        enter(player, home)
        return home.entrance
    }

    public fun fairyRingDestination(player: Player): CoordGrid? =
        gardenRoom(player, setOf(2, 3))?.let { gardenLanding(player, it) }

    internal fun resolveSpiritTree(request: CultivatedSpiritTrees) {
        val room = gardenRoom(request.player, setOf(1, 3)) ?: return
        request.hasHouseTree = true
        if (request.reserveHouse)
            request.houseDestination = gardenLanding(request.player, room)?.translateZ(-1)
    }

    private fun gardenRoom(player: Player, variants: Set<Int>): HouseRoom? {
        if (!player.members || player.vars[varbits.poh_house_location] == 0) return null
        return layouts.layout(player).values.firstOrNull {
            it.type == HouseRoomType.SuperiorGarden && it.variant(1) in variants
        }
    }

    private fun gardenLanding(player: Player, room: HouseRoom): CoordGrid? {
        prepare(player, false) ?: return null
        val hotspot =
            homes[player]?.hotspots?.get(room.cell)?.firstOrNull {
                locTypes[it.id]?.internalName == "poh_superior_garden_hotspot_treering"
            } ?: return null
        return hotspot.coords
    }

    private fun allocate(layout: Map<Int, HouseRoom>, building: Boolean, style: Int): Home? {
        val groundPlane=if(layout.values.any{it.floor<0})1 else 0
        val template =
            RegionTemplate.create {
                for (room in layout.values) {
                    val source =
                        ZoneKey(
                            room.source.x / 8 + 8 * (style / 4),
                            room.source.z / 8,
                            style % 4,
                        )
                    this[room.x, room.z, groundPlane+room.floor] =
                        RegionZoneCopy(source, rotation = room.rotation, flag = null)
                }
            }
        val region = regions.add(template) ?: return null
        region.reclaimFromCycle = clock + ARRIVAL_RESERVATION_TICKS
        val hotspots = mutableMapOf<Int, List<LocInfo>>()
        val home = Home(region, building, style, hotspots,groundPlane)
        for (room in layout.values) {
            val originals = locs.findAll(ZoneKey.from(home.origin(room.cell))).toList()
            hotspots[room.cell] = originals.filter { hotspot(room, it) != null }
            for (loc in originals) {
                val name = locTypes[loc.id]?.internalName.orEmpty()
                if (name.startsWith("poh_hotspot_door")) {
                    val next = layout[room.neighbour(loc.angleId)]
                    if (next != null && next.type.hasDoor((loc.angleId + 2) and 3, next.rotation))
                        locs.del(loc, Int.MAX_VALUE)
                } else if (room.cell == HouseRoom.ENTRANCE_CELL && name == "poh_crude_garden_1") {
                    locs.change(loc, byName.getValue("poh_exit_portal"), Int.MAX_VALUE)
                }
            }
            for (slot in 1..8) refresh(room, home, slot)
        }
        return home
    }

    private fun enter(player: Player, home: Home) {
        petRocks.clear(player)
        menagerie.clear(player)
        homes[player] = home
        val petRoom=layouts.layout(player).values.firstOrNull{it.type.isMenagerie}
        if(petRoom!=null && !home.building)menagerie.restore(player,home.origin(petRoom.cell),petRoom)
        for (room in layouts.layout(player).values) petRocks.restore(
            player,
            room,
            home.origin(room.cell),
        )
        VarPlayerIntMapSetter.set(player, varbits.poh_building_mode, if (home.building) 1 else 0)
        if(visitOf(player)==null)presence.enter(player) { occupant -> registry[occupant.coords] === home.region }
    }

    public suspend fun buildRoom(access: ProtectedAccess, door: BoundLocInfo) {
        with(access) {
            val home = homes[player] ?: return
            if (
                !home.building ||
                    registry[coords] !== home.region ||
                    registry[door.coords] !== home.region
            )
                return
            val layout = layouts.layout(player)
            val room = layout[home.cell(door.coords)] ?: return
            val angle = door.angleId
            val cell = room.neighbour(angle)
            if (cell in layout) {
                mes("There is already a room there.")
                return
            }
            val level = statBase(stats.construction)
            val dimension = (3 + level / 15).coerceAtMost(7)
            if (
                (cell and 15) !in 1..dimension ||
                    ((cell shr 4) and 15) !in 1..dimension
            ) {
                mes("You cannot extend the house any further there at your current level.")
                return
            }
            if(room.floor==1 && layout[cell-256]?.type?.outdoors != false){
                mes("You need an indoor room directly below this space.")
                return
            }
            val choices = HouseRoomType.entries.filter { it.permits(room.floor) }
            val type =
                choices.getOrNull(
                    menu(
                        "Build a room",
                        true,
                        choices.map { "${it.label} (level ${it.level}, ${it.fee} coins)" },
                    )
                ) ?: return
            val existing = layout.values.firstOrNull { type.isMenagerie && it.type.isMenagerie }
            if (existing?.type == type) {
                mes("You can only build one menagerie.")
                return
            }
            if (existing == null && layout.size >= roomLimit(level)) {
                mes("You cannot add any more rooms at your current level.")
                return
            }
            val fee = if (existing != null) 0 else type.fee
            if (type == HouseRoomType.CostumeRoom && layout.values.any { it.type == type }) {
                mes("You can only build one costume room.")
                return
            }
            if (stat(stats.construction) < type.level) {
                mes("You need a Construction level of ${type.level} to build that room.")
                return
            }
            if (invCoinTotal() < fee) {
                mes("You need ${type.fee} coins to build that room.")
                return
            }
            val orientations = (0..3).filter { type.hasDoor((angle + 2) and 3, it) }
            val rotation =
                orientations.getOrNull(
                    menu(
                        "Choose the room rotation",
                        true,
                        orientations.map { "${it * 90} degrees" },
                    )
                ) ?: return
            val replacement = existing?.copy(cell = cell, type = type, rotation = rotation) ?: HouseRoom(cell, type, rotation)
            val proposed = (if (existing == null) layout else layout - existing.cell) + (cell to replacement)
            if (!HouseRoom.connected(proposed)) {
                mes("That change would cut off part of your house.")
                return
            }
            changeLayout(access, home, proposed, fee)
        }
    }

    public suspend fun plan(access: ProtectedAccess) {
        with(access) {
            val home = homes[player] ?: return
            if (registry[coords] !== home.region || !home.building) {
                mes("Enter your house in building mode to edit its rooms.")
                return
            }
            val layout = layouts.layout(player)
            val rooms =
                layout.values.filter { it.cell != HouseRoom.ENTRANCE_CELL }.sortedBy { it.cell }
            val room =
                rooms.getOrNull(
                    menu("Manage a room", true, rooms.map { "${it.type.label} (${it.x}, ${it.z}, ${when(it.floor){-1 -> "basement";1 -> "upper";else -> "ground"}})" })
                ) ?: return
            val action =
                choice3(
                    "Rotate clockwise.",
                    1,
                    "Remove this room and its furniture.",
                    2,
                    "Cancel.",
                    0,
                )
            if (action == 0) return
            if (room.petRocks != 0L) {
                mes("Pick up the pet rocks in the room first.")
                return
            }
            if(action==2 && room.type.isMenagerie && (menagerie.count(player)>0 || menagerie.hasFighters(player))){mes("Take your pets out of the menagerie first.");return}
            if (action == 2 && room.type == HouseRoomType.CostumeRoom && costumes.hasStored(player)) {
                mes("Empty the costume room before removing it.")
                return
            }
            val proposed =
                if (action == 1)
                    layout + (room.cell to room.copy(rotation = (room.rotation + 1) and 3))
                else layout - room.cell
            if (!HouseRoom.connected(proposed)) {
                mes("That change would cut off part of your house.")
                return
            }
            if (
                action == 2 &&
                    !choice2(
                        "Yes, remove the room and everything in it.",
                        true,
                        "Keep the room.",
                        false,
                    )
            )
                return
            changeLayout(access, home, proposed, 0)
        }
    }

    internal suspend fun removeLanding(access:ProtectedAccess,target:BoundLocInfo){
        with(access){
            val home=homes[player]?:return
            if(!home.building || registry[coords]!==home.region || registry[target.coords]!==home.region)return
            val layout=layouts.layout(player)
            val room=layout[home.cell(target.coords)]?:return
            val landing=room.stairLanding(layout)?:return
            if(landing.petRocks!=0L){mes("Pick up the pet rocks in the room first.");return}
            val proposed=layout-landing.cell
            if(!HouseRoom.connected(proposed)){mes("Removing that room would cut off part of the house.");return}
            if(choice2("Remove the connecting room and its furniture?",true,"Keep the room.",false))changeLayout(this,home,proposed,0,room)
        }
    }

    internal suspend fun climb(access:ProtectedAccess,target:BoundLocInfo, requestedDirection:Int=0) {
        with(access){
            val visit=visitOf(player)
            val owner=visit?.host ?: player
            val home=visit?.home ?: homes[owner] ?: return
            if(registry[coords]!==home.region || registry[target.coords]!==home.region)return
            val layout=layouts.layout(owner)
            val room=layout[home.cell(target.coords)]?:return
            val direction=room.stairDirection
            if(direction==0 || requestedDirection!=0 && direction!=requestedDirection){
                mes("The staircase doesn't lead that way.")
                return
            }
            val destination=room.stairLanding(layout)
            if(destination!=null){
                telejump(home.stairLanding(destination))
                return
            }
            val next=room.cell+256*direction
            if(visit!=null || !home.building || next in layout || room.floor+direction !in -1..1){
                mes("There is no connecting staircase.")
                return
            }
            val rooms=if(direction<0)listOf(HouseRoomType.SkillHall,HouseRoomType.QuestHall,HouseRoomType.DungeonStairs)
                else listOf(HouseRoomType.SkillHall,HouseRoomType.QuestHall)
            val type=rooms.getOrNull(menu("Build a connecting room",true,rooms.map { "${it.label} (${it.fee} coins)" }))?:return
            if(stat(stats.construction)<type.level){mes("You need a Construction level of ${type.level} to build that room.");return}
            if(layout.size>=roomLimit(statBase(stats.construction))){mes("You cannot add any more rooms at your current level.");return}
            val tier=room.variant(1)
            val recipe=furniture.recipes(room.type,1)[tier-1]
            val sawBoost=stat(stats.construction)<recipe.level && SawItems.saw in inv && charges()>0
            if(stat(stats.construction)+(if(sawBoost)3 else 0)<recipe.level){
                mes("You need a Construction level of ${recipe.level} to build the matching staircase.");return
            }
            if(listOf(objs.hammer,objs.imcando_hammer,objs.imcando_hammer_offhand).none { it in inv || it in worn } || StashObjs.saws.none{it in inv || it in worn}){
                mes("You need a hammer and saw to build the matching staircase.");return
            }
            if(invCoinTotal()<type.fee){mes("You need ${type.fee} coins to build that room.");return}
            val fromSack=mutableMapOf<Int,Int>()
            for((id,count) in recipe.materials){
                val item=objTypes.getValue(id)
                val missing=(count-invTotal(inv,item)).coerceAtLeast(0)
                if(ContractRewards.sackCount(this,item)<missing){
                    mes("To build the matching staircase you need "+recipe.materials.joinToString { (obj,amount)->"$amount ${objTypes.getValue(obj).name.lowercase()}" }+".")
                    return
                }
                if(missing>0)fromSack[id]=missing
            }
            val landing=HouseRoom(next,type,room.rotation).withStairs(tier,-direction)
            val proposed=layout+(next to landing)
            changeLayout(this,home,proposed,0,landing){
                val result=player.invTransaction(inv){
                    val inv=select(inv)
                    delete(inv,objs.coins.id,type.fee)
                    for((id,count)in recipe.materials){
                        val loose=count-fromSack.getOrDefault(id,0)
                        if(loose>0)delete(inv,id,loose)
                    }
                }
                if(result.success){
                    for((id,count)in fromSack)ContractRewards.adjustSack(this,objTypes.getValue(id),-count)
                    if(sawBoost)useCharge()
                    statAdvance(stats.construction,recipe.xp*ContractRewards.outfitModifier(this))
                }
                result.success
            }
        }
    }

    private fun roomLimit(level:Int):Int=if(level>=99)38 else if(level>=96)37 else 24+(level-20).coerceAtLeast(0)/6

    private fun changeLayout(
        access: ProtectedAccess,
        old: Home,
        layout: Map<Int, HouseRoom>,
        fee: Int,
        landing:HouseRoom? = null,
        pay: (() -> Boolean)? = null,
    ) {
        if (homes[access.player] !== old || registry[access.coords] !== old.region) return
        val home = allocate(layout, true, old.style)
        if (home == null) {
            access.mes("There is no house space available right now.")
            return
        }
        if (pay!=null && !pay()) return
        if (fee > 0 && !access.invTakeFee(fee)) return
        layouts.replace(access.player, layout)
        enter(access.player, home)
        access.telejump(landing?.let(home::stairLanding) ?: home.entrance)
        access.mes("You update your house.")
    }

    public fun changeMode(access: ProtectedAccess, building: Boolean) {
        if(building && hasGuests(access.player)){
            access.mes("Expel your guests before switching to building mode.")
            return
        }
        val home = homes[access.player] ?: return
        if (registry[access.coords] !== home.region || home.building == building) return
        prepare(access.player, building)?.let(access::telejump)
    }

    internal fun roomAt(player: Player, tile: CoordGrid, building: Boolean = false): HouseRoom? {
        val home = homes[player] ?: return null
        if (registry[tile] !== home.region || (building && !home.building)) return null
        return layouts.layout(player)[home.cell(tile)]
    }

    private fun visitOf(player:Player):Visit? {
        val visit=visits[player]?:return null
        if(registry[player.coords]===visit.home.region)return visit
        visits.remove(player)
        return null
    }
    private fun hasGuests(host:Player)=visits.keys.toList().any{visitOf(it)?.host===host}
    internal fun publicRoom(player:Player,tile:CoordGrid):HouseRoom? {
        val visit=visitOf(player)
        return if(visit==null)roomAt(player,tile)
        else if(registry[tile]===visit.home.region)layouts.layout(visit.host)[visit.home.cell(tile)] else null
    }
    internal fun telescopeBlocked(player:Player,loc:BoundLocInfo):Boolean {
        val study=publicRoom(player,loc.coords)?:return false
        if(study.type!=HouseRoomType.Study)return false
        if(study.floor<0)return true
        val owner=visitOf(player)?.host?:player
        // In the anchor study the telescope sits on the north edge, native loc angle 2.
        val direction=(loc.angleId+3) and 3
        val adjacent=layouts.layout(owner)[study.neighbour(direction)]?:return false
        return adjacent.type !in setOf(HouseRoomType.Garden, HouseRoomType.SuperiorGarden, HouseRoomType.Menagerie)
    }

    internal fun visit(a:ProtectedAccess,name:String,site:HouseLocation)=with(a){
        val playerName=name.trim().replace('_',' ')
        val host=online.firstOrNull{it.displayName.replace('_',' ').equals(playerName,true) || it.username.replace('_',' ').equals(playerName,true)}
        if(host==null || !player.members || !host.members || host.vars[varbits.poh_house_location]!=site.cacheValue){
            mes("That player has no available house at this portal.");return@with
        }
        if(host===player){prepare(player,false)?.let(::telejump);return@with}
        if(host.vars[HouseTravelBits.locked]!=0 || player.vars[HouseTravelBits.ironman]!=0 || homes[host]?.building==true){
            mes("You can't enter that house.");return@with
        }
        val destination=prepare(host,false)?:return@with
        val house=homes.getValue(host)
        visits[player]=Visit(host,house,site.outside)
        telejump(destination)
        presence.enter(player){guest->registry[guest.coords]===house.region}
    }

    internal fun expel(host:Player){
        for(guest in visits.keys.toList()){
            val visit=visits[guest]?:continue
            if(visit.host!==host)continue
            if(visitOf(guest)!=null){
                org.rsmod.game.entity.util.PathingEntityCommon.telejump(guest,collision,visit.outside)
                presence.leave(guest)
            }
            visits.remove(guest)
        }
    }

    internal fun toggleLock(a:ProtectedAccess)=with(a){
        if(roomAt(player,coords)==null)return@with
        vars[HouseTravelBits.locked]=1-vars[HouseTravelBits.locked]
        mes(if(vars[HouseTravelBits.locked]!=0)"You lock the portal." else "You unlock the portal.")
    }

    internal fun update(
        player: Player,
        room: HouseRoom,
        changed: HouseRoom,
        slot: Int = 0,
    ): Boolean {
        val home = homes[player] ?: return false
        val layout = layouts.layout(player)
        if (registry[player.coords] !== home.region || layout[room.cell] != room) return false
        layouts.replace(player, layout + (room.cell to changed))
        if (slot != 0) refresh(changed, home, slot)
        return true
    }

    public fun contains(access: ProtectedAccess): Boolean {
        val home = homes[access.player] ?: return false
        return registry[access.coords] === home.region
    }

    internal fun exitTo(access: ProtectedAccess, dest: CoordGrid) {
        val visiting=visitOf(access.player)!=null
        if (!visiting && !contains(access)) return
        access.teleport(dest)
        if(visiting){visits.remove(access.player);presence.leave(access.player)} else depart(access.player)
    }

    public fun leave(access: ProtectedAccess) {
        val visit=visitOf(access.player)
        if(visit!=null){
            access.telejump(visit.outside)
            visits.remove(access.player)
            presence.leave(access.player)
            return
        }
        val home = homes[access.player] ?: return
        if (registry[access.coords] !== home.region) return
        access.telejump(
            HouseLocation.fromCacheValue(access.vars[varbits.poh_house_location])?.outside
                ?: HouseLocation.Rimmington.outside
        )
        depart(access.player)
    }

    private fun depart(owner:Player){
        if(hasGuests(owner)){presence.leave(owner);return}
        clear(owner)
    }

    public fun logout(player: Player) {
        val visit=visitOf(player)
        if(visit!=null){players.relocateOnLogout(player,visit.outside);presence.leave(player)}
        visits.remove(player)
        expel(player)
        val home = homes[player] ?: return
        if (registry[player.coords] === home.region) {
            val outside =
                HouseLocation.fromCacheValue(player.vars[varbits.poh_house_location])?.outside
                    ?: HouseLocation.Rimmington.outside
            players.relocateOnLogout(player, outside)
        }
        clear(player)
    }

    private fun clear(player: Player) {
        menagerie.clear(player)
        petRocks.clear(player)
        homes.remove(player)
        presence.leave(player)
        VarPlayerIntMapSetter.set(player, varbits.poh_building_mode, 0)
    }

    public suspend fun operate(
        access: ProtectedAccess,
        target: BoundLocInfo,
        upgrade: Boolean = false,
    ) {
        with(access) {
            val home = homes[player] ?: return
            if (!home.building || registry[coords] !== home.region) return
            val cell = home.cell(target.coords)
            val layout = layouts.layout(player)
            val room = layout[cell] ?: return
            val loc =
                home.hotspots[cell]?.firstOrNull {
                    it.coords == target.coords && it.layer == target.layer
                } ?: return
            val slot = hotspot(room, loc) ?: return
            val stairs=slot==1 && room.type.stairHall
            val built = room.variant(slot)
            if (built != 0 && !upgrade) {
                if(room.type.isMenagerie && slot==3 && menagerie.hasFighters(player)){mes("Take your pets out of the arena first.");return}
                if(room.type.isMenagerie && slot==1 && menagerie.count(player)>0){mes("Take your pets out of the pet house first.");return}
                if (room.type == HouseRoomType.CostumeRoom && costumes.hasStored(player, slot)) {
                    mes("Empty the furniture before removing it.")
                    return
                }
                if(stairs && room.stairLanding(layout)!=null){mes("Remove the connected landing room first.");return}
                if (choice2("Remove this furniture?", true, "Keep it.", false)) {
                    if(stairs){
                        val empty=room.withStairs(0,0)
                        changeLayout(this,home,layout+(cell to empty),0,empty)
                        return
                    }
                    val returned = furniture.recipes(room.type, slot).getOrNull(built - 1)?.returnedOnRemove
                    if (returned != null && invAdd(inv,objTypes.getValue(returned)).failure) {
                        mes("Make room to take the furniture with you.")
                        return
                    }
                    val empty = room.furnish(slot, 0)
                    layouts.replace(player, layout + (cell to empty))
                    refresh(empty, home, slot)
                    mes("You remove the furniture.")
                }
                return
            }
            val choices =
                furniture.recipes(room.type, slot).withIndex().filter {
                    it.value.upgradeFrom == built
                }
            if (choices.isEmpty()) return
            val selected =
                menu(
                    "Build furniture",
                    true,
                    choices.map { it.value.name + " (level " + it.value.level + ")" },
                )
            val entry = choices.getOrNull(selected) ?: return
            val recipe = entry.value
            val direction = if(!stairs)0 else when(room.floor){
                -1 -> 1
                1 -> -1
                else -> choice2("Build stairs leading up.",1,"Build stairs leading down.",-1)
            }
            val habitat = room.type == HouseRoomType.Menagerie && slot == 6
            val sawBoost =
                !recipe.plant && !habitat &&
                    SawItems.saw in inv &&
                    charges() > 0 &&
                    stat(stats.construction) < recipe.level
            for ((skill, level) in recipe.requirements) {
                val boost = if (sawBoost && skill.id == stats.construction.id) 3 else 0
                if (stat(skill) + boost < level) {
                    mes("You need a ${skill.displayName} level of $level to build this.")
                    return
                }
            }
            val water =
                if (recipe.plant || (habitat && entry.index < 3)) (1..8).firstOrNull { GardenTools.wateringCans[it] in inv }
                else null
            if ((recipe.plant || (habitat && entry.index < 3)) && water == null) {
                mes("You need a watering can containing water.")
                return
            }
            if (
                !recipe.plant &&
                    (listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand).none {
                        it in inv || it in worn
                    } || StashObjs.saws.none { it in inv || it in worn })
            ) {
                mes("You need a hammer and saw to build furniture.")
                return
            }
            val materials = recipe.materials.toMutableList()
            val nailIndex = materials.indexOfFirst { it.first == objs.steel_nails.id }
            if (nailIndex >= 0) {
                val count = materials.removeAt(nailIndex).second
                val nail =
                    listOf(
                            objs.bronze_nails,
                            objs.iron_nails,
                            objs.steel_nails,
                            objs.black_nails,
                            objs.mithril_nails,
                            objs.adamant_nails,
                            objs.rune_nails,
                        )
                        .firstOrNull { invTotal(inv, it) >= count }
                if (nail == null) {
                    mes("You need $count nails to build this.")
                    return
                }
                materials += nail.id to count
            }
            if (
                materials.any { (id, count) ->
                    val item = objTypes.getValue(id)
                    invTotal(inv, item) + ContractRewards.sackCount(this, item) < count
                }
            ) {
                mes(
                    "To build this furniture you need " +
                        materials.joinToString { (id, count) ->
                            "$count ${objTypes[id]!!.name.lowercase()}"
                        } +
                        "."
                )
                return
            }
            anim(if (recipe.plant) GardenAnimations.water else seqs.human_hammer_hit)
            delay(if (recipe.plant || recipe.returnedOnRemove != null) 5 else 3)
            if (layouts.snapshot(player)[cell] != room || registry[coords] !== home.region) return
            val fromSack =
                materials
                    .mapNotNull { (id, count) ->
                        val item = objTypes.getValue(id)
                        val missing = (count - invTotal(inv, item)).coerceAtLeast(0)
                        if (missing > ContractRewards.sackCount(this, item)) return
                        if (missing == 0) null else id to missing
                    }
                    .toMap()
            fun pay():Boolean {
                val result=player.invTransaction(inv) {
                    val inventory = select(inv)
                    for ((id, count) in materials) {
                        val loose = count - fromSack.getOrDefault(id, 0)
                        if (loose > 0) delete(inventory, id, loose)
                    }
                    if (water != null) {
                        delete(inventory, GardenTools.wateringCans[water].id, 1)
                        add(inventory, GardenTools.wateringCans[water - 1].id, 1)
                    }
                }
                if(!result.success)return false
                for ((id, count) in fromSack) ContractRewards.adjustSack(
                    this,
                    objTypes.getValue(id),
                    -count,
                )
                if (sawBoost) useCharge()
                statAdvance(stats.construction, recipe.xp * ContractRewards.outfitModifier(this))
                return true
            }
            if(stairs){
                val changed=room.withStairs(entry.index+1,direction)
                changeLayout(this,home,layout+(cell to changed),0,changed,::pay)
                return
            }
            if(pay()){
                val changed = room.furnish(slot, entry.index + 1)
                layouts.replace(player, layout + (cell to changed))
                refresh(changed, home, slot)
                if (recipe.farmingXp != 0) statAdvance(stats.farming, recipe.farmingXp.toDouble())
                if (recipe.location == "poh_landscape_istafar_1")
                    player.completeDiaryTask(DiaryTask.BuildIsafdarPainting)
                mes("You build the ${recipe.name.lowercase()}.")
            }
        }
    }

    private fun hotspot(room: HouseRoom, loc: LocInfo): Int? {
        val name = locTypes[loc.id]?.internalName ?: return null
        val prefix = "poh_${room.type.hotspotPrefix}_"
        if(room.type.stairHall && (name=="poh_hall1_1_stairs_up" || name=="poh_hall1_1_stairs_top" || name=="poh_hall2_1_stairs_up" || name=="poh_hall2_1_stairs_top" || name=="poh_dungeon_stairs_1_stairs"))return 1
        if (!name.startsWith(prefix)) return null
        if (room.type == HouseRoomType.CostumeRoom) return CostumeFurniture.entries
            .firstOrNull { name == "poh_cos_room_${it.locPrefix}_hotspot" }?.slot
        if(room.type==HouseRoomType.ThroneRoom && name=="poh_throne_room_3_q")return 8
        if(room.type.isMenagerie)return when{
            name=="poh_menagerie_pethouse_hotspot"->1
            name=="poh_menagerie_scratchingpost_hotspot"->2
            name=="poh_menagerie_combatring_hotspot"||name=="poh_menagerie_combatring_mat_hotspot"->3
            name=="poh_menagerie_petlist_hotspot"->4
            name=="poh_menagerie_petfeeder_hotspot"->5
            room.type==HouseRoomType.Menagerie && (name=="poh_menagerie_habitat_feature" || name.startsWith("poh_menagerie_habitat_ground_"))->6
            else->null
        }
        if (room.type == HouseRoomType.SuperiorGarden) {
            return when (name.removePrefix(prefix)) {
                "treering" -> 1
                "topiary" -> 2
                "pool" -> 3
                else ->
                    when {
                        name.contains("_theme_") -> 4
                        name.contains("_fence_") -> 5
                        name.contains("_seating_a_") -> 6
                        name.contains("_seating_b_") -> 7
                        else -> null
                    }
            }
        }
        val slot =
            name.substring(prefix.length).takeWhile { it.isDigit() }.toIntOrNull() ?: return null
        return slot.takeUnless { room.cell == HouseRoom.ENTRANCE_CELL && it == 1 }
    }

    private fun refresh(room: HouseRoom, home: Home, slot: Int) {
        val selected = room.variant(slot)
        val recipes = furniture.recipes(room.type, slot)
        for (loc in home.hotspots[room.cell].orEmpty().filter { hotspot(room, it) == slot }) {
            val recipe = recipes.getOrNull(selected - 1)
            if(room.type.stairHall && slot==1 && locTypes[loc.id]!!.internalName.orEmpty().substringAfter("_1_") in setOf("middle","side","corner")){
                locs.del(loc,Int.MAX_VALUE)
                continue
            }
            if (recipe == null) {
                if (home.building && recipes.isNotEmpty()) locs.add(loc, Int.MAX_VALUE)
                else locs.del(loc, Int.MAX_VALUE)
                continue
            }
            val name =
                when {
                    room.type.stairHall && slot==1 -> if(!locTypes[loc.id]!!.internalName.orEmpty().contains("_stairs_") && locTypes[loc.id]!!.internalName!="poh_dungeon_stairs_1_stairs") null else if(room.stairDirection<0)recipe.location.replace("poh_stairs_","poh_stairstop_") else recipe.location
                    room.type == HouseRoomType.PortalChamber &&
                        slot in 1..3 &&
                        room.portal(slot) != 0 ->
                        recipe.location.removeSuffix("_empty") +
                            "_" +
                            HousePortals.destinations[room.portal(slot) - 1].suffix
                    room.type == HouseRoomType.SkillHall &&
                        slot == 2 &&
                        selected == 3 &&
                        room.variant(12) != 0 -> "poh_trophy_kalphitequeen_gilded"
                    else -> furniture.built(recipe, locTypes[loc.id]!!.internalName!!, selected)
                }
            if (name == null) locs.del(loc, Int.MAX_VALUE)
            else locs.change(loc, byName.getValue(name), Int.MAX_VALUE)
        }
    }

    private companion object {
        // Covers the four-tick Teleport to House animation and the subsequent zone update.
        const val ARRIVAL_RESERVATION_TICKS = 6
    }
}

internal object HouseButtons : ComponentReferences() {
    val expel = find("poh_options:expel_guests")
    val viewer = find("poh_options:viewer")
    val build = find("poh_options:build_mode_on")
    val normal = find("poh_options:build_mode_off")
    val leave = find("poh_options:leave_house")
}

public class StarterHouseScript
@Inject
internal constructor(
    private val furniture: HouseFurniture,
    private val home: StarterHouse,
    private val houses: PlayerOwnedHouseRepository,
    private val npcTypes: NpcTypeList,
    private val locTypes: LocTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        houses.onPrepareHouse(home::prepare)
        onEvent<org.rsmod.api.player.events.TelescopeObservation>{if(home.telescopeBlocked(player,telescope))blocked=true}
        onEvent<CultivatedSpiritTrees> { home.resolveSpiritTree(this) }
        onEvent<HouseFairyRingDestination> { destination = home.fairyRingDestination(player) }
        for (agent in
            npcTypes.values.filter {
                it.internalName in
                    setOf("poh_estate_agent", "prif_estate_agent", "fortis_estate_agent")
            }) {
            onOpNpc1(HashedNpcType(null, agent.internalName, agent.id)) {
                if (
                    statBase(stats.construction) >= 99 &&
                        choice2("Discuss my house.", false, "Buy a Construction cape.", true)
                ) {
                    SkillcapeSale.offer(this, stats.construction, objs.construction_hood)
                    return@onOpNpc1
                }
                if (vars[varbits.poh_house_location] != 0) {
                    when (choice3("Move my house.", 1, "Redecorate my house.", 2, "Leave.", 0)) {
                        1 -> offerRelocation()
                        2 -> redecorate(it.npc.coords)
                    }
                } else if (
                    choice2(
                        "Buy a house in Rimmington for 1,000 coins.",
                        true,
                        "No, thank you.",
                        false,
                    )
                ) {
                    home.purchase(this)
                }
            }
        }
        for (site in HouseSite.entries) {
            val portal = locTypes.values.single { it.internalName == site.portal }
            onOpLoc1(portal) {
                val mode =
                    choice4(
                        "Go to your house.",
                        1,
                        "Go to your house in building mode.",
                        2,
                        "Go to a friend's house.",3,
                        "Cancel.",
                        0,
                    )
                if(mode==3)home.visit(this,stringDialog("Whose house do you wish to visit?"),site.location)
                else if (mode != 0) enter(site, mode == 2)
            }
            onOpLoc2(portal) { enter(site, false) }
            onOpLoc3(portal) { enter(site, true) }
            onOpLoc4(portal) { home.visit(this,stringDialog("Whose house do you wish to visit?"),site.location) }
        }
        onOpLoc2(locTypes.values.single{it.internalName=="poh_exit_portal"}){home.toggleLock(this)}
        onOpLoc1(locTypes.values.single { it.internalName == "poh_exit_portal" }) {
            home.leave(this)
        }
        for (type in locTypes.values) {
            val name = type.internalName.orEmpty()
            if(name in setOf("poh_stairs_3","poh_stairstop_3","poh_stairs_4","poh_stairstop_4","poh_stairs_5","poh_stairstop_5","poh_spiralstairs","poh_spiralstairs_2")){
                onOpLoc1(type){home.climb(this,it.loc)}
                if(name.contains("spiral")){
                    onOpLoc2(type){home.climb(this,it.loc,1)}
                    onOpLoc3(type){home.climb(this,it.loc,-1)}
                }
                onOpLoc4(type){home.removeLanding(this,it.loc)}
                onOpLoc5(type){home.operate(this,it.loc)}
            }else if (name.startsWith("poh_hotspot_door")) {
                onOpLoc5(type) { home.buildRoom(this, it.loc) }
            } else if (
                name in furniture.builtNames ||
                    name.startsWith("poh_fairy_ring_last_") ||
                    name.startsWith("poh_spirit_ring_last_") ||
                    HouseRoomType.entries.any { name.startsWith("poh_${it.hotspotPrefix}_") }
            ) {
                onOpLoc5(type) { home.operate(this, it.loc) }
                if (type.op.getOrNull(3).equals("Upgrade", ignoreCase = true))
                    onOpLoc4(type) { home.operate(this, it.loc, true) }
            }
        }
        onIfModalButton(HouseButtons.viewer) { home.plan(this) }
        onIfModalButton(HouseButtons.build) { home.changeMode(this, true) }
        onIfModalButton(HouseButtons.normal) { home.changeMode(this, false) }
        onIfModalButton(HouseButtons.expel){if(home.contains(this))home.expel(player)}
        onIfModalButton(HouseButtons.leave) { home.leave(this) }
        onPlayerLogout { home.logout(player) }
    }

    private suspend fun ProtectedAccess.redecorate(agent: CoordGrid) {
        if (!player.members) return
        val styles = HouseStyle.entries
        val index =
            menu("Redecorate your house", true, styles.map { "${it.label} (${it.price} coins)" })
        val style = styles.getOrNull(index) ?: return
        val template = if (style.template < 0) index else style.template
        val needsPlans = style == HouseStyle.Hosidius && vars[ContractVars.hosidius] == 0
        if (needsPlans && ContractItems.plans !in inv) {
            mes("Bring Hosidius blueprints from Mahogany Homes to unlock this style.")
            return
        }
        if (vars[HouseTravelBits.style] == template) {
            mes("Your house already has that style.")
            return
        }
        if (statBase(stats.construction) < style.level) {
            mes("You need a Construction level of ${style.level} for that style.")
            return
        }
        if (
            !choice2(
                "Pay ${style.price} coins to redecorate in ${style.label.lowercase()}?",
                true,
                "Cancel.",
                false,
            )
        )
            return
        val payment =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, objs.coins.id, style.price)
                if (needsPlans) delete(bag, ContractItems.plans.id, 1)
            }
        if (payment.failure) {
            mes("You need ${style.price} coins for the redecoration.")
            return
        }
        if (needsPlans) vars[ContractVars.hosidius] = 1
        vars[HouseTravelBits.style] = template
        mes("Your house has been redecorated.")
        if (style == HouseStyle.Fancy) {
            when {
                agent.chebyshevDistance(CoordGrid(3240, 3476)) <= 8 ->
                    player.completeDiaryTask(DiaryTask.DecorateVarrockFancyStone)
                agent.chebyshevDistance(CoordGrid(2737, 3502)) <= 8 ->
                    player.completeDiaryTask(DiaryTask.DecorateSeersFancyStone)
            }
        }
    }

    private suspend fun ProtectedAccess.offerRelocation() {
        if (!player.members) return
        val current = vars[varbits.poh_house_location]
        val choices = HouseSite.entries.filterNot { it.location.cacheValue == current }
        val index =
            menu(
                "Move your house",
                true,
                choices.map { "${it.name}: ${it.fee} coins (level ${it.level})" },
            )
        val site = choices.getOrNull(index) ?: return
        if (statBase(stats.construction) < site.level) {
            mes("You need a Construction level of ${site.level} to move to ${site.name}.")
            return
        }
        if (
            (site == HouseSite.Hosidius && vars[HouseTravelBits.kourendVisited] == 0) ||
                (site == HouseSite.Aldarin && vars[HouseTravelBits.varlamoreVisited] == 0)
        ) {
            mes("You need to visit that region before you can move your house there.")
            return
        }
        if (site == HouseSite.Prifddinas && vars[varbits.song_of_the_elves] < 200) {
            mes("You need to complete Song of the Elves to move to Prifddinas.")
            return
        }
        if (!choice2("Move to ${site.name} for ${site.fee} coins.", true, "Cancel.", false)) return
        if (!invTakeFee(site.fee)) {
            mes("You need ${site.fee} coins to move your house.")
            return
        }
        VarPlayerIntMapSetter.set(player, varbits.poh_house_location, site.location.cacheValue)
        mes("Your house has been moved to ${site.name}.")
    }

    private fun ProtectedAccess.enter(site: HouseSite, building: Boolean) {
        val current = HouseLocation.fromCacheValue(vars[varbits.poh_house_location])
        if (current != site.location) {
            mes(
                if (current == null) "Speak to an estate agent to buy a house."
                else "Your house is in ${current.name}."
            )
            return
        }
        val destination = home.prepare(player, building)
        if (destination == null) {
            mes("Your house is not ready to enter.")
            return
        }
        telejump(destination)
        when (site) {
            HouseSite.Hosidius -> player.completeDiaryTask(DiaryTask.EnterHosidiusHouse)
            HouseSite.Yanille -> player.completeDiaryTask(DiaryTask.EnterYanilleHouse)
            else -> Unit
        }
    }
}
