package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.back
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class EssencePouchStorage
@Inject
constructor(
    private val random: GameRandom,
    private val objTypes: ObjTypeList,
    private val accessLauncher: ProtectedAccessLauncher,
) {
    public fun fill(access: ProtectedAccess, pouch: EssencePouchDefinition, slot: Int): Int {
        if (!access.requirePouchUse(pouch)) {
            return 0
        }
        val initialCount = count(access.player, pouch)
        var capacity = capacity(access.player, pouch)
        if (initialCount >= capacity) {
            access.mes(FULL_MESSAGE)
            return 0
        }

        val currentType = essenceType(access.player, pouch)
        val essence =
            if (initialCount == 0 || currentType == null) {
                acceptedEssences.firstOrNull { access.inv.contains(it.obj) }
            } else {
                currentType
            }
        if (essence == null || !access.inv.contains(essence.obj)) {
            access.mes(NO_ESSENCE_MESSAGE)
            return 0
        }

        var stored = initialCount
        var degradation = degradation(access.player, pouch)
        var moved = 0
        val protected = hasPouchProtection(access.player)
        while (stored < capacity && access.inv.contains(essence.obj)) {
            val deleted = access.invDel(access.inv, essence.obj, count = 1).completed()
            if (deleted == 0) {
                break
            }
            stored++
            moved++

            if (pouch.degraded != null && !protected) {
                degradation += if (pouch.deterministicDegradation) 1 else random.of(4)
                setDegradation(access.player, pouch, degradation)
                capacity = pouch.capacity(access.player.statBase(stats.runecrafting), degradation)
            }

            if (stored > capacity) {
                val excess = stored - capacity
                val returned =
                    access
                        .invAdd(access.inv, essence.obj, count = excess, strict = false)
                        .completed()
                stored = capacity
                moved -= returned
            }

            setCount(access.player, pouch, stored)
            setEssenceType(access.player, pouch, if (stored == 0) 0 else essence.code)
            if (capacity == 0) {
                val current = access.inv[slot]
                if (current != null && pouch.variants.any { current.isType(it) }) {
                    val currentType = pouch.variants.first { current.isType(it) }
                    access.invDel(access.inv, currentType, count = 1, slot = slot)
                }
                clear(access.player, pouch)
                return 0
            }
        }

        val current = access.inv[slot]
        if (
            current.isType(pouch.normal) && pouch.degraded != null && pouch.isDegraded(degradation)
        ) {
            access.invReplaceSlot(access.inv, slot, count = 1, pouch.degraded)
        }
        return moved
    }

    public fun empty(access: ProtectedAccess, pouch: EssencePouchDefinition): Int {
        if (!access.requirePouchUse(pouch)) {
            return 0
        }
        val stored = count(access.player, pouch)
        if (stored <= 0) {
            access.mes(EMPTY_MESSAGE)
            return 0
        }
        val essence = essenceType(access.player, pouch)
        if (essence == null) {
            clear(access.player, pouch)
            return 0
        }
        val moved =
            access.invAdd(access.inv, essence.obj, count = stored, strict = false).completed()
        if (moved > 0) {
            val remaining = stored - moved
            setCount(access.player, pouch, remaining)
            if (remaining == 0) {
                setEssenceType(access.player, pouch, 0)
            }
        }
        return moved
    }

    public fun check(access: ProtectedAccess, pouch: EssencePouchDefinition) {
        if (!access.requirePouchUse(pouch)) {
            return
        }
        val stored = count(access.player, pouch)
        val essence = essenceType(access.player, pouch)
        when {
            stored <= 0 || essence == null -> access.mes(EMPTY_MESSAGE)
            stored == 1 -> access.mes("There is 1 ${essence.displayName} in your pouch.")
            else -> access.mes("There are $stored ${essence.displayName} in your pouch.")
        }
    }

    public fun bankside(player: Player, pouch: EssencePouchDefinition, slot: Int) {
        accessLauncher.launch(player) {
            if (count(player, pouch) > 0) {
                empty(this, pouch)
            } else {
                fill(this, pouch, slot)
            }
        }
    }

    public fun needsRepair(access: ProtectedAccess): Boolean =
        EssencePouchDefinitions.all.any { pouch ->
            val degraded = pouch.degraded
            degraded != null &&
                (degradation(access.player, pouch) > 0 ||
                    degraded in access.inv ||
                    degraded in access.bank)
        }

    public fun repairAll(access: ProtectedAccess): Int {
        var repaired = 0
        for (pouch in EssencePouchDefinitions.all) {
            val degraded = pouch.degraded ?: continue
            if (degradation(access.player, pouch) > 0) {
                setDegradation(access.player, pouch, 0)
                repaired++
            }
            repaired += replaceAll(access, access.inv, degraded, pouch.normal)
            repaired += replaceAll(access, access.bank, degraded, pouch.normal)
        }
        return repaired
    }

    public fun capacity(player: Player, pouch: EssencePouchDefinition): Int =
        pouch.capacity(player.statBase(stats.runecrafting), degradation(player, pouch))

    public fun count(player: Player, pouch: EssencePouchDefinition): Int =
        when (pouch.normal.id) {
            RunecraftObjs.smallPouch.id -> player.smallPouchAmount
            RunecraftObjs.mediumPouch.id -> player.mediumPouchAmount
            RunecraftObjs.largePouch.id -> player.largePouchAmount
            RunecraftObjs.giantPouch.id -> player.giantPouchAmount
            RunecraftObjs.colossalPouch.id -> player.colossalPouchAmount
            else -> error("Unknown essence pouch: ${pouch.normal}")
        }

    public fun essenceType(player: Player, pouch: EssencePouchDefinition): EssenceType? =
        EssenceType.fromCode(
            when (pouch.normal.id) {
                RunecraftObjs.smallPouch.id -> player.smallPouchType
                RunecraftObjs.mediumPouch.id -> player.mediumPouchType
                RunecraftObjs.largePouch.id -> player.largePouchType
                RunecraftObjs.giantPouch.id -> player.giantPouchType
                RunecraftObjs.colossalPouch.id -> player.colossalPouchType
                else -> error("Unknown essence pouch: ${pouch.normal}")
            }
        )

    public fun degradation(player: Player, pouch: EssencePouchDefinition): Int =
        when (pouch.normal.id) {
            RunecraftObjs.smallPouch.id -> 0
            RunecraftObjs.mediumPouch.id -> player.mediumPouchDegradation
            RunecraftObjs.largePouch.id -> player.largePouchDegradation
            RunecraftObjs.giantPouch.id -> player.giantPouchDegradation
            RunecraftObjs.colossalPouch.id -> player.colossalPouchDegradation
            else -> error("Unknown essence pouch: ${pouch.normal}")
        }

    public fun setState(
        player: Player,
        pouch: EssencePouchDefinition,
        count: Int,
        essence: EssenceType?,
        degradation: Int = degradation(player, pouch),
    ) {
        setCount(player, pouch, count)
        setEssenceType(player, pouch, if (count == 0) 0 else requireNotNull(essence).code)
        setDegradation(player, pouch, degradation)
    }

    public fun clear(player: Player, pouch: EssencePouchDefinition) {
        setCount(player, pouch, 0)
        setEssenceType(player, pouch, 0)
        setDegradation(player, pouch, 0)
    }

    private fun replaceAll(
        access: ProtectedAccess,
        inventory: Inventory,
        from: ObjType,
        to: ObjType,
    ): Int {
        var replaced = 0
        for (slot in inventory.indices) {
            if (inventory[slot].isType(from)) {
                if (access.invReplaceSlot(inventory, slot, count = 1, to).success) {
                    replaced++
                }
            }
        }
        return replaced
    }

    private fun setCount(player: Player, pouch: EssencePouchDefinition, value: Int) {
        when (pouch.normal.id) {
            RunecraftObjs.smallPouch.id -> player.smallPouchAmount = value
            RunecraftObjs.mediumPouch.id -> player.mediumPouchAmount = value
            RunecraftObjs.largePouch.id -> player.largePouchAmount = value
            RunecraftObjs.giantPouch.id -> player.giantPouchAmount = value
            RunecraftObjs.colossalPouch.id -> player.colossalPouchAmount = value
            else -> error("Unknown essence pouch: ${pouch.normal}")
        }
    }

    private fun setEssenceType(player: Player, pouch: EssencePouchDefinition, value: Int) {
        when (pouch.normal.id) {
            RunecraftObjs.smallPouch.id -> player.smallPouchType = value
            RunecraftObjs.mediumPouch.id -> player.mediumPouchType = value
            RunecraftObjs.largePouch.id -> player.largePouchType = value
            RunecraftObjs.giantPouch.id -> player.giantPouchType = value
            RunecraftObjs.colossalPouch.id -> player.colossalPouchType = value
            else -> error("Unknown essence pouch: ${pouch.normal}")
        }
    }

    private fun setDegradation(player: Player, pouch: EssencePouchDefinition, value: Int) {
        when (pouch.normal.id) {
            RunecraftObjs.smallPouch.id -> Unit
            RunecraftObjs.mediumPouch.id -> player.mediumPouchDegradation = value
            RunecraftObjs.largePouch.id -> player.largePouchDegradation = value
            RunecraftObjs.giantPouch.id -> player.giantPouchDegradation = value
            RunecraftObjs.colossalPouch.id -> player.colossalPouchDegradation = value
            else -> error("Unknown essence pouch: ${pouch.normal}")
        }
    }

    private fun hasPouchProtection(player: Player): Boolean {
        if(player.vars[TempleVars.playingGuardiansOfTheRift]!=0 && player.lefthand?.id==26848)return true
        val cape = player.back ?: return false
        if (
            cape.id == objs.runecrafting_skillcape.id ||
                cape.id == objs.runecrafting_skillcape_t.id ||
                cape.id == objs.max_cape_worn.id
        ) {
            return true
        }
        return objTypes[cape].internalName?.startsWith("skillcape_max") == true
    }

    private fun ProtectedAccess.requirePouchUse(pouch: EssencePouchDefinition): Boolean {
        if (!player.members) {
            mes(MEMBERS_MESSAGE)
            return false
        }
        if (player.statBase(stats.runecrafting) < pouch.requiredLevel) {
            mes("You need a Runecraft level of ${pouch.requiredLevel} to use this pouch.")
            return false
        }
        return true
    }

    public companion object {
        public const val EMPTY_MESSAGE: String = "There is no essence in this pouch."
        public const val FULL_MESSAGE: String = "Your pouch is full."
        public const val NO_ESSENCE_MESSAGE: String =
            "You do not have any essence to put in this pouch."
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use an essence pouch."

        internal val acceptedEssences: List<EssenceType> =
            listOf(EssenceType.Guardian, EssenceType.Daeyalt, EssenceType.Pure, EssenceType.Rune)
    }
}

