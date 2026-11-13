package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.TelescopeObservation
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.magic.spell.utility.standard.PlayerOwnedHousePresenceRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class HouseGuestTest {
    data class Deps @Inject constructor(val homes:StarterHouse,val layouts:HouseRoomRepository,val presence:PlayerOwnedHousePresenceRepository)

    @Test fun GameTestState.`visitors and hosts see the same telescope obstruction and only the host controls entry`() =
        runInjectedGameTest(Deps::class,null,StarterHouseScript::class){d->
            player.members=true
            player.displayName="Host"
            player.setVarBit(varbits.poh_house_location,1)
            val cell=HouseRoom.ENTRANCE_CELL+1
            val study=HouseRoom(cell,HouseRoomType.Study).furnish(6,3)
            val hotspot=locTypes.getValue(15424)
            placeMapLoc(CoordGrid(1893,5743),hotspot,LocShape[10],LocAngle[2])
            val nearRoom=study.neighbour(1)
            d.layouts.replace(player,d.layouts.layout(player)+mapOf(cell to study,nearRoom to HouseRoom(nearRoom,HouseRoomType.Kitchen)))
            placeMapLoc(CoordGrid(1859,5707),locTypes.getValue(15361),LocShape[10],LocAngle[0])
            val entry=checkNotNull(d.homes.prepare(player,false))
            val tile=entry.translate(9,5)
            val type=locTypes.getValue(13658)
            val scope=BoundLocInfo(checkNotNull(findLoc(tile,type)),type)
            val rimmington=org.rsmod.content.skills.magic.spell.utility.standard.HouseLocation.Rimmington
            val guest=registerPlayer(rimmington.outside).apply{members=true;displayName="Visitor"}
            fun observe(who:Player)=TelescopeObservation(who,scope).also{eventBus.publish(it)}.blocked
            val outside=placeMapLoc(rimmington.outside.translate(0,1),locTypes.getValue(15478),LocShape[10],LocAngle[0])
            val exit=BoundLocInfo(checkNotNull(findLoc(entry.translate(-1,1),locTypes.getValue(4525))),locTypes.getValue(4525))
            fun visit(){
                guest.opLoc4(outside);advance(3)
                guest.resumeActiveCoroutine(org.rsmod.api.player.input.ResumePStringDialogInput("Host"));advance(2)
            }
            player.teleport(entry)
            player.opLoc2(exit);advance(3)
            assertEquals(1,player.vars[cacheTypes.varbits.getValue(2183)])
            visit();assertFalse(d.presence.isInside(guest))
            player.opLoc2(exit);advance(3)
            visit();assertTrue(d.presence.isInside(guest));assertTrue(observe(guest));assertTrue(observe(player))
            guest.opLoc2(exit);advance(3)
            player.withProtectedAccess{d.homes.changeMode(this,true)}
            assertEquals(0,player.vars[varbits.poh_building_mode])
            player.withProtectedAccess{d.homes.leave(this)}
            assertTrue(d.presence.isInside(guest))
            assertTrue(observe(guest))
            d.layouts.replace(player,d.layouts.layout(player)+(nearRoom to HouseRoom(nearRoom,HouseRoomType.Garden)))
            assertFalse(observe(guest))
            player.teleport(checkNotNull(d.homes.prepare(player,false)))
            player.withProtectedAccess{ifOpenMainModal(cacheTypes.interfaces.values.single{it.internalName=="poh_options"})}
            player.ifButton(cacheTypes.components.values.single{it.internalName=="poh_options:expel_guests"});advance(2)
            assertEquals(rimmington.outside,guest.coords)
            assertFalse(d.presence.isInside(guest))
            visit();assertTrue(d.presence.isInside(guest))
            d.homes.logout(player)
            assertEquals(rimmington.outside,guest.coords)
        }
}
