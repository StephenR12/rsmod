package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.EnduranceRing
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.ring
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.magic.spell.utility.standard.PotionProtectionScript
import org.rsmod.content.skills.magic.spell.utility.standard.StaminaEffectRepository
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class SepulchreRelicsTest {
    class Deps
    @Inject
    constructor(val items: HeldUInteractions, val stamina: StaminaEffectRepository)

    @Test
    fun GameTestState
        .`stamina mix and potion doses charge the wearable ring and extend one successful drink`() =
        runInjectedGameTest(
            Deps::class,
            null,
            RingOfEndurance::class,
            PotionProtectionScript::class,
        ) { d ->
            player.members = true
            player.setBaseLevel(stats.agility, 70)
            player.setCurrentLevel(stats.agility, 70)
            fun use() {
                player.withProtectedAccess {
                    d.items.interact(this, inv, objTypes[inv[0]!!], 0, objTypes[inv[1]!!], 1)
                }
            }
            player.inv[0] = InvObj(objTypes.getValue(24844))
            player.inv[1] = InvObj(objs.stamina_mix_2)
            use()
            assertEquals(2, EnduranceRing.charges(player.inv[0]))
            assertEquals(objs.vial_empty.id, player.inv[1]?.id)
            player.inv[1] = InvObj(objs.stamina_potion_4)
            use()
            assertEquals(6, EnduranceRing.charges(player.inv[0]))
            player.withProtectedAccess { opHeld2(0) }
            assertEquals(6, EnduranceRing.charges(player.ring))
            player.runEnergy = 1000
            eventBus.publish(
                PlayerConsumableEvent.Consumed(player, objs.stamina_mix_2, objs.stamina_potion_2)
            )
            assertEquals(5000, player.runEnergy)
            assertEquals(5, EnduranceRing.charges(player.ring))
            assertEquals(400, d.stamina.remainingCycles(player))
            assertEquals(20, player.vars[varbits.stamina_duration])
            player.ring = null
            d.stamina.tick(player)
            assertEquals(200, d.stamina.remainingCycles(player))
            assertEquals(20, player.vars[varbits.stamina_duration])
        }

    @Test
    fun GameTestState
        .`investigating the skeleton and inserting a recovered floor page survives losing the tome`() =
        runInjectedGameTest(HeldUInteractions::class, null, SepulchreRelics::class) { items ->
            player.members = true
            val tile = CoordGrid(2396, 5979)
            val skeleton = placeMapLoc(tile, locTypes.getValue(38479), LocShape[10], LocAngle[2])
            player.teleport(tile.translate(-1, 0))
            player.opLoc1(skeleton)
            advance(3)
            val tome = objTypes.getValue(24761)
            val page = objTypes.getValue(24763)
            assertEquals(1, player.inv.count(tome))
            val slot = player.inv.indexOfFirst { it?.id == tome.id }
            player.inv[4] = InvObj(page)
            player.withProtectedAccess { items.interact(this, inv, page, 4, tome, slot) }
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(10418)])
            assertEquals(0, player.inv.count(page))
            player.inv[slot] = null
            player.opLoc1(skeleton)
            advance(3)
            assertEquals(1, player.inv.count(tome))
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(10418)])
        }
}
