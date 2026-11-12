package org.rsmod.content.generic.npcs.cow

import jakarta.inject.Inject
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.content.skills.thieving.ThievingStunStateRepository
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DairyCow @Inject constructor(private val stunStates: ThievingStunStateRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(content.dairy_cow) { attemptMilkingCow(it.loc) }
        onOpLoc2(content.dairy_cow) { stealCowbell(it.loc) }
        onOpLocU(content.dairy_cow) { mes("The cow doesn't want that.") }
        onOpLocU(content.dairy_cow, objs.bucket_empty) { attemptMilkingCow(it.loc) }
        onOpHeld1(cow_objs.cowbells) { resolveCowbellPlay() }

        onPlayerQueue(cow_queues.milk) { milkCow() }
    }

    private suspend fun ProtectedAccess.attemptMilkingCow(loc: BoundLocInfo) {
        arriveDelay()
        faceSquare(loc.coords)
        if (objs.bucket_empty !in inv) {
            if (loc.coords.isNearGillie()) {
                startDialogue { noBucket() }
            } else {
                mes("You'll need an empty bucket to collect the milk.")
            }
            return
        }
        weakQueue(cow_queues.milk, 2)
    }

    private fun ProtectedAccess.milkCow() {
        val replace = invReplace(inv, objs.bucket_empty, 1, objs.bucket_of_milk)
        if (replace.failure) {
            return
        }
        spam("You milk the cow.")
        anim(seqs.milkit)
        soundSynth(synths.milk_cow)
        weakQueue(cow_queues.milk, 8)
    }

    private suspend fun Dialogue.noBucket() {
        chatNpcSpecific(
            "Gillie Groats the Milkmaid",
            cow_npcs.gillie_the_milkmaid,
            laugh,
            "Tee hee! You've never milked a cow before, have you?",
        )
        chatPlayer(quiz, "Erm... No. How could you tell?")
        chatNpcSpecific(
            "Gillie Groats the Milkmaid",
            cow_npcs.gillie_the_milkmaid,
            laugh,
            "Because you're spilling milk all over the floor. What a " +
                "waste! You need something to hold the milk.",
        )
        chatPlayer(neutral, "Ah yes, I really should have guessed that one, shouldn't I?")
        chatNpcSpecific(
            "Gillie Groats the Milkmaid",
            cow_npcs.gillie_the_milkmaid,
            laugh,
            "You're from the city, aren't you... Try it again with an empty bucket.",
        )
        chatPlayer(neutral, "Right, I'll do that.")
    }

    private suspend fun ProtectedAccess.stealCowbell(loc: BoundLocInfo) {
        arriveDelay()
        faceSquare(loc.coords)
        resolveCowbellTheft()
    }

    public suspend fun ProtectedAccess.resolveCowbellTheft() {
        if (!player.members) {
            mes("You need to be on a members' world to steal a cowbell.")
            return
        }
        if (player.vars[cow_varbits.coldWarProgress] == 0) {
            mesbox("You need to have started the Cold War quest to attempt this.")
            return
        }
        if (player.thievingLvl < COWBELL_LEVEL) {
            mes("You need a Thieving level of $COWBELL_LEVEL to steal a cowbell.")
            return
        }
        if (player.inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        if (actionDelay > player.currentMapClock) {
            return
        }

        anim(seqs.human_pickpocket)
        actionDelay = player.currentMapClock + ATTEMPT_INTERVAL_CYCLES
        if (statRandom(stats.thieving, CHANCE_LOW, CHANCE_HIGH, invisibleBoost = 0)) {
            val result = invAdd(player.inv, cow_objs.cowbells)
            if (result.failure) {
                mes("You don't have enough inventory space.")
                return
            }
            statAdvance(stats.thieving, COWBELL_XP)
            spam("You steal a cowbell.")
            return
        }

        spam("You fail to steal a cowbell.")
        mes("The cow kicks you and stuns you!")
        mes("MOO")
        actionDelay = player.currentMapClock + FAILURE_BLOCK_CYCLES
        stunStates.stun(player, FAILURE_BLOCK_CYCLES)
        resetAnim()
        anim(seqs.stunned_thieving)
        spotanim(spotanims.stunned_thieving)
        queueHit(delay = 1, type = HitType.Typeless, damage = COW_KICK_DAMAGE)
    }

    public fun ProtectedAccess.resolveCowbellPlay() {
        if (player.vars[cow_varbits.penguinTransmog] == 0) {
            mes("You must be wearing the penguin suit to play these.")
            return
        }
        stopAction()
        anim(cow_seqs.cowbellPlay)
    }

    private fun CoordGrid.isNearGillie(): Boolean {
        return this == CoordGrid(0, 50, 51, 54, 8) || this == CoordGrid(0, 50, 51, 52, 11)
    }

    companion object {
        const val COWBELL_LEVEL = 15
        const val COWBELL_XP = 16.0
        const val CHANCE_LOW = 128
        const val CHANCE_HIGH = 200
        const val ATTEMPT_INTERVAL_CYCLES = 2
        const val FAILURE_BLOCK_CYCLES = 8
        const val COW_KICK_DAMAGE = 1
    }
}
