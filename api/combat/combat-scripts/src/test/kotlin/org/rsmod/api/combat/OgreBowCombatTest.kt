package org.rsmod.api.combat

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rsmod.api.combat.scripts.PvNCombatScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.scripts.WeaponAttackTypesScript
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.testing.GameTestExtension
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@ExtendWith(GameTestExtension::class)
class OgreBowCombatTest {
    private object Shooting : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
            bind(AttackTypes::class.java).`in`(Scopes.SINGLETON)
        }
    }

    @Test
    fun GameTestState.`fifth npc option rejects the wrong gear and fires a brutal from the composite bow`() =
        runInjectedGameTest(AttackStyles::class,Shooting,PvNCombatScript::class,WeaponAttackStylesScript::class,WeaponAttackTypesScript::class) {
            advance(12)
            player.members=true
            player.observerUUID=player.uuid
            player.placeAt(CoordGrid(2592,2962))
            player.stats[stats.ranged]=99
            val bird=spawnNpc(player.coords.translate(3,0),npcTypes.getValue(1475))
            assertEquals("Attack",bird.visType.op[4])
            player.worn[3]=InvObj(objs.ogre_bow)
            player.worn[13]=InvObj(objs.rune_brutal,10)
            player.setVarp(varps.big_chompy_bird_hunting,65)
            player.opNpc5(bird);advance(2)
            assertEquals(10,player.worn[13]?.count)
            player.worn[3]=InvObj(objs.comp_ogre_bow)
            player.setVarp(varps.big_chompy_bird_hunting,64)
            player.opNpc5(bird);advance(2)
            assertEquals(10,player.worn[13]?.count)
            player.setVarp(varps.big_chompy_bird_hunting,65)
            player.opNpc5(bird);advance(2)
            assertEquals(9,player.worn[13]?.count, "Combat messages: ${client.mapOf(net.rsprot.protocol.game.outgoing.misc.player.MessageGame::message)}")
            assertTrue(player.actionDelay>player.currentMapClock)
            advance(6)
            assertTrue((player.worn[13]?.count ?: 0)<9)
        }
}
