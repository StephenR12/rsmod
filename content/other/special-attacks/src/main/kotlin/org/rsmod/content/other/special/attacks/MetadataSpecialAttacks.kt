package org.rsmod.content.other.special.attacks

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.NextMeleeAttackRepository
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.PlayerCombatEvents
import org.rsmod.api.player.magic.StaffOfTheDeadStateRepository
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statRestore
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MagicSpecialAttack
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.api.specials.combat.ShieldSpecialAttack
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType

/**
 * Registers every cache-advertised special which does not have a bespoke implementation.
 *
 * Cache descriptions are the canonical variant mapping: ornamented, poisoned, PvP and temporary
 * game-mode variants therefore share the same effect without maintaining parallel id lists. The
 * bespoke maps run first and are never replaced by this fallback.
 */
class MetadataSpecialAttacks
@Inject
constructor(
    private val weapons: SpecialAttackWeapons,
    private val objTypes: ObjTypeList,
    private val ammunition: RangedAmmoManager,
    private val staffProtection: StaffOfTheDeadStateRepository,
    private val nextMeleeAttack: NextMeleeAttackRepository,
) : SpecialAttackMap {
    override val priority: Int = Int.MAX_VALUE

    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (definition in weapons.definitions()) {
            val obj = objTypes[definition.objId] ?: continue
            if (isRegistered(obj)) {
                continue
            }
            val effect = definition.effectName(obj)
            when (effect) {
                in SHIELD_EFFECTS -> registerShield(obj, MetadataShieldSpecial(manager))
                in INSTANT_EFFECTS ->
                    registerInstant(
                        obj,
                        MetadataInstantSpecial(staffProtection, nextMeleeAttack, effect),
                    )
                in RANGED_EFFECTS ->
                    registerRanged(
                        obj,
                        MetadataRangedSpecial(manager, ammunition, objTypes, effect),
                    )
                in MAGIC_EFFECTS -> registerMagic(obj, MetadataMagicSpecial(manager, effect))
                else -> registerMelee(obj, MetadataMeleeSpecial(manager, effect))
            }
        }
    }

    private fun SpecialAttackWeapons.Definition.effectName(obj: UnpackedObjType): String {
        val namedEffect = description?.substringBefore(':')?.trim()
        if (!namedEffect.isNullOrEmpty()) {
            return namedEffect
        }
        return when {
            obj.name.contains("shield", ignoreCase = true) ||
                obj.name.contains("ward", ignoreCase = true) -> SHIELD_EFFECT
            obj.name.contains("axe", ignoreCase = true) -> "Lumber Up"
            obj.name.contains("pickaxe", ignoreCase = true) -> "Rock Knocker"
            obj.name.contains("harpoon", ignoreCase = true) -> "Fishstabber"
            else -> error("Special-attack metadata has no effect description: $obj")
        }
    }

    private class MetadataInstantSpecial(
        private val staffProtection: StaffOfTheDeadStateRepository,
        private val nextMeleeAttack: NextMeleeAttackRepository,
        private val effect: String,
    ) : org.rsmod.api.specials.instant.InstantSpecialAttack {
        override suspend fun ProtectedAccess.activate(): Boolean {
            when (effect) {
                "Lumber Up" -> player.statBoost(stats.woodcutting, constant = 3, percent = 0)
                "Fishstabber" -> player.statBoost(stats.fishing, constant = 3, percent = 0)
                "Rock Knocker" -> player.statBoost(stats.mining, constant = 3, percent = 0)
                "Sanctuary" -> player.statBoost(stats.defence, constant = 8, percent = 0)
                "Rampage" -> {
                    val before =
                        player.stat(stats.attack) +
                            player.stat(stats.defence) +
                            player.stat(stats.ranged) +
                            player.stat(stats.magic)
                    player.statSub(stats.attack, constant = 0, percent = 10)
                    player.statSub(stats.defence, constant = 0, percent = 10)
                    player.statSub(stats.ranged, constant = 0, percent = 10)
                    player.statSub(stats.magic, constant = 0, percent = 10)
                    val after =
                        player.stat(stats.attack) +
                            player.stat(stats.defence) +
                            player.stat(stats.ranged) +
                            player.stat(stats.magic)
                    player.statBoost(
                        stats.strength,
                        constant = 10 + ((before - after) / 4),
                        percent = 0,
                    )
                }
                "Tumeken's Light" -> {
                    if (player.stat(stats.prayer) < 50) {
                        mes("You need at least 50 Prayer points to use this special attack.")
                        return false
                    }
                    player.statSub(stats.prayer, constant = 50, percent = 0)
                    player.statRestore(stats.attack)
                    player.statRestore(stats.strength)
                    player.statRestore(stats.defence)
                    player.statRestore(stats.ranged)
                    player.statRestore(stats.magic)
                    player.statRestore(stats.agility)
                    player.statRestore(stats.hitpoints)
                    player.statBoost(stats.hitpoints, constant = 0, percent = 20)
                    player.runEnergy = constants.run_max_energy
                    UpdateRun.energy(player, player.runEnergy)
                    VarPlayerIntMapSetter.set(player, varps.poison, 0)
                    VarPlayerIntMapSetter.set(player, varps.poison_2, 0)
                    VarPlayerIntMapSetter.set(player, varbits.poison_type, 0)
                }
                "Power of Death" -> {
                    staffProtection.activate(player, duration = 100)
                    mes("A protective power surrounds you.")
                }
                "Virulence" -> {
                    val poisonState = player.vars[varps.poison]
                    val toxinDamage =
                        when {
                            poisonState >= VENOM_THRESHOLD ->
                                minOf(MAX_VENOM_DAMAGE, (poisonState - VENOM_OFFSET) * 2)
                            poisonState > 0 -> (poisonState + POISON_STATE_PER_DAMAGE - 1) / 5
                            else -> 0
                        }
                    if (toxinDamage <= 0) {
                        mes("You are not poisoned or envenomed.")
                        return false
                    }
                    VarPlayerIntMapSetter.set(player, varps.poison, 0)
                    VarPlayerIntMapSetter.set(player, varps.poison_2, 0)
                    VarPlayerIntMapSetter.set(player, varbits.poison_type, 0)
                    nextMeleeAttack.setMinimumDamage(player, toxinDamage, duration = 100)
                    mes("The halberd metabolises the poison and empowers your next attack.")
                }
                "Entice" -> {
                    nextMeleeAttack.guarantee(player, duration = 10)
                    mes("Your next melee attack is guaranteed to hit.")
                }
                else -> return false
            }
            return true
        }
    }

    private class MetadataMeleeSpecial(
        private val manager: SpecialAttackManager,
        private val effect: String,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = strike(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = strike(target, attack)

        private fun ProtectedAccess.strike(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            if (effect == "Blood infusion") {
                val sacrifice = max(1, player.hitpoints / 4)
                player.statSub(stats.hitpoints, sacrifice, percent = 0)
            }

            manager.playWeaponFx(this, attack)
            val config =
                if (effect == "Unleash") {
                    val energy = manager.currentSpecialEnergy(this)
                    val fivePercentUnits = energy / 50
                    val extraEnergy = (energy - 50).coerceAtLeast(0)
                    if (extraEnergy > 0) {
                        manager.takeSpecialEnergy(this, extraEnergy)
                    }
                    MeleeConfig(
                        accuracyMultiplier = 1.0 + (fivePercentUnits * 0.05),
                        damageMultiplier = 1.0 + (fivePercentUnits * 0.025),
                    )
                } else {
                    MeleeConfig.forEffect(effect, player, target)
                }
            val hits = rollHits(target, attack, config)
            val total = hits.sum()
            manager.giveCombatXp(this, target, attack, total)
            hits.forEachIndexed { index, damage ->
                manager.queueMeleeHit(this, target, damage, delay = 1 + index)
            }
            applyMeleeEffect(target, effect, total)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.rollHits(
            target: PathingEntity,
            attack: CombatAttack.Melee,
            config: MeleeConfig,
        ): List<Int> {
            if (effect == "Disrupt") {
                val maxHit =
                    manager.calculateMeleeMaxHit(
                        this,
                        target,
                        attack.type,
                        attack.style,
                        multiplier = 1.0,
                    )
                return listOf(random.of((maxHit / 2)..max(1, maxHit * 3 / 2)))
            }
            if (effect == "Shove") {
                return listOf(0)
            }
            return List(config.hits) {
                val rolled =
                    if (config.guaranteed) {
                        manager.rollMeleeMaxHit(
                            this,
                            target,
                            attack.type,
                            attack.style,
                            config.damageMultiplier,
                        )
                    } else {
                        manager.rollMeleeDamage(
                            source = this,
                            target = target,
                            attack = attack,
                            accuracyMultiplier = config.accuracyMultiplier,
                            maxHitMultiplier = config.damageMultiplier,
                        )
                    }
                max(config.minimumDamage, rolled)
            }
        }

        private fun ProtectedAccess.applyMeleeEffect(
            target: PathingEntity,
            effect: String,
            damage: Int,
        ) {
            if (effect == "Shove") {
                immobilize(target, cycles = 5)
                return
            }
            if (damage <= 0) {
                return
            }
            when (effect) {
                "Smash" -> drainDefence(target, percent = if (damage > 0) 30 else 0)
                "Pulverize" -> drainDefence(target, percent = 35)
                "Backstab",
                "Snipe" -> drainDefence(target, amount = damage)
                "Warstrike" -> drainWarstrike(target, damage)
                "Sunder" -> drainAllCombat(target, max(1, damage / 10))
                "Weaken" -> drainAttackStrengthDefence(target, percent = 10)
                "Soulshot" -> drainMagic(target, damage)
                "Healing Blade" -> {
                    player.statHeal(stats.hitpoints, max(10, damage / 2), percent = 0)
                    player.statHeal(stats.prayer, max(5, damage / 4), percent = 0)
                }
                "Favour of the War God" -> {
                    drainPrayer(target, damage)
                    player.statHeal(stats.prayer, damage, percent = 0)
                }
                "Blood Sacrifice" -> {
                    manager.queueMeleeHit(this, target, 25, delay = 5)
                    player.statHeal(stats.hitpoints, 25, percent = 0)
                }
                "Energy Drain" -> {
                    if (target is Player) {
                        val siphoned = minOf(100, target.runEnergy)
                        target.runEnergy -= siphoned
                        player.runEnergy = (player.runEnergy + siphoned).coerceAtMost(1000)
                    }
                }
                "Sever" -> {
                    if (target is Player) {
                        VarPlayerIntMapSetter.set(target, varbits.protect_from_melee, 0)
                        VarPlayerIntMapSetter.set(target, varbits.protect_from_missiles, 0)
                        VarPlayerIntMapSetter.set(target, varbits.protect_from_magic, 0)
                    }
                }
                "Ice Cleave" -> immobilize(target, cycles = 34)
                "Binding Tentacle" -> immobilize(target, cycles = 9)
                "Scorching shackles" -> immobilize(target, cycles = 20)
                "Liquify" -> {
                    val boost = damage / 4
                    player.statBoost(stats.attack, constant = boost, percent = 0)
                    player.statBoost(stats.strength, constant = boost, percent = 0)
                    player.statBoost(stats.defence, constant = boost, percent = 0)
                }
                "Shield Bash" -> drainHighestOffensive(target, percent = 5)
                "Retainer" -> immobilize(target, cycles = 10)
            }
        }
    }

    private class MetadataRangedSpecial(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val objTypes: ObjTypeList,
        private val effect: String,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = shoot(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = shoot(target, attack)

        private fun ProtectedAccess.shoot(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weapon = objTypes[attack.weapon]
            val quiverType = objTypes.getOrNull(player.quiver)
            if (!ammunition.attemptAmmoUsage(player, weapon, quiverType)) {
                manager.stopCombat(this)
                return false
            }
            val config = RangedConfig.forEffect(effect)
            val thrown =
                weapon.isCategoryType(categories.throwing_weapon) && effect in THROWN_EFFECTS
            val ammoType = if (thrown) weapon else quiverType ?: weapon
            val available =
                if (thrown) player.righthand?.count ?: 0 else player.quiver?.count ?: Int.MAX_VALUE
            if (available < config.hits) {
                mes("You do not have enough ammunition for this special attack.")
                manager.stopCombat(this)
                return false
            }

            manager.playWeaponFx(this, attack)
            val projType = weapon.paramOrNull(params.proj_type)
            val travel = ammoType.paramOrNull(params.proj_travel)
            val launch = ammoType.paramOrNull(params.proj_launch)
            spotanim(launch, height = 96, slot = constants.spotanim_slot_combat)

            var totalDamage = 0
            repeat(config.hits) { index ->
                val projectile =
                    if (projType != null && travel != null) {
                        manager.spawnProjectile(this, target, travel, projType)
                    } else {
                        null
                    }
                val serverDelay = projectile?.serverCycles ?: (1 + index)
                val clientDelay = projectile?.clientCycles ?: 0
                val damage =
                    if (config.guaranteed) {
                        manager.rollRangedMaxHit(
                            this,
                            target,
                            attack.type,
                            attack.style,
                            config.damageMultiplier,
                            boltSpecDamage = 0,
                        )
                    } else {
                        manager.rollRangedDamage(
                            this,
                            target,
                            attack,
                            accuracyMultiplier = config.accuracyMultiplier,
                            maxHitMultiplier = config.damageMultiplier,
                        )
                    }
                totalDamage += damage
                if (index == 0) {
                    manager.queueRangedHit(
                        this,
                        target,
                        if (thrown) null else quiverType,
                        damage,
                        clientDelay,
                        serverDelay,
                    )
                } else {
                    manager.queueRangedDamage(
                        this,
                        target,
                        if (thrown) null else quiverType,
                        damage,
                        serverDelay,
                    )
                }
            }

            if (thrown) {
                ammunition.useThrownWeapon(
                    player,
                    weapon,
                    target.coords,
                    dropDelay = 1,
                    useCount = config.hits,
                )
            } else if (quiverType != null) {
                ammunition.useQuiverAmmo(
                    player,
                    quiverType,
                    target.coords,
                    dropDelay = 1,
                    useCount = config.hits,
                )
            }
            manager.giveCombatXp(this, target, attack, totalDamage)
            if (effect == "Chainhit" && target is Npc) publish(PlayerCombatEvents.Chainhit(player, target))
            if (effect == "Toxic Siphon") {
                player.statHeal(stats.hitpoints, totalDamage / 2, percent = 0)
            }
            if (effect == "Soulshot") {
                drainMagic(target, totalDamage)
            }
            if (effect == "Division") {
                val magicLevel =
                    when (target) {
                        is Npc -> target.magicLvl
                        is Player -> target.stat(stats.magic)
                    }
                drainDefence(target, amount = max(1, magicLevel / 10))
            }
            if (effect == "Snipe") {
                drainDefence(target, amount = totalDamage)
            }
            if (effect == "Hamstring" && target is Player) {
                target.runEnergy = (target.runEnergy - 200).coerceAtLeast(0)
            }
            if (effect == "Scorching shackles") {
                immobilize(target, cycles = 20)
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private class MetadataMagicSpecial(
        private val manager: SpecialAttackManager,
        private val effect: String,
    ) : MagicSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean = cast(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean = cast(target, attack)

        private fun ProtectedAccess.cast(
            target: PathingEntity,
            attack: CombatAttack.Staff,
        ): Boolean {
            anim(seqs.human_castwave_staff)
            val config = MagicConfig.forEffect(effect)
            val accurate =
                config.guaranteed ||
                    manager.rollStaffAccuracy(this, target, attack.style, config.accuracyMultiplier)
            val baseMaxHit = (player.magicLvl / 3) + 1
            val damage =
                if (accurate) {
                    manager.rollStaffMaxHit(this, target, baseMaxHit, config.damageMultiplier)
                } else {
                    0
                }
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, damage, clientDelay = 0)
            when (effect) {
                "Invocate" -> player.statAdd(stats.prayer, damage / 2, percent = 0)
                "Condemn" -> {
                    drainDefence(target, percent = 15)
                    drainMagic(target, percentOfCurrentMagic(target, 15))
                }
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private class MetadataShieldSpecial(private val manager: SpecialAttackManager) :
        ShieldSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc): Boolean = breathe(target)

        override suspend fun ProtectedAccess.attack(target: Player): Boolean = breathe(target)

        private fun ProtectedAccess.breathe(target: PathingEntity): Boolean {
            anim(seqs.human_castwave_staff)
            val damage = random.of(1..25)
            manager.queueMagicHit(this, target, damage, clientDelay = 0)
            manager.continueCombat(this, target)
            return true
        }
    }

    private data class MeleeConfig(
        val accuracyMultiplier: Double = 1.0,
        val damageMultiplier: Double = 1.0,
        val hits: Int = 1,
        val guaranteed: Boolean = false,
        val minimumDamage: Int = 0,
    ) {
        companion object {
            fun forEffect(effect: String, player: Player, target: PathingEntity): MeleeConfig =
                when (effect) {
                    "Puncture" -> MeleeConfig(1.15, 1.15, hits = 2)
                    "Abyssal Puncture" -> MeleeConfig(1.25, 0.85, hits = 2)
                    "Slice and Dice" -> MeleeConfig(1.0, 0.5, hits = 4)
                    "Burning barrage" -> MeleeConfig(1.0, 0.67, hits = 3)
                    "Lingering Lightning" -> MeleeConfig(1.0, 0.6, hits = 2)
                    "Brand" -> MeleeConfig(1.25, 1.25, hits = 4)
                    "Sweep" -> MeleeConfig(1.0, 1.1, hits = if (target.size > 1) 2 else 1)
                    "Shield Bash" -> MeleeConfig(1.2, 1.0, hits = if (target is Npc) 2 else 1)
                    "The Judgement" -> MeleeConfig(2.0, 1.375)
                    "Healing Blade",
                    "Ice Cleave",
                    "Blood Sacrifice" -> MeleeConfig(2.0, 1.1)
                    "Smash" -> MeleeConfig(1.0, 1.5)
                    "Shatter" -> MeleeConfig(1.25, 1.5)
                    "Wild Stab" -> MeleeConfig(1.25, 1.25)
                    "Cleave" -> MeleeConfig(1.25, 1.25)
                    "Sunder" -> MeleeConfig(2.0, 1.1)
                    "Hammer Blow" -> MeleeConfig(1.5, 1.0, minimumDamage = 5)
                    "Concentrated Shot" -> MeleeConfig(1.25, 1.25)
                    "Wrath of Amascut" -> MeleeConfig(2.0, 1.25)
                    "Blood infusion" -> MeleeConfig(1.25, 1.25)
                    "Flames of Ralos" -> MeleeConfig(1.5, 1.5)
                    "Soul Rend" -> MeleeConfig(2.0, 1.3)
                    "Liquify" -> MeleeConfig(2.0, 1.0)
                    "Eviscerate" -> MeleeConfig(1.5, 1.0)
                    "Impale" -> MeleeConfig(1.1, 1.1)
                    "Saradomin's Blessed Lightning" -> MeleeConfig(1.0, 1.25)
                    "Seeking Lunge" -> MeleeConfig(1.0, 0.7)
                    "Feint" -> MeleeConfig(2.0, 1.2)
                    "Spear Wall" -> MeleeConfig(1.0, 0.5)
                    "Behead",
                    "Retainer",
                    "Powerstab",
                    "Sol Slam" -> MeleeConfig()
                    "Penance" -> {
                        val usedPrayer = max(0, player.basePrayerLvl - player.stat(stats.prayer))
                        MeleeConfig(damageMultiplier = 1.0 + (usedPrayer * 0.005))
                    }
                    "Powershot",
                    "Soulshot" -> MeleeConfig(guaranteed = true)
                    else -> MeleeConfig()
                }
        }
    }

    private data class RangedConfig(
        val accuracyMultiplier: Double = 1.0,
        val damageMultiplier: Double = 1.0,
        val hits: Int = 1,
        val guaranteed: Boolean = false,
    ) {
        companion object {
            fun forEffect(effect: String): RangedConfig =
                when (effect) {
                    "Snapshot",
                    "Duality" -> RangedConfig(hits = 2)
                    "Descent of Darkness" -> RangedConfig(1.0, 1.3, hits = 2)
                    "Swarm" -> RangedConfig(hits = 4, damageMultiplier = 0.4)
                    "Armadyl Eye",
                    "Evoke" -> RangedConfig(accuracyMultiplier = 2.0)
                    "Concentrated Shot" -> RangedConfig(1.25, 1.25)
                    "Toxic Siphon" -> RangedConfig(2.0, 1.5)
                    "Powershot",
                    "Soulshot" -> RangedConfig(guaranteed = true)
                    "Momentum Throw" -> RangedConfig(1.25, 1.0)
                    "Eclipse" -> RangedConfig(1.5, 1.0)
                    "Hamstring",
                    "Phantom Strike" -> RangedConfig(1.5, 1.2)
                    else -> RangedConfig()
                }
        }
    }

    private data class MagicConfig(
        val accuracyMultiplier: Double = 1.0,
        val damageMultiplier: Double = 1.0,
        val guaranteed: Boolean = false,
    ) {
        companion object {
            fun forEffect(effect: String): MagicConfig =
                when (effect) {
                    "Immolate" -> MagicConfig(1.5, 1.0)
                    "Condemn" -> MagicConfig(1.5, 1.2)
                    "Pulsate" -> MagicConfig(2.0, 2.0)
                    "Soul Rend" -> MagicConfig(2.0, 1.3)
                    else -> MagicConfig()
                }
        }
    }

    companion object {
        private const val VENOM_THRESHOLD = 1_000_000
        private const val VENOM_OFFSET = 999_997
        private const val MAX_VENOM_DAMAGE = 20
        private const val POISON_STATE_PER_DAMAGE = 5
        private const val SHIELD_EFFECT = "Dragonfire"

        private val SHIELD_EFFECTS = setOf(SHIELD_EFFECT)

        private val INSTANT_EFFECTS =
            setOf(
                "Lumber Up",
                "Fishstabber",
                "Rock Knocker",
                "Sanctuary",
                "Rampage",
                "Power of Death",
                "Tumeken's Light",
                "Virulence",
                "Entice",
            )

        private val RANGED_EFFECTS =
            setOf(
                "Annihilate",
                "Armadyl Eye",
                "Chainhit",
                "Concentrated Shot",
                "Division",
                "Descent of Darkness",
                "Duality",
                "Eclipse",
                "Evoke",
                "Hamstring",
                "Momentum Throw",
                "Phantom Strike",
                "Powershot",
                "Scorching shackles",
                "Snapshot",
                "Snipe",
                "Soulshot",
                "Swarm",
                "Toxic Siphon",
            )

        private val THROWN_EFFECTS =
            setOf("Chainhit", "Duality", "Hamstring", "Momentum Throw", "Phantom Strike")

        private val MAGIC_EFFECTS =
            setOf(
                "Condemn",
                "Explosive Shatter",
                "Immolate",
                "Invocate",
                "Pulsate",
                "Scatter ashes",
                "Soul Rend",
            )
    }
}

private fun drainDefence(target: PathingEntity, percent: Int = 0, amount: Int = 0) {
    when (target) {
        is Npc -> {
            val drain = amount + (target.baseDefenceLvl * percent / 100)
            target.defenceLvl = (target.defenceLvl - drain).coerceAtLeast(0)
        }
        is Player -> target.statSub(stats.defence, amount, percent)
    }
}

private fun drainMagic(target: PathingEntity, amount: Int) {
    when (target) {
        is Npc -> target.magicLvl = (target.magicLvl - amount).coerceAtLeast(0)
        is Player -> target.statSub(stats.magic, amount, percent = 0)
    }
}

private fun percentOfCurrentMagic(target: PathingEntity, percent: Int): Int =
    when (target) {
        is Npc -> max(1, target.magicLvl * percent / 100)
        is Player -> max(1, target.stat(stats.magic) * percent / 100)
    }

private fun drainHighestOffensive(target: PathingEntity, percent: Int) {
    when (target) {
        is Npc -> {
            val highest =
                maxOf(target.attackLvl, target.strengthLvl, target.rangedLvl, target.magicLvl)
            val drain = max(1, highest * percent / 100)
            when (highest) {
                target.attackLvl -> target.attackLvl = (target.attackLvl - drain).coerceAtLeast(0)
                target.strengthLvl ->
                    target.strengthLvl = (target.strengthLvl - drain).coerceAtLeast(0)
                target.rangedLvl -> target.rangedLvl = (target.rangedLvl - drain).coerceAtLeast(0)
                else -> target.magicLvl = (target.magicLvl - drain).coerceAtLeast(0)
            }
        }
        is Player -> {
            val offensive =
                listOf(stats.attack, stats.strength, stats.ranged, stats.magic).maxBy(target::stat)
            target.statSub(offensive, constant = 0, percent = percent)
        }
    }
}

private fun drainPrayer(target: PathingEntity, amount: Int) {
    if (target is Player) {
        target.statSub(stats.prayer, amount, percent = 0)
    }
}

private fun drainAttackStrengthDefence(target: PathingEntity, percent: Int) {
    when (target) {
        is Npc -> {
            target.attackLvl =
                (target.attackLvl - target.baseAttackLvl * percent / 100).coerceAtLeast(0)
            target.strengthLvl =
                (target.strengthLvl - target.baseStrengthLvl * percent / 100).coerceAtLeast(0)
            target.defenceLvl =
                (target.defenceLvl - target.baseDefenceLvl * percent / 100).coerceAtLeast(0)
        }
        is Player -> {
            target.statSub(stats.attack, 0, percent)
            target.statSub(stats.strength, 0, percent)
            target.statSub(stats.defence, 0, percent)
        }
    }
}

private fun drainAllCombat(target: PathingEntity, amount: Int) {
    when (target) {
        is Npc -> {
            target.attackLvl = (target.attackLvl - amount).coerceAtLeast(0)
            target.strengthLvl = (target.strengthLvl - amount).coerceAtLeast(0)
            target.defenceLvl = (target.defenceLvl - amount).coerceAtLeast(0)
            target.rangedLvl = (target.rangedLvl - amount).coerceAtLeast(0)
            target.magicLvl = (target.magicLvl - amount).coerceAtLeast(0)
        }
        is Player -> {
            target.statSub(stats.attack, amount, 0)
            target.statSub(stats.strength, amount, 0)
            target.statSub(stats.defence, amount, 0)
            target.statSub(stats.ranged, amount, 0)
            target.statSub(stats.magic, amount, 0)
        }
    }
}

private fun drainWarstrike(target: PathingEntity, amount: Int) {
    var remaining = amount
    when (target) {
        is Npc -> {
            val defenceDrain = minOf(target.defenceLvl, remaining)
            target.defenceLvl -= defenceDrain
            remaining -= defenceDrain
            val strengthDrain = minOf(target.strengthLvl, remaining)
            target.strengthLvl -= strengthDrain
            remaining -= strengthDrain
            val attackDrain = minOf(target.attackLvl, remaining)
            target.attackLvl -= attackDrain
            remaining -= attackDrain
            target.magicLvl = (target.magicLvl - remaining).coerceAtLeast(0)
        }
        is Player -> {
            val order =
                listOf(
                    stats.defence,
                    stats.strength,
                    stats.prayer,
                    stats.attack,
                    stats.magic,
                    stats.ranged,
                )
            for (stat in order) {
                if (remaining <= 0) break
                val drain = minOf(target.stat(stat), remaining)
                target.statSub(stat, drain, 0)
                remaining -= drain
            }
        }
    }
}

private fun immobilize(target: PathingEntity, cycles: Int) {
    target.abortRoute()
    target.delay = max(target.delay, target.currentMapClock + cycles)
}
