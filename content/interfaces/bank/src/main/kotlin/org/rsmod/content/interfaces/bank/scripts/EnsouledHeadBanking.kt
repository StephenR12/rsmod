package org.rsmod.content.interfaces.bank.scripts

import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjTypeList

/** The reversible source change used while a bank transaction expires fresh ensouled heads. */
internal data class FreshHeadExpiration(val slot: Int, val original: InvObj, val processedId: Int)

/**
 * Converts a fresh-drop head to its ordinary tradeable identity before it enters bank storage.
 *
 * Cache 233 stores each fresh head immediately before an identically named, certificate-capable
 * processed head. The description guard keeps this structural rule specific to reanimation heads
 * while allowing the same code to follow later cache revisions.
 */
internal fun Inventory.expireFreshEnsouledHeads(
    objTypes: ObjTypeList,
    slots: Iterable<Int> = indices,
): List<FreshHeadExpiration> = buildList {
    for (slot in slots) {
        val original = this@expireFreshEnsouledHeads[slot] ?: continue
        val initial = objTypes[original]
        if (
            !initial.name.startsWith("Ensouled ") ||
                !initial.desc.contains("reanimate", ignoreCase = true) ||
                initial.canCert
        ) {
            continue
        }
        val processed = objTypes.types[initial.id + 1] ?: continue
        if (
            processed.name != initial.name || processed.desc != initial.desc || !processed.canCert
        ) {
            continue
        }
        this@expireFreshEnsouledHeads[slot] = InvObj(processed, original.count)
        add(FreshHeadExpiration(slot, original, processed.id))
    }
}

/** Restores only heads which did not actually leave the source inventory. */
internal fun Inventory.restoreUndepositedFreshHeads(expirations: Iterable<FreshHeadExpiration>) {
    for (expiration in expirations) {
        val remaining = this[expiration.slot] ?: continue
        if (remaining.id == expiration.processedId) {
            this[expiration.slot] = expiration.original.copy(count = remaining.count)
        }
    }
}
