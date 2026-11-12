package org.rsmod.api.combat.manager

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Player

class NextMeleeAttackRepositoryTest {
    @Test
    fun `minimum damage and guaranteed accuracy merge and are consumed once`() {
        val player = Player()
        val repository = NextMeleeAttackRepository()

        repository.setMinimumDamage(player, damage = 6, duration = 100)
        repository.guarantee(player, duration = 10)

        val effect = repository.consume(player)
        assertEquals(6, effect?.minimumDamage)
        assertTrue(effect?.guaranteed == true)
        assertNull(repository.consume(player))
    }

    @Test
    fun `expired modifier is discarded`() {
        val player = Player()
        val repository = NextMeleeAttackRepository()

        repository.guarantee(player, duration = 10)
        player.currentMapClock += 10

        assertNull(repository.consume(player))
    }

    @Test
    fun `rearming keeps strongest minimum and longest expiry`() {
        val player = Player()
        val repository = NextMeleeAttackRepository()

        repository.setMinimumDamage(player, damage = 10, duration = 20)
        repository.setMinimumDamage(player, damage = 4, duration = 40)

        val effect = repository.consume(player)
        assertEquals(10, effect?.minimumDamage)
        assertEquals(player.currentMapClock + 40, effect?.expiresAt)
        assertFalse(effect?.guaranteed ?: true)
    }
}
