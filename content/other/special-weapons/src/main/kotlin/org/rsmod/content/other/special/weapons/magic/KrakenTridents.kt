package org.rsmod.content.other.special.weapons.magic

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.events.PlayerCombatPermission
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.weapons.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

internal object TridentObjs : ObjReferences() {
    val full = find("tots")
    val fang = find("magic_fang")
    val tentacle = find("kraken_tentacle")
    val notedTentacle = find("cert_kraken_tentacle")
    val seas = Trident(find("tots_charged"), find("tots_uncharged"), false, false)
    val swamp = Trident(find("toxic_tots_charged"), find("toxic_tots_uncharged"), true, false)
    val enhanced = Trident(find("tots_i_charged"), find("tots_i_uncharged"), false, true)
    val enhancedSwamp =
        Trident(find("toxic_tots_i_charged"), find("toxic_tots_i_uncharged"), true, true)
    val variants = listOf(seas, swamp, enhanced, enhancedSwamp)

    fun type(id: Int): Trident? =
        if (id == full.id) seas else variants.find { it.charged.id == id || it.empty.id == id }
}

internal data class Trident(
    val charged: ObjType,
    val empty: ObjType,
    val toxic: Boolean,
    val extended: Boolean,
) {
    val cap = if (extended) 20000 else 2500
    val level = if (toxic) 78 else 75

    fun charges(item: InvObj) =
        when (item.id) {
            TridentObjs.full.id -> 2500
            charged.id -> item.vars.coerceIn(0, cap)
            else -> 0
        }

    fun item(charges: Int): InvObj =
        when {
            charges <= 0 -> InvObj(empty)
            this == TridentObjs.seas && charges >= cap -> InvObj(TridentObjs.full)
            else -> InvObj(charged, 1, charges.coerceAtMost(cap))
        }
}

internal object TridentEffects : SpotanimReferences() {
    val casting = find("slayer_tots_casting")
    val travel = find("slayer_tots_projectile")
    val splash = find("slayer_tots_impact")
    val toxicCasting = find("toxic_tots_casting")
    val toxicTravel = find("toxic_tots_projectile")
    val toxicSplash = find("toxic_tots_impact")
}

class KrakenTridents @Inject constructor() : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val attack = TridentAttack(manager)
        register(TridentObjs.full, attack)
        for (type in TridentObjs.variants) {
            register(type.charged, attack)
            register(type.empty, attack)
        }
    }

    private class TridentAttack(private val manager: WeaponAttackManager) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            cast(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            val venue = PlayerCombatPermission(player, target)
            publish(venue)
            if (venue.allowed == true) cast(target, attack)
            else {
                manager.stopCombat(this)
                mes(
                    "The trident's built-in spell can only be used against players in safe minigames."
                )
            }
            return true
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Staff) {
            val item = player.righthand ?: return
            val trident = TridentObjs.type(item.id) ?: return
            val quantity = trident.charges(item)
            if (quantity == 0) {
                manager.stopCombat(this)
                mes("Your trident is uncharged.")
                return
            }
            val base = (player.magicLvl / 3 - 5).coerceAtLeast(1) + (if (trident.toxic) 3 else 0)
            val effects = if (trident.toxic) StaffEffects.swamp else StaffEffects.seas
            castPoweredStaff(this, manager, target, attack, base, effects)
            // The queued hit captures the charged weapon before the last cast empties it.
            player.righthand = trident.item(quantity - 1)
            if (quantity == 1) {
                manager.stopCombat(this)
                mes("Your trident has run out of charges.")
            } else manager.continueCombat(this, target)
        }
    }
}
