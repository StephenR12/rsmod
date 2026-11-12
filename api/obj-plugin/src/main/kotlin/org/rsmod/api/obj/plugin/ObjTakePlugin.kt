package org.rsmod.api.obj.plugin

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.synths
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.advanced.onDefaultOpObj3
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj
import org.rsmod.objtx.TransactionResultList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ObjTakePlugin
@Inject
constructor(
    private val repo: ObjRepository,
    destinations: Set<ObjTakeDestination>,
    transforms: Set<ObjTakeTransform>,
) : PluginScript() {
    private val destinations =
        destinations.sortedWith(
            compareByDescending<ObjTakeDestination> { it.priority }.thenBy { it.javaClass.name }
        )
    private val transforms =
        transforms.sortedWith(
            compareByDescending<ObjTakeTransform> { it.priority }.thenBy { it.javaClass.name }
        )

    override fun ScriptContext.startup() {
        onDefaultOpObj3 { triggerTake(it.obj) }
    }

    private suspend fun ProtectedAccess.triggerTake(obj: Obj) {
        if (!player.hasDestinationSpace(obj)) {
            player.mes(Constants.dm_take_invspace)
            return
        }
        player.resetAnim()
        if (player.coords != obj.coords) {
            takeFar(obj)
        } else {
            soundSynth(synths.pick2)
            takeClose(obj)
        }
    }

    private suspend fun ProtectedAccess.takeFar(obj: Obj) {
        delay(1)
        anim(seqs.human_pickuptable)
        soundSynth(synths.pick2)
        takeClose(obj)
    }

    private fun ProtectedAccess.takeClose(obj: Obj) {
        val take = player.transaction(obj)
        if (take.result.failure) {
            mes(Constants.dm_take_invspace)
            return
        }
        val removed = repo.del(obj)
        if (!removed) {
            mes(Constants.dm_take_taken)
            return
        }
        take.result.commitAll()
        take.destination?.onTake(this, obj)
    }

    private fun Player.hasDestinationSpace(obj: Obj): Boolean = transaction(obj).result.success

    private fun Player.transaction(obj: Obj): TakeTransaction {
        val redirect =
            destinations.firstNotNullOfOrNull { destination ->
                destination.destination(this, obj)?.let { destination to it }
            }
        val target = redirect?.second ?: inv
        val type = transforms.firstNotNullOfOrNull { it.transform(this, obj) } ?: obj.type
        return TakeTransaction(
            result = invAdd(target, type, obj.count, vars = obj.vars, autoCommit = false),
            destination = redirect?.first,
        )
    }

    private data class TakeTransaction(
        val result: TransactionResultList<InvObj>,
        val destination: ObjTakeDestination?,
    )
}
