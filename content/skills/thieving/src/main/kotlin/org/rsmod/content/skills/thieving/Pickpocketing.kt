package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueReward
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.back
import org.rsmod.api.player.feet
import org.rsmod.api.player.front
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.magic.ShadowVeilStateRepository
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.player.torso
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj

public class Pickpocketing
@Inject
constructor(
    private val coinPouches: CoinPouches,
    private val stunStates: ThievingStunStateRepository,
    private val shadowVeil: ShadowVeilStateRepository,
    private val clueDrops: PickpocketClueDrops,
    private val objRepo: ObjRepository,
) {
    public fun attempt(access: ProtectedAccess, npc: Npc, definition: PickpocketDefinition) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to train Thieving.")
                return
            }
            if (player.thievingLvl < definition.level) {
                mes("You need a Thieving level of ${definition.level} to pick this pocket.")
                return
            }
            if (actionDelay > player.currentMapClock) {
                return
            }
            if (coinPouches.count(this) >= coinPouches.limit(this)) {
                mes("You need to open your coin pouches before you can continue pickpocketing.")
                return
            }
            if (
                player.inv.isFull() &&
                    definition.rewards.none { reward -> player.inv.has(reward.obj) }
            ) {
                mes("You don't have enough inventory space.")
                return
            }

            faceEntitySquare(npc)
            npc.playerFaceClose(player)
            anim(seqs.human_pickpocket)
            actionDelay = player.currentMapClock + SUCCESS_INTERVAL_CYCLES

            val bounds = successBounds(this, definition)
            if (statRandom(stats.thieving, bounds.first, bounds.last, invisibleBoost = 0)) {
                val reward =
                    if (definition.rewards.size == 1) {
                        definition.rewards.single()
                    } else {
                        definition.rewardAt(random.of(definition.totalRewardWeight))
                    }
                val doubled = rogueOutfitDoubles(this)
                if (!grantRewards(this, listOf(reward) + definition.guaranteedRewards, doubled)) {
                    mes("You don't have enough inventory space.")
                    return
                }
                definition.tertiaryDrop?.let { tertiary ->
                    grantTertiaryReward(this, tertiary, doubled)
                }
                definition.clueDrop?.let { clueDrop ->
                    grantClueReward(this, clueDrops.roll(this, clueDrop))
                }
                statAdvance(stats.thieving, definition.xp)
                soundSynth(synths.thieving)
                spam("You pick the ${definition.name}'s pocket.")
                return
            }

            spam("You fail to pick the ${definition.name}'s pocket.")
            if (shadowVeil.protects(player, random)) {
                mes("Your attempt to steal goes unnoticed.")
                return
            }

            npc.say("What do you think you're doing?")
            if (dodgyNecklaceProtects(this)) {
                mes("Your dodgy necklace protects you.")
                return
            }

            actionDelay = player.currentMapClock + definition.pickpocketBlockCycles
            stunStates.stun(player, definition.movementBlockCycles)
            resetAnim()
            anim(seqs.stunned_thieving)
            spotanim(spotanims.stunned_thieving)
            queueHit(npc, delay = 1, type = HitType.Typeless, damage = definition.damage)
        }
    }

    public fun successBounds(access: ProtectedAccess, definition: PickpocketDefinition): IntRange {
        with(access) {
            val diaryBonus =
                player.vars[varbits.ardougne_diary_hard_complete] != 0 ||
                    (definition.mediumArdougneDiaryApplies &&
                        player.vars[varbits.ardougne_diary_medium_complete] != 0)
            val glovesBonus = player.hands?.id == objs.gloves_of_silence.id
            val capeBonus =
                player.back?.id == objs.thieving_skillcape.id ||
                    player.back?.id == objs.thieving_skillcape_t.id ||
                    player.back?.id == objs.max_cape_worn.id
            val firstModifier = if (diaryBonus) 110 else if (glovesBonus) 105 else 100
            val capeModifier = if (capeBonus) 110 else 100
            val low = definition.chanceLow * firstModifier / 100 * capeModifier / 100
            val high = definition.chanceHigh * firstModifier / 100 * capeModifier / 100
            return low..high
        }
    }

    public fun rogueDoubleChance(access: ProtectedAccess): Int {
        with(access) {
            val pieces =
                listOf(
                        player.hat?.id == ThievingObjs.rogueMask.id,
                        player.torso?.id == ThievingObjs.rogueTop.id,
                        player.legs?.id == ThievingObjs.rogueTrousers.id,
                        player.hands?.id == ThievingObjs.rogueGloves.id,
                        player.feet?.id == ThievingObjs.rogueBoots.id,
                    )
                    .count { it }
            return if (pieces == ROGUE_SET_PIECES) 100 else pieces * ROGUE_CHANCE_PER_PIECE
        }
    }

    private fun rogueOutfitDoubles(access: ProtectedAccess): Boolean {
        with(access) {
            val chance = rogueDoubleChance(this)
            return chance == 100 || (chance > 0 && random.of(100) < chance)
        }
    }

    private fun grantRewards(
        access: ProtectedAccess,
        rewards: List<PickpocketReward>,
        doubled: Boolean,
    ): Boolean {
        with(access) {
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    for (reward in rewards) {
                        val rewardCount = reward.rollCount(random)
                        if (reward.coinPouch != null) {
                            add(inventory, reward.obj.id, count = rewardCount)
                            if (doubled) {
                                add(
                                    inventory,
                                    objs.coins.id,
                                    count = reward.coinPouch.rollRogueBonus(random, rewardCount),
                                )
                            }
                        } else {
                            val multiplier = if (doubled) 2 else 1
                            add(inventory, reward.obj.id, count = rewardCount * multiplier)
                        }
                    }
                }
            return transaction.success
        }
    }

    private fun grantClueReward(access: ProtectedAccess, reward: ClueReward?) {
        if (reward == null) {
            return
        }
        with(access) {
            val obj = reward.obj
            if (obj != null) {
                player.invTransaction(player.inv) { add(select(player.inv), obj.id, count = 1) }
            }
            reward.message?.let(::mes)
        }
    }

    private fun grantTertiaryReward(
        access: ProtectedAccess,
        drop: PickpocketTertiaryDrop,
        doubled: Boolean,
    ) {
        with(access) {
            if (random.of(drop.denominator) != 0) {
                return
            }
            val count = if (doubled && drop.rogueDoubles) 2 else 1
            invAddOrDrop(objRepo, drop.obj, count = count)
        }
    }

    private fun dodgyNecklaceProtects(access: ProtectedAccess): Boolean {
        with(access) {
            val necklace = player.front ?: return false
            if (
                necklace.id != objs.dodgy_necklace.id || random.of(100) >= DODGY_PROTECTION_PERCENT
            ) {
                return false
            }
            val charges = if (necklace.vars == 0) DODGY_CHARGES else necklace.vars
            if (charges <= 1) {
                player.front = null
                mes("Your dodgy necklace crumbles to dust.")
            } else {
                player.front = InvObj(objs.dodgy_necklace, necklace.count, vars = charges - 1)
            }
            return true
        }
    }

    public companion object {
        public const val SUCCESS_INTERVAL_CYCLES: Int = 2
        public const val DODGY_PROTECTION_PERCENT: Int = 25
        public const val DODGY_CHARGES: Int = 10
        public const val ROGUE_CHANCE_PER_PIECE: Int = 15
        public const val ROGUE_SET_PIECES: Int = 5
    }
}
