package org.rsmod.content.skills.magic.spell.utility.arena

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.interact.NpcTInteractions
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.runes.combo.*
import org.rsmod.api.spells.runes.compact.*
import org.rsmod.api.spells.runes.fake.*
import org.rsmod.api.spells.runes.staves.*
import org.rsmod.api.spells.runes.subs.*
import org.rsmod.api.spells.runes.unlimited.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.magic.spell.utility.standard.*
import org.rsmod.content.other.coalbag.CoalBagModule
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.Component
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class MageTrainingArenaTest {
    @Test fun GameTestState.`alchemy accepts only the cupboard currency and pays banked coins and experience`() = game { _ ->
        val door=placeMapLoc(CoordGrid(3363,3321),locTypes.getValue(23675))
        player.teleport(CoordGrid(3363,3320))
        player.inv[0]=InvObj(objs.coins,5)
        player.opLoc1(door);advance(2)
        assertEquals(CoordGrid(3363,3320),player.coords)
        player.clearInv()
        player.opLoc1(door);advance(2)
        assertEquals(CoordGrid(3362,9624,2),player.coords)
        player.inv[0]=InvObj(objs.nature_rune,10)
        player.inv[1]=InvObj(objs.fire_rune,50)
        // At most one of the six cupboards is empty during a rotation.
        for((i,z) in listOf(9632,9636).withIndex()){
            val c=CoordGrid(3360,z,2)
            player.teleport(c.translate(1,0))
            player.opLoc2(placeMapLoc(c,locTypes.getValue(23679+i*2),angle=LocAngle.North));advance(2)
        }
        val goods=player.inv.indices.filter { player.inv[it]?.id in 6893..6897 }
        assertTrue(goods.size>=5)
        castOnSlot(components.magic_spellbook_low_alchemy,objs.spell_low_alchemy,goods.first())
        val value=player.count(objTypes.getValue(8890))
        assertTrue(value in listOf(1,5,8,15,30))
        assertEquals(0,player.count(objs.coins))
        val collector=placeMapLoc(CoordGrid(3364,9646,2),locTypes.getValue(10734),angle=LocAngle.South)
        player.teleport(CoordGrid(3364,9645,2))
        player.inv[26]=InvObj(objs.coins,100)
        val before=player.statMap.getFineXP(stats.magic)
        player.opLoc1(collector);advance(2)
        assertEquals(value*20,player.statMap.getFineXP(stats.magic)-before)
        assertEquals(0,player.count(objs.coins));assertEquals(0,player.count(objTypes.getValue(8890)))
        player.withProtectedAccess { assertEquals(value/10,bank.count(objs.coins)) }
    }

    @Test fun GameTestState.`shapes and dragonstones award their points only inside the enchanting chamber`() = game { _ ->
        val c=CoordGrid(3360,3318)
        val door=placeMapLoc(c,locTypes.getValue(23674),angle=LocAngle.South)
        player.teleport(c.translate(1,0))
        player.opLoc1(door);advance(2)
        assertEquals(CoordGrid(3354,9640),player.coords)
        val pile=CoordGrid(3345,9654)
        player.teleport(pile.translate(0,-1))
        random.next=0
        player.opLoc1(placeMapLoc(pile,locTypes.getValue(23694),angle=LocAngle.South));advance(2)
        val slot=player.inv.indexOfFirst { it?.id==6903 }
        assertTrue(slot>=0)
        player.inv[23]=InvObj(objs.cosmic_rune,3);player.inv[24]=InvObj(objs.water_rune,4)
        castOnSlot(components.magic_spellbook_enchant_1,objs.spell_enchant_lvl_1,slot)
        assertEquals(2,player.vars[cacheTypes.varps.getValue(65698)])
        assertEquals(1,player.count(objTypes.getValue(6902)))
        val hole=CoordGrid(3363,9640)
        player.teleport(hole.translate(1,0))
        val xp=player.statMap.getFineXP(stats.magic)
        player.opLoc1(placeMapLoc(hole,locTypes.getValue(23698)));advance(3)
        assertEquals(200,player.statMap.getFineXP(stats.magic)-xp)
        assertEquals(0,player.count(objTypes.getValue(6902)))
        player.inv[0]=InvObj(objTypes.getValue(6900))
        player.teleport(CoordGrid(3363,3317));advance(2)
        player.inv[0]=InvObj(objTypes.getValue(6900))
        castOnSlot(components.magic_spellbook_enchant_1,objs.spell_enchant_lvl_1,0)
        assertEquals(2,player.count(objs.cosmic_rune));assertEquals(0,player.count(objTypes.getValue(6902)))
    }

    @Test fun GameTestState.`all four currencies buy Bones to Peaches once and its palace cast completes the diary`() = game { d ->
        val guardian=spawnNpc(CoordGrid(3363,3318,1),npcTypes.getValue(5984))
        player.teleport(CoordGrid(3363,3317,1))
        val funds=listOf(200,300,2000,199)
        for((i,n) in funds.withIndex())player.setVarp(cacheTypes.varps.getValue(65696+i),n)
        player.opNpc4(guardian);advance(2)
        val shop=cacheTypes.interfaces.getValue(197)
        assertTrue(player.ui.containsModal(shop))
        fun button(id:Int,child:Int?=null){player.ifButton(cacheTypes.components.getValue((197 shl 16) or id),comsub=child);advance()}
        button(11,10);button(9)
        assertEquals(0,player.vars[varbits.magictraining_bonespeaches])
        assertEquals(200,player.vars[cacheTypes.varps.getValue(65696)])
        player.setVarp(cacheTypes.varps.getValue(65699),200);button(9)
        assertEquals(1,player.vars[varbits.magictraining_bonespeaches])
        for(i in 0..3)assertEquals(0,player.vars[cacheTypes.varps.getValue(65696+i)])
        player.ifClose();advance()
        player.teleport(CoordGrid(3290,3168))
        player.inv[0]=InvObj(objs.bones);player.inv[1]=InvObj(objs.nature_rune,2)
        player.inv[2]=InvObj(objs.water_rune,4);player.inv[3]=InvObj(objs.earth_rune,4)
        player.withProtectedAccess {d.bones.cast(this,objs.spell_bones_to_peaches)}
        assertEquals(1,player.count(objs.peach))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.PeachesAlKharidPalace))
    }

    @Test fun GameTestState.`graveyard blocks teleportation and converts deposited fruit into points and deferred runes`() = game { d ->
        val gate=CoordGrid(3366,3318)
        player.teleport(gate.translate(-1,0))
        player.opLoc1(placeMapLoc(gate,locTypes.getValue(23676),angle=LocAngle.North));advance(2)
        assertEquals(CoordGrid(3363,9641,1),player.coords)
        player.inv[0]=InvObj(objs.law_rune);player.inv[1]=InvObj(objs.air_rune,3);player.inv[2]=InvObj(objs.fire_rune)
        player.withProtectedAccess { d.teleports.cast(this,objs.spell_varrock_teleport) }
        advance(5);assertEquals(CoordGrid(3363,9641,1),player.coords);assertEquals(1,player.count(objs.law_rune))
        val pile=CoordGrid(3374,9650,1)
        player.teleport(pile.translate(0,-1))
        val loc=placeMapLoc(pile,locTypes.getValue(10728))
        repeat(4){ player.opLoc1(loc);advance() }
        assertEquals(4,player.count(objTypes.getValue(6907)))
        player.inv[23]=InvObj(objs.nature_rune);player.inv[24]=InvObj(objs.water_rune,2);player.inv[25]=InvObj(objs.earth_rune,2)
        player.withProtectedAccess { d.bones.cast(this,objs.spell_bones_to_bananas) }
        assertEquals(16,player.count(objs.banana))
        val chute=CoordGrid(3365,9648,1)
        player.teleport(chute.translate(-1,0))
        player.opLoc1(placeMapLoc(chute,locTypes.getValue(10735),angle=LocAngle.South));advance(2)
        assertEquals(1,player.vars[cacheTypes.varps.getValue(65699)])
        assertEquals(0,player.count(objs.banana))
        val exit=CoordGrid(3363,9640,1)
        player.teleport(exit.translate(-1,0));player.opLoc1(placeMapLoc(exit,locTypes.getValue(23677)));advance(2)
        assertEquals(CoordGrid(3363,3317),player.coords)
        assertEquals(1,listOf(objs.water_rune,objs.earth_rune,objs.nature_rune,objs.death_rune,objs.blood_rune).sumOf {player.count(it)})
    }

    @Test fun GameTestState.`telekinetic grab slides a private guardian against walls and pays the fifth maze bonus`() = game { d ->
        val entry=CoordGrid(3363,3315)
        player.teleport(entry.translate(0,1))
        random.next=0
        player.setVarp(cacheTypes.varps.getValue(65694),4)
        player.opLoc1(placeMapLoc(entry,locTypes.getValue(23673),angle=LocAngle.East));advance(3)
        val corner=player.coords.translate(1,-4)
        fun guardian(id:Int)=npcRepo.findAll(ZoneKey.from(player.coords),4).single { it.id==id }
        val statue=guardian(6777)
        assertEquals(corner.translate(9,0),statue.coords)
        assertNotNull(guardian(5979))
        player.inv[0]=InvObj(objs.law_rune,15);player.inv[1]=InvObj(objs.air_rune,15)
        val path=listOf(9 to 9,6 to 9,6 to 1,3 to 1,3 to 9,2 to 9,2 to 0,0 to 0,0 to 9)
        for((i,expected) in path.withIndex()){
            val side=when("NWSWNWSWN"[i]){'N'->4 to 10; 'S'->4 to -1;'W'->-1 to 4;else->10 to 4}
            player.teleport(corner.translate(side.first,side.second));advance()
            d.target.interact(player,statue,components.magic_spellbook_telegrab,-1,objs.spell_tele_grab);advance(4)
            if(i<path.lastIndex)assertEquals(corner.translate(expected.first,expected.second),statue.coords,"slide $i")
        }
        assertEquals(corner.translate(0,9),guardian(6779).coords)
        assertEquals(10,player.vars[cacheTypes.varps.getValue(65696)])
        assertEquals(16,player.count(objs.law_rune))
        assertEquals(6,player.count(objs.air_rune))
        assertEquals(0,player.vars[cacheTypes.varps.getValue(65694)])
    }

    private fun GameTestScope.castOnSlot(component:ComponentType,spell:ObjType,slot:Int){
        val target=player.inv[slot]!!
        eventBus.publish(IfOverlayButtonT(player,-1,objTypes[spell],slot,objTypes[target],Component(component.packed),Component(components.inv_items.packed)))
    }
    private fun GameTestState.game(body:GameTestScope.(Deps)->Unit)=runInjectedGameTest(Deps::class,Module,
        MageTrainingArena::class,TelekineticTheatreScript::class,ArenaRewards::class,AlchemySpellScript::class,JewelleryEnchantSpellScript::class,
        ComboRuneScript::class,CompactRuneScript::class,FakeRuneScript::class,StaffSubstituteScript::class,RuneSubstituteScript::class,UnlimitedRuneScript::class){d->
        player.members=true;player.setCurrentLevel(stats.magic,99);player.observerUUID=player.uuid
        body(d)
    }
    class Deps @Inject constructor(val bones:BoneConversionSpells,val target:NpcTInteractions,val teleports:StandardTeleportSpells)
    private object Module:AbstractModule(){
        override fun configure(){install(MagicSpellModule());install(CoalBagModule())
            for(type in listOf(ComboRuneRepository::class.java,CompactRuneRepository::class.java,FakeRuneRepository::class.java,StaffSubstituteRepository::class.java,
                RuneSubstituteRepository::class.java,UnlimitedRuneRepository::class.java,UtilitySpellCooldownRepository::class.java))bind(type).`in`(Scopes.SINGLETON)
        }
    }
}