public enum class EssenceType(
    public val code: Int,
    public val obj: ObjType,
    public val displayName: String,
) {
    Rune(1, RunecraftObjs.runeEssence, "rune essence"),
    Pure(2, RunecraftObjs.pureEssence, "pure essence"),
    Daeyalt(3, RunecraftObjs.daeyaltEssence, "daeyalt essence"),
    Guardian(4, RunecraftObjs.guardianEssence, "guardian essence");

    companion object {
        private val byCode = entries.associateBy(EssenceType::code)

        public fun fromCode(code: Int): EssenceType? = byCode[code]
    }
}

private var Player.smallPouchAmount by intVarBit(RunecraftVarbits.smallPouchAmount)
private var Player.mediumPouchAmount by intVarBit(RunecraftVarbits.mediumPouchAmount)
private var Player.largePouchAmount by intVarBit(RunecraftVarbits.largePouchAmount)
private var Player.giantPouchAmount by intVarBit(RunecraftVarbits.giantPouchAmount)
private var Player.colossalPouchAmount by intVarBit(RunecraftVarbits.colossalPouchAmount)
private var Player.smallPouchType by intVarBit(RunecraftVarbits.smallPouchType)
private var Player.mediumPouchType by intVarBit(RunecraftVarbits.mediumPouchType)
private var Player.largePouchType by intVarBit(RunecraftVarbits.largePouchType)
private var Player.giantPouchType by intVarBit(RunecraftVarbits.giantPouchType)
private var Player.colossalPouchType by intVarBit(RunecraftVarbits.colossalPouchType)
private var Player.mediumPouchDegradation by intVarp(RunecraftVarps.mediumPouchDegradation)
private var Player.largePouchDegradation by intVarp(RunecraftVarps.largePouchDegradation)
private var Player.giantPouchDegradation by intVarp(RunecraftVarps.giantPouchDegradation)
private var Player.colossalPouchDegradation by intVarBit(RunecraftVarbits.colossalPouchDegradation)
