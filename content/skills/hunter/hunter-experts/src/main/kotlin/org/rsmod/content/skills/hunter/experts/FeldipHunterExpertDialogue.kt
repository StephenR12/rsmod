package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.entity.Npc

public class FeldipHunterExpertDialogue
@Inject
constructor(
    private val grants: HunterExpertKitGrant,
    private val skillcapes: HunterExpertSkillcapePurchase,
) {
    public suspend fun talk(access: ProtectedAccess, npc: Npc) {
        access.startDialogue(npc) {
            if (
                access.player.statBase(stats.hunter) >= HunterExpertSkillcapePurchase.HUNTER_LEVEL
            ) {
                masterMenu()
            } else {
                standardOpening()
            }
        }
    }

    private suspend fun Dialogue.standardOpening() {
        chatNpc(
            neutral,
            "Whoah there, stranger, careful where you're walking. I almost clobbered you " +
                "there; thought you were a larupia or something.",
        )
        standardMenu()
    }

    private suspend fun Dialogue.standardMenu() {
        when (
            choice5(
                "What are you doing here?",
                1,
                "What's a larupia?",
                2,
                "What is that cool cape you're wearing?",
                3,
                "Ahh, leave me alone you crazy killing person.",
                4,
                KIT_OPTION,
                5,
            )
        ) {
            1 -> activity()
            2 -> larupiaClothing()
            3 -> capeInformation()
            4 -> goodbye()
            5 -> hunterKit()
        }
    }

    private suspend fun Dialogue.activity() {
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(
            neutral,
            "Me? Oh, I'm just having some fun, capturing wild animals, living on the edge, " +
                "stuff like that.",
        )
        chatNpc(
            neutral,
            "Hey, want me to teach you how to catch the critters around here? Ain't come " +
                "across anything yet that I couldn't capture.",
        )
        activityMenu()
    }

    private suspend fun Dialogue.activityMenu() {
        when (
            choice5(
                "Okay, where's a good place to start?",
                1,
                "What sort of things are there to catch around here?",
                2,
                "How can I improve my chances of making a capture?",
                3,
                "Where can I get clothes and equipment like yours?",
                4,
                "I think I'll give it a miss right now.",
                5,
            )
        ) {
            1 -> beginnerBirds()
            2 -> creatures()
            3 -> captureChance()
            4 -> equipment()
            5 -> {
                chatPlayer(neutral, "I think I'll give it a miss right now, thanks.")
                chatNpc(neutral, "Suit yourself!")
            }
        }
    }

    private suspend fun Dialogue.beginnerBirds() {
        chatPlayer(quiz, "Ok, where's a good place to start?")
        chatNpc(
            neutral,
            "Your easiest bet around here is probably to go for some of the birds. Go for the " +
                "crimson swifts; they're the bright red ones that hang around near the coast. " +
                "They're as obliging as can be, or maybe they're just",
        )
        chatNpc(
            neutral,
            "plain dumb, but either way they just seem to throw themselves into traps half " +
                "the time.",
        )
        chatPlayer(quiz, "What sort of traps should I use?")
        chatNpc(
            neutral,
            "Just use a standard bird snare and you'll be fine. You can get them in any decent " +
                "shop that sells Hunter gear.",
        )
        chatPlayer(quiz, "How do they work, then?")
        birdSnareExplanation(neatComma = false)
        chatNpc(neutral, "So, was there anything else you wanted to ask about?")
        when (
            choice2(
                "What other sorts of things are there to catch around here?",
                1,
                "Thanks for your help.",
                2,
            )
        ) {
            1 -> creatures()
            2 -> thanks()
        }
    }

    private suspend fun Dialogue.creatures() {
        chatPlayer(quiz, "What sort of things are there to catch around here?")
        chatNpc(
            neutral,
            "What're you interested in? You've got anything from birds for beginners up to " +
                "larupias for the pros, not to mention weasels, butterflies, barb-tails and " +
                "chinchompas.",
        )
        creatureMenu()
    }

    private suspend fun Dialogue.creatureMenu() {
        when (
            choice4(
                "How would I go about catching birds?",
                1,
                "How do I get hold of a weasel?",
                2,
                "Well, seeing as you seem to like larupias so much...",
                3,
                "More...",
                4,
            )
        ) {
            1 -> birdTutorial()
            2 -> weaselTutorial()
            3 -> larupiaTutorial()
            4 -> moreCreatureMenu()
        }
    }

    private suspend fun Dialogue.moreCreatureMenu() {
        when (
            choice4(
                "Barb-tails sound interesting.",
                1,
                "What on earth is a chinchompa?",
                2,
                "Somehow butterflies don't strike me as your sort of thing.",
                3,
                "Back...",
                4,
            )
        ) {
            1 -> barbTailTutorial()
            2 -> chinchompaTutorial()
            3 -> butterflyTutorial()
            4 -> creatureMenu()
        }
    }

    private suspend fun Dialogue.birdTutorial() {
        chatPlayer(quiz, "How would I go about catching birds?")
        chatNpc(
            neutral,
            "There are a couple of varieties that you can try your hand at catching around " +
                "here, but the technique for both is essentially the same. You need to set up " +
                "a bird snare.",
        )
        chatPlayer(quiz, "How do they work, then?")
        birdSnareExplanation(neatComma = true)
        chatNpc(neutral, "So, was there anything else you wanted to ask about?")
        creatureFollowup()
    }

    private suspend fun Dialogue.birdSnareExplanation(neatComma: Boolean) {
        chatNpc(
            neutral,
            "Well, they're kind of sneaky, really. You've got a tall post with what appears to " +
                "be a little perch sticking out the side. Now, the bird will see this as " +
                "somewhere to sit, but the perch is actually rigged such that when the bird",
        )
        chatNpc(neutral, "lands on it, it'll drop away.")
        chatNpc(
            neutral,
            "Further, as the perch drops, it releases a noose that tightens around the bird's " +
                "feet and captures it. ${if (neatComma) "Neat, huh?" else "Neat huh?"}",
        )
    }

    private suspend fun Dialogue.weaselTutorial() {
        chatPlayer(quiz, "How do I get hold of a weasel?")
        chatNpc(
            neutral,
            "Not with large quantities of rice and treacle as many people would have you " +
                "believe. Besides, the Feldip weasel isn't technically a weasel, but, as is " +
                "typical with any sort of colloquial name, it's been called that often enough",
        )
        chatNpc(neutral, "now that it's stuck.")
        chatNpc(
            neutral,
            "Anyway, the Feldip weasel's main survival technique is in hiding; other than that, " +
                "it's pretty stupid. So, if you can actually find one, then you've probably got " +
                "a fairly good chance of catching one.",
        )
        chatPlayer(quiz, "And how would I actually go about finding one?")
        chatNpc(
            neutral,
            "The best place to start would probably be just south of here. You'll find lots of " +
                "kebbit burrows there. Take a close look at some of them, follow the trails and " +
                "you can't go far wrong.",
        )
        creatureFollowup()
    }

    private suspend fun Dialogue.larupiaTutorial() {
        chatPlayer(quiz, "Well, seeing as you seem to like larupias so much...")
        chatNpc(neutral, "Ah yes, yes, larupias are fun; just think of them as big kittens.")
        chatPlayer(neutral, "Uh huh, big kittens that just want to rip your throat out!")
        chatNpc(
            neutral,
            "Indeed, and you can use this to your advantage. If a larupia is keen to chase you " +
                "then let it; that way you can happily lead it straight into your trap without " +
                "any fuss or waiting around.",
        )
        chatPlayer(quiz, "Into what sort of trap should I lead it?")
        chatNpc(
            neutral,
            "A pit trap should do you. There are some appropriate pits to the east of here that " +
                "you can use. Cover one with a few logs and branches, tease the kitty and then " +
                "lead it to the pit. Simple.",
        )
        creatureFollowup()
    }

    private suspend fun Dialogue.barbTailTutorial() {
        chatPlayer(quiz, "Barb-tails sound interesting.")
        chatNpc(
            neutral,
            "They do sound interesting, don't they? Unfortunately, they're actually just " +
                "another type of kebbit; those things get everywhere. Still, the tails of these " +
                "particular kebbits can be useful for fishing.",
        )
        chatPlayer(quiz, "So, how would I go about catching one them?")
        chatNpc(neutral, "Just squash them with a boulder, quick and simple.")
        chatPlayer(
            quiz,
            "What, you mean just pick up some boulders and start throwing them at kebbits? That " +
                "doesn't sound simple.",
        )
        chatNpc(
            neutral,
            "No silly, you let the creature squash itself. Just use some sticks to hold a " +
                "boulder in a nice precarious position, place some bait underneath and let the " +
                "creature knock itself out.",
        )
        creatureFollowup()
    }

    private suspend fun Dialogue.chinchompaTutorial() {
        chatPlayer(quiz, "What on earth is a chinchompa?")
        chatNpc(
            neutral,
            "Awwww, chinchompas are great. They're highly volatile little balls of fluff. Cute " +
                "as you like, but for some reason they do have this tendency to spontaneously " +
                "combust.",
        )
        chatPlayer(quiz, "What? Why?")
        chatNpc(
            neutral,
            "Probably something to do with the fact they'll eat anything and everything, " +
                "especially spicy food.",
        )
        chatNpc(
            neutral,
            "Anyways, if you want to capture a chinchompa, I suggest using a box trap. That way, " +
                "if the little fluffball does explode, then at least most of the blast will be " +
                "contained within the box.",
        )
        creatureFollowup()
    }

    private suspend fun Dialogue.butterflyTutorial() {
        chatPlayer(quiz, "Somehow butterflies don't strike me as your sort of thing.")
        chatNpc(
            neutral,
            "Not really, but some people like them. The ones around here are at least vaguely " +
                "interesting, though. The black ones are supposedly magical butterflies.",
        )
        chatPlayer(quiz, "Doesn't that make them harder to catch?")
        chatNpc(
            neutral,
            "Not particularly; you can still catch them using an ordinary butterfly net. Oh, " +
                "and you'll probably need some butterfly jars in which to keep them as well.",
        )
        creatureFollowup()
    }

    private suspend fun Dialogue.creatureFollowup() {
        when (
            choice4(
                "I'd like to ask about another creature...",
                1,
                "How can I improve my chances of making a capture?",
                2,
                "Where can I get clothes and equipment like yours?",
                3,
                "Thanks for your help.",
                4,
            )
        ) {
            1 -> creatureMenu()
            2 -> captureChance()
            3 -> equipment()
            4 -> thanks()
        }
    }

    private suspend fun Dialogue.captureChance() {
        chatPlayer(quiz, "How can I improve my chances of making a capture?")
        chatNpc(
            neutral,
            "There are four key factors that will affect your chances: what bait you use, how " +
                "close to the trap you are, your appearance and your smell.",
        )
        captureChanceMenu()
    }

    private suspend fun Dialogue.captureChanceMenu() {
        when (
            choice4(
                "How does bait help?",
                1,
                "Why is it important how close I am to the trap?",
                2,
                "Why would the creatures care about my appearance?",
                3,
                "What's wrong with my smell!?",
                4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "How does bait help?")
                chatNpc(
                    neutral,
                    "Unfortunately, creatures don't just jump into traps for the fun of it " +
                        "nearly as often as we as Hunters might like. However, it is possible to " +
                        "improve those odds. If you can find the right bait to use on your trap,",
                )
                chatNpc(
                    neutral,
                    "something your prey really craves, then hunger overrides fear and the " +
                        "creature is just that little bit more likely to bounce to its furry " +
                        "little doom.",
                )
                captureChanceMenu()
            }
            2 -> {
                chatPlayer(quiz, "Why is it important how close I am to the trap?")
                chatNpc(
                    neutral,
                    "However well you might be blending in, if you're standing with your nose " +
                        "pressed right up against your own trap, the poor little critters ain't " +
                        "going to go anywhere near it. So, you need to make sure you put",
                )
                chatNpc(
                    neutral,
                    "a sensible distance - I'd say at least four strides - between yourself " +
                        "and your trap.",
                )
                captureChanceMenu()
            }
            3 -> {
                chatPlayer(quiz, "Why would the creatures care about my appearance?")
                chatNpc(
                    neutral,
                    "You're a human. From the point of view of a lot of creatures, you're big " +
                        "and scary. So, it'd be best if the furry little monsters didn't notice " +
                        "you. You need to blend in like the creatures themselves.",
                )
                chatPlayer(quiz, "And how do I do that?")
                chatNpc(
                    neutral,
                    "Easy, dress up like one of them. Even easier, dress up using the furs of " +
                        "the creatures themselves; then you're bound to get the colour right. " +
                        "And, I suppose, if you don't fancy just slinging a dead fur over your",
                )
                chatNpc(
                    neutral,
                    "shoulders, you can probably get them made into something more appealing. " +
                        "I hear there are tailors in Varrock who are good at that sort of thing.",
                )
                captureChanceMenu()
            }
            4 -> {
                chatPlayer(quiz, "What's wrong with my smell!?")
                chatNpc(
                    neutral,
                    "I'm sure there's nothing wrong with your smell...from a human point of " +
                        "view. But to your prey, any trap that you've set up will reek of human, " +
                        "and that's going to work against you if you don't do anything about it.",
                )
                chatPlayer(quiz, "So what should I do?")
                chatNpc(
                    neutral,
                    "You need to mask that lovely human scent of yours with something else. " +
                        "Smoke works well. Take a lit torch and waft it around the trap a bit. " +
                        "You'll quickly overpower most of the other smells around the trap.",
                )
                captureChanceMenu()
            }
        }
    }

    private suspend fun Dialogue.equipment() {
        chatPlayer(quiz, "Where can I get clothes and equipment like yours?")
        chatNpc(
            neutral,
            "Well, the equipment isn't too hard to get hold of. You've got specialist shops " +
                "like Aleck's in Yanille that'll see you right. He's a bit pricey, mind, but " +
                "it's pretty decent kit.",
        )
        chatNpc(
            neutral,
            "The clothing is a tad trickier to get hold of, the materials required being rather " +
                "difficult to seperate from their reluctant owners.",
        )
        chatNpc(
            neutral,
            "But, if you can get the furs, there are shops like the tailor's in east Varrock " +
                "that can help you.",
        )
    }

    private suspend fun Dialogue.thanks() {
        chatPlayer(happy, "Thanks for your help.")
        chatNpc(neutral, "Not a problem, any time.")
    }

    private suspend fun Dialogue.larupiaClothing() {
        chatPlayer(quiz, "What's a larupia?")
        chatNpc(
            neutral,
            "What's a larupia? You don't know? You mean you really don't recognise the " +
                "significance of these clothes I'm wearing?",
        )
        chatPlayer(neutral, "Well, they're certainly decorative...what about them?")
        chatNpc(
            neutral,
            "Well, these are the furs from a larupia. Caught the creature myself I did, and " +
                "these make me blend in better, see?",
        )
        chatNpc(
            neutral,
            "If you want to be successful as a hunter you've got to be as stealthy as a kyatt, " +
                "as quiet as a mouse and blend in like a, well, like a larupia, I guess!",
        )
        when (
            choice3(
                "So, what's good to hunt around here?",
                1,
                "How can I improve my chances of making a capture?",
                2,
                "Where can I get clothes and equipment like yours?",
                3,
            )
        ) {
            1 -> creatures()
            2 -> captureChance()
            3 -> equipment()
        }
    }

    private suspend fun Dialogue.capeInformation() {
        chatPlayer(quiz, "What is that cool cape you're wearing?")
        chatNpc(
            neutral,
            "This? It's a Hunter's Skillcape. It shows that I am a true master of the art of " +
                "Hunting and provides five daily teleports to either the Feldip Hills or " +
                "Wilderness Hunter areas. If you ever manage to train your",
        )
        chatNpc(neutral, "Hunter skill to its maximum level then I could sell you one.")
    }

    private suspend fun Dialogue.goodbye() {
        chatPlayer(neutral, "Ahh, leave me alone you crazy killing person.")
        chatNpc(neutral, "Heh, well I guess it's not everyone's thing.")
    }

    private suspend fun Dialogue.masterMenu() {
        when (
            choice2(
                "Skillcape of Hunter.",
                1,
                "Hunting.",
                2,
                title = "What would you like to talk about?",
            )
        ) {
            1 -> skillcape()
            2 -> standardOpening()
        }
    }

    private suspend fun Dialogue.skillcape() {
        chatPlayer(quiz, "Can I buy a Hunter's Skillcape from you?")
        chatNpc(
            neutral,
            "I would be delighted to recognise you as a master hunter, the cape provides five " +
                "daily teleports to either the Feldip Hills or Wilderness Hunter areas. I will " +
                "need 99000 coins to cover the expense of making such a",
        )
        chatNpc(neutral, "costly item!")
        when (
            choice2("99000! That seems a bit expensive.", 1, "Certainly, I'm happy to pay that.", 2)
        ) {
            1 -> {
                chatPlayer(quiz, "99000! That seems a bit expensive.")
                chatNpc(
                    neutral,
                    "Actually it's quite reasonable; it's made from the fur of the nearly " +
                        "extinct Raharni wildcat. Well, if you change your mind you know where " +
                        "to find me.",
                )
            }
            2 -> buySkillcape()
        }
    }

    private suspend fun Dialogue.buySkillcape() {
        chatPlayer(happy, "Certainly, I'm happy to pay that.")
        when (skillcapes.purchase(access)) {
            HunterSkillcapePurchaseResult.Purchased ->
                chatNpc(happy, "Now you look like a true hunter!")
            HunterSkillcapePurchaseResult.InsufficientCoins -> {
                chatPlayer(sad, "But it seems that I don't have enough money with me.")
                chatNpc(neutral, "Well, come back and see me when you do.")
            }
            HunterSkillcapePurchaseResult.NoInventorySpace ->
                chatNpc(
                    neutral,
                    "Unfortunately all Skillcapes are only available with a free hood, it's " +
                        "part of a skill promotion deal; buy one get one free, you know. So " +
                        "you'll need to free up some inventory space before I can sell you one.",
                )
            HunterSkillcapePurchaseResult.NotEligible -> Unit
        }
    }

    private suspend fun Dialogue.hunterKit() {
        chatPlayer(quiz, KIT_OPTION)
        if (grants.hasClaimed(access)) {
            alreadyClaimed()
            return
        }
        chatNpc(neutral, "I do have a spare Hunter kit lying around, you can have it for free.")
        when (grants.claim(access)) {
            HunterExpertKitClaim.Granted -> {
                objbox(objs.hunter_kit, "Hunting expert gives you a Hunter kit.")
                chatPlayer(happy, "Thanks! I will put this to good use.")
                chatNpc(neutral, "Is there anything else I can help you with?")
                standardMenu()
            }
            HunterExpertKitClaim.AlreadyClaimed -> alreadyClaimed()
            HunterExpertKitClaim.NoInventorySpace -> mesbox(constants.dm_invspace)
        }
    }

    private suspend fun Dialogue.alreadyClaimed() {
        chatNpc(
            neutral,
            "I don't have any more Hunter kits lying around, sorry. Maybe you could try one of " +
                "the many shops that sell hunting traps, you can find one in Yanille.",
        )
        chatNpc(neutral, "Is there anything else I can help you with?")
        standardMenu()
    }

    private companion object {
        const val KIT_OPTION = "Can you give me some hunting traps?"
    }
}
