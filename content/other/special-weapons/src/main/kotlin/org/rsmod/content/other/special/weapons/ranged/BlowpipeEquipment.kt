package org.rsmod.content.other.special.weapons.ranged

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.hat
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.fletching.toxicblowpipe.ToxicBlowpipeState
import org.rsmod.content.skills.magic.spell.attacks.ancient.AncientNpcPoisonRepository
import org.rsmod.content.skills.magic.spell.utility.standard.PoisonProtectionRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isAnyType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BlowpipeEquipment
@Inject
constructor(
    private val npcPoison: AncientNpcPoisonRepository,
    private val playerPoison: PoisonProtectionRepository,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (pipe in listOf(objs.blazing_blowpipe, objs.toxic_blowpipe)) {
            onEvent<NpcHitEvents.WeaponImpact>(pipe.id) {
                if (source.hasSerpentineHelm() || random.randomBoolean(4)) npcPoison.envenom(npc)
            }
            onProtectedEvent<BankedObjEvents.ConfigureCharges>(pipe.id) {
                player.scaleTarget =
                    countDialog("How many scales should the blowpipe hold on withdrawal?")
                        .coerceIn(0, ToxicBlowpipeState.MAX_CAPACITY)
            }
        }
        onEvent<PlayerHitEvents.Impact> {
            if (
                damage > 0 &&
                    hit.isFromPlayer &&
                    (hit.isRighthandObj(objs.toxic_blowpipe) ||
                        hit.isRighthandObj(objs.blazing_blowpipe)) &&
                    !player.hasSerpentineHelm() &&
                    random.randomBoolean(4)
            )
                playerPoison.inflictVenom(player)
        }
        for (pipe in
            listOf(
                objs.toxic_blowpipe,
                objs.blazing_blowpipe,
                objs.toxic_blowpipe_empty,
                objs.blazing_blowpipe_empty,
            )) {
            onProtectedEvent<BankedObjEvents.Autocharge>(pipe.id) { autocharge(this, it) }
        }
    }

    fun autocharge(access: ProtectedAccess, event: BankedObjEvents.Autocharge) {
        val pipe = access.inv[event.slot] ?: return
        if (pipe != event.obj) return
        val state = ToxicBlowpipeState.unpack(pipe)
        val needed = access.player.scaleTarget - state.scales
        if (needed <= 0) return
        val amount = event.takeMaterial(objs.zulrahs_scales, needed)
        if (amount > 0) {
            val type =
                if (pipe.isAnyType(objs.blazing_blowpipe, objs.blazing_blowpipe_empty))
                    objs.blazing_blowpipe
                else objs.toxic_blowpipe
            access.inv[event.slot] =
                InvObj(type, vars = state.copy(scales = state.scales + amount).pack())
        }
        if (amount < needed)
            access.mes(
                "There aren't enough Zulrah's scales in your bank to charge the blowpipe fully."
            )
    }

    private fun Player.hasSerpentineHelm(): Boolean =
        hat.isAnyType(BlowpipeHelms.green, BlowpipeHelms.cyan, BlowpipeHelms.red)
}

internal object BlowpipeHelms : ObjReferences() {
    val green = find("serpentine_helm_charged")
    val cyan = find("serpentine_helm_charged_cyan")
    val red = find("serpentine_helm_charged_red")
}

internal object BlowpipeBits : VarBitReferences() {
    val scales = find("charges_toxic_blowpipe_quantity")
}

internal var Player.scaleTarget by intVarBit(BlowpipeBits.scales)
