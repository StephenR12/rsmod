package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class HousePoolsTest {
    data class Deps @Inject constructor(val house:StarterHouse,val rooms:HouseRoomRepository)

    @Test fun GameTestState.`fancy and ornate pools restore their own effects and preserve skill boosts`() =
        runInjectedGameTest(Deps::class,null,StarterHouseScript::class,HousePools::class){d->
            player.members=true
            advance(15)
            player.setVarBit(varbits.poh_house_location,1)
            val cell=HouseRoom.ENTRANCE_CELL+1
            val hotspot=locTypes.getValue(29122)
            placeMapLoc(CoordGrid(1902,5696),hotspot,LocShape[10],LocAngle[0])
            fun drink(tier:Int){
                d.rooms.replace(player,d.rooms.layout(player)+(cell to HouseRoom(cell,HouseRoomType.SuperiorGarden).furnish(3,tier)))
                val entry=checkNotNull(d.house.prepare(player,false))
                val tile=entry.translate(10,-2)
                val type=locTypes.getValue(29236+tier)
                val loc=BoundLocInfo(checkNotNull(findLoc(tile,type)),type)
                player.teleport(tile.translate(-1,0))
                player.opLoc1(loc);advance(5)
            }
            player.setBaseLevel(stats.magic,60);player.setCurrentLevel(stats.magic,35)
            player.setBaseLevel(stats.attack,60);player.setCurrentLevel(stats.attack,66)
            player.setBaseLevel(stats.hitpoints,60);player.setCurrentLevel(stats.hitpoints,21)
            player.setBaseLevel(stats.prayer,65);player.setCurrentLevel(stats.prayer,12)
            player.setVarBit(varbits.prayer_drain_counter,5)
            player.withProtectedAccess{vars[varps.sa_energy]=0;vars[varps.poison]=1024;vars[varps.disease]=5;vars[varps.surge_potion_cooldown_cycles]=50}
            player.runEnergy=1350
            drink(4)
            assertEquals(60,player.stat(stats.magic))
            assertEquals(66,player.stat(stats.attack))
            assertEquals(65,player.stat(stats.prayer))
            assertEquals(0,player.vars[varbits.prayer_drain_counter])
            assertEquals(1000,player.vars[varps.sa_energy])
            assertEquals(10000,player.runEnergy)
            assertEquals(21,player.stat(stats.hitpoints))
            assertEquals(1024,player.vars[varps.poison])
            player.withProtectedAccess{d.house.leave(this)}
            drink(5)
            assertEquals(60,player.stat(stats.hitpoints))
            assertEquals(0,player.vars[varps.poison])
            assertEquals(0,player.vars[varps.disease])
            assertEquals(0,player.vars[varps.surge_potion_cooldown_cycles])
        }
}
