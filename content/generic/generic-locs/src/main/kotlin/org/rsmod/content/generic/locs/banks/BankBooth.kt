package org.rsmod.content.generic.locs.banks

import jakarta.inject.Inject
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.player.events.interact.LocContentEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BankBooth @Inject constructor(private val types: LocTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc2(content.bank_booth) { openBank() }
        onGameStartup {
            for (type in types.values) {
                val chest = type.name.startsWith("Bank chest", ignoreCase = true)
                val deposit = type.name.startsWith("Bank deposit", ignoreCase = true)
                if ("Bank" !in type.op && !chest && !deposit) continue
                for ((index, option) in type.op.withIndex()) {
                    val action: ProtectedAccess.() -> Unit =
                        when {
                            option == "Bank" || (chest && option == "Use") -> { -> openBank() }
                            option == "Collect" -> { ->
                                    ifOpenMainModal(interfaces.ge_collection_box)
                                }
                            option == "Deposit-box" || (deposit && option == "Deposit") -> { ->
                                    ifOpenMainModal(BankInterfaces.deposit)
                                }
                            else -> continue
                        }
                    if (
                        index !in ops.indices ||
                            eventBus.contains(ops[index], type.id) ||
                            eventBus.contains(groupOps[index], type.contentGroup)
                    )
                        continue
                    when (index) {
                        0 -> onOpLoc1(type) { action() }
                        1 -> onOpLoc2(type) { action() }
                        2 -> onOpLoc3(type) { action() }
                        3 -> onOpLoc4(type) { action() }
                    }
                }
            }
        }
    }

    private fun ProtectedAccess.openBank() {
        ifOpenMainSidePair(main = interfaces.bank_main, side = interfaces.bank_side)
    }

    private val ops =
        listOf(
            LocEvents.Op1::class.java,
            LocEvents.Op2::class.java,
            LocEvents.Op3::class.java,
            LocEvents.Op4::class.java,
        )
    private val groupOps =
        listOf(
            LocContentEvents.Op1::class.java,
            LocContentEvents.Op2::class.java,
            LocContentEvents.Op3::class.java,
            LocContentEvents.Op4::class.java,
        )
}

internal object BankInterfaces : InterfaceReferences() {
    val deposit = find("bank_depositbox")
}
