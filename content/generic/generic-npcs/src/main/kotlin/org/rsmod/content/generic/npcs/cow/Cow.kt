package org.rsmod.content.generic.npcs.cow

import jakarta.inject.Inject
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Cow
@Inject
constructor(private val worldRepo: WorldRepository, private val random: GameRandom) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onAiTimer(content.cow) { npc.cowTimer() }
        onOpNpcU(content.cow) { mes("The cow doesn't want that.") }
        onOpNpcU(content.cow, objs.bucket_empty) {
            mes("Only dairy cows are suitable for milking.")
        }
        onOpNpc1(cow_npcs.fairy_cow) { talkToFairyCow(it.npc) }
        onOpNpcU(cow_npcs.fairy_cow, objs.nuffs_certificate) { showCertificateToFairyCow(it.npc) }
    }

    private suspend fun ProtectedAccess.talkToFairyCow(npc: Npc) =
        startDialogue(npc) {
            chatNpc(
                neutral,
                "Hmm, interesting, there appears to be a human trying to talk to me. Is it aware " +
                    "that cows can't talk I wonder? Maybe if I just ignore it, it might go away.",
            )
            mesbox("The cow is studiously ignoring you.")
        }

    private suspend fun ProtectedAccess.showCertificateToFairyCow(npc: Npc) =
        startDialogue(npc) {
            mesbox("You show the certificate to the cow.")
            chatPlayer(quiz, "Do you know what these strange marks mean?")
            chatNpc(
                neutral,
                "Can't the human take a hint? I've been ignoring you! Do you think I would ignore " +
                    "you if I wanted to discuss your Rorschach ink-blots ?",
            )
        }

    private fun Npc.cowTimer() {
        val next = random.of(15..34)
        aiTimer(next)

        if (random.randomBoolean(4)) {
            sayFlavourText()
        }
    }

    private fun Npc.sayFlavourText() {
        worldRepo.soundArea(coords, synths.cow_atmospheric, radius = 10)
        say("Moo")
        anim(seqs.cow_update_graze)
    }
}
