package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerGnomeCookingSuccess
import org.rsmod.api.player.events.interact.NpcConversation
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RestaurantRestrictedInterfaces : InterfaceReferences() {
    val assault = listOf("att", "col", "def", "heal").map { find("barbassault_over_$it") }
}

internal object RestaurantNpcs : NpcReferences() {
    val junior = find("aluft_gnome_delivery_controller")
    val chef = find("aluft_gianne")
    val barman = find("blurberry")
    val courier = find("aluft_delivery_mounted_terrorchic")
}

internal object RestaurantVars : VarpReferences() {
    val training = find("gnome_restaurant_training")
    val refusal = find("gnome_restaurant_refused_at")
    val started = find("aluft_minigame_start_time")
}

internal object RestaurantSaves : VarpBuilder() {
    init {
        for (n in listOf("gnome_restaurant_training", "gnome_restaurant_refused_at")) build(n) {
            transmitNever = true
        }
    }
}

internal object RestaurantBits : VarBitReferences() {
    val active = find("aluft_minigame_started")
    val time = find("aluft_time_allowed")
    val item = find("aluft_current_order")
    val customer = find("aluft_current_customer")
    val credits = find("aluft_credits")
    val meals = find("aluft_player_delivery")
    val played = find("aluft_played_before")
    val reward = find("aluft_training_reward")
    val easy = find("collection_aluft_easy_deliveries")
    val hard = find("collection_aluft_hard_deliveries")
    val wgs = find("wgs")
    val monkey = find("mm2_progress")
}

internal object RestaurantItems : ObjReferences() {
    val box = find("aluft_delivery_box")
    val token = find("aluft_free_delivery_token")
    val dough = find("cert_gianne_dough")

    data class Lesson(val output: ObjType, val kit: List<Pair<ObjType, Int>>)

    private fun lesson(name: String, vararg kit: Pair<String, Int>) =
        Lesson(find(name), kit.map { find(it.first) to it.second })

    val books = listOf(find("giannes_cook_book"), find("cocktail_guide"))
    val lessons =
        listOf(
            listOf(
                lesson(
                    "toad_crunchies",
                    "toads_legs" to 2,
                    "gnome_spice" to 1,
                    "equa_leaves" to 1,
                    "gianne_dough" to 1,
                    "crunchy_tray" to 1,
                ),
                lesson(
                    "chocchip_crunchies",
                    "chocolate_bar" to 2,
                    "chocolate_dust" to 1,
                    "gianne_dough" to 1,
                    "gnome_spice" to 1,
                ),
                lesson(
                    "toad_batta",
                    "cheese" to 1,
                    "gianne_dough" to 1,
                    "equa_leaves" to 1,
                    "toads_legs" to 1,
                    "gnome_spice" to 1,
                    "batta_tin" to 1,
                ),
                lesson(
                    "cheese+tom_batta",
                    "gianne_dough" to 1,
                    "gnome_spice" to 1,
                    "cheese" to 1,
                    "tomato" to 1,
                    "equa_leaves" to 1,
                ),
            ),
            listOf(
                lesson(
                    "fruit_blast",
                    "lemon" to 2,
                    "orange" to 1,
                    "pineapple" to 1,
                    "cocktail_shaker" to 1,
                    "cocktail_glass_empty" to 1,
                    "knife" to 1,
                ),
                lesson(
                    "pineapple_punch",
                    "pineapple" to 3,
                    "orange" to 2,
                    "lemon" to 1,
                    "lime" to 1,
                    "cocktail_glass_empty" to 1,
                ),
                lesson(
                    "wizard_blizzard",
                    "vodka" to 1,
                    "gin" to 1,
                    "lemon" to 1,
                    "pineapple" to 1,
                    "lime" to 2,
                    "orange" to 1,
                    "cocktail_glass_empty" to 1,
                ),
                lesson(
                    "sgg",
                    "vodka" to 1,
                    "lime" to 4,
                    "equa_leaves" to 1,
                    "cocktail_glass_empty" to 1,
                ),
            ),
        )
    val orders =
        listOf(
                "toad_crunchies",
                "spicy_crunchies",
                "worm_crunchies",
                "chocchip_crunchies",
                "fruit_batta",
                "toad_batta",
                "worm_batta",
                "vegetable_batta",
                "cheese+tom_batta",
                "worm_hole",
                "veg_ball",
                "tangled_toads_legs",
                "chocolate_bomb",
                "fruit_blast",
                "pineapple_punch",
                "wizard_blizzard",
                "sgg",
                "drunk_dragon",
                "chocolate_saturday",
                "blurberry_special",
            )
            .map { find(it) }
}

private data class Customer(
    val name: String,
    val tile: CoordGrid,
    val hard: Boolean,
    val variants: List<Int>,
) {
    fun at(player: Player): CoordGrid =
        if (name == "Garkor" && player.vars[RestaurantBits.monkey] >= 195) CoordGrid(2479, 3463, 1)
        else tile

    fun near(player: Player, npc: Npc): Boolean {
        val center = at(player)
        return npc.level == center.level &&
            npc.coords.chebyshevDistance(center) <=
                when (name) {
                    "Wingstone" -> 85
                    "Gnome Coach",
                    "Penwie" -> 20
                    else -> 10
                }
    }
}

private val customers: List<Customer> by lazy {
    requireNotNull(RestaurantItems.javaClass.getResourceAsStream("gnome-customers.tsv"))
        .bufferedReader()
        .useLines { lines ->
            lines
                .map { line ->
                    val (name, x, z, l) = line.split('\t')
                    val cols = line.split('\t')
                    Customer(
                        name,
                        CoordGrid(x.toInt(), z.toInt(), l.toInt()),
                        cols[4] == "hard",
                        cols[5].split(',').map { it.toInt() },
                    )
                }
                .toList()
        }
}

public class GnomeRestaurant
@Inject
constructor(
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val items: ObjTypeList,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(RestaurantNpcs.chef) { teach(0) }
        onOpNpc1(RestaurantNpcs.barman) { teach(1) }
        onEvent<PlayerGnomeCookingSuccess> {
            for (track in 0..1) {
                val phase = phase(player, track)
                if (
                    phase in 2..8 &&
                        phase % 2 == 0 &&
                        RestaurantItems.lessons[track][(phase - 2) / 2].output.id == output.id
                )
                    setPhase(player, track, phase + 1)
            }
        }
        onOpNpc1(RestaurantNpcs.junior) { welcome() }
        onOpNpc3(RestaurantNpcs.junior) { assign(false) }
        onOpNpc4(RestaurantNpcs.junior) { assign(true) }
        onOpHeld1(RestaurantItems.box) { if (RestaurantItems.box in inv) checkBox() }
        onOpHeld1(RestaurantItems.token) {
            mes(
                "Your token entitles you to ${vars[RestaurantBits.meals]} item(s) of gnome cuisine."
            )
        }
        onOpHeld2(RestaurantItems.token) { redeem() }
        // Customer Talk-to is also used by shops, pilots and quests. The normal conversation
        // continues unless this customer is the one on the player's active delivery box.
        for ((index, customer) in customers.withIndex()) for (id in customer.variants) {
            onProtectedEvent<NpcConversation>(id) {
                if (
                    vars[RestaurantBits.active] != 0 &&
                        vars[RestaurantBits.customer] == index + 1 &&
                        customer.near(player, it.npc)
                ) {
                    it.handled = true
                    deliver(index)
                }
            }
            val npc = types.getValue(id)
            onOpNpcU(npc, RestaurantItems.box) { if (customer.near(player, it.npc)) deliver(index) }
            for (food in RestaurantItems.orders) onOpNpcU(npc, food) {
                if (customer.near(player, it.npc) && order()?.id == food.id) deliver(index)
            }
        }
    }

    private fun phase(p: Player, track: Int) =
        (p.vars[RestaurantVars.training] ushr (track * 4)) and 15

    private fun setPhase(p: Player, track: Int, value: Int) {
        val shift = track * 4
        val next = (p.vars[RestaurantVars.training] and (15 shl shift).inv()) or (value shl shift)
        VarPlayerIntMapSetter.set(p, RestaurantVars.training, next)
    }

    private fun ProtectedAccess.teach(track: Int) {
        if (!player.members) return
        val phase = phase(player, track)
        if (phase == 0) {
            val book = RestaurantItems.books[track]
            if (book !in inv && !invAdd(inv, book).success) return
            setPhase(player, track, 1)
            mes("Take this recipe book, then speak to me about your first assignment.")
            return
        }
        if (phase >= 10) {
            mes("You have my recommendation. Speak to Gianne jnr. about a job.")
            rewardDough()
            return
        }
        if (phase in 2..8 && phase % 2 == 0) {
            val lesson = RestaurantItems.lessons[track][(phase - 2) / 2]
            mes("Prepare ${items[lesson.output].name} yourself, then bring it back to me.")
            return
        }
        val next = if (phase == 1) 0 else (phase - 3) / 2 + 1
        val prior = if (phase == 1) null else RestaurantItems.lessons[track][next - 1].output
        val lesson = RestaurantItems.lessons[track].getOrNull(next)
        if (
            !player
                .invTransaction(inv) {
                    val bag = select(inv)
                    if (prior != null) delete(bag, prior.id, 1)
                    if (lesson != null) for ((item, count) in lesson.kit) add(bag, item.id, count)
                }
                .success
        ) {
            mes("Bring the finished dish and leave room for your training supplies.")
            return
        }
        if (lesson == null) {
            setPhase(player, track, 10)
            mes("You've finished your training and earned my recommendation.")
            rewardDough()
        } else {
            setPhase(player, track, 2 + 2 * next)
            mes(
                "I've given you the ingredients. Prepare ${items[lesson.output].name} and bring it to me."
            )
        }
    }

    private fun ProtectedAccess.rewardDough() {
        if (
            phase(player, 0) == 10 &&
                phase(player, 1) == 10 &&
                vars[RestaurantBits.reward] == 0 &&
                invAdd(inv, RestaurantItems.dough, 100).success
        ) {
            vars[RestaurantBits.reward] = 1
            mes("You receive a voucher for 100 balls of Gianne dough.")
        }
    }

    private fun ProtectedAccess.qualified(): Boolean {
        if (!player.members || phase(player, 0) != 10 || phase(player, 1) != 10) {
            mes("Gianne jnr. needs recommendations from Aluft Gianne snr. and Blurberry first.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.welcome() {
        if (!qualified()) return
        rewardDough()
        replaceToken()
        when (
            menu(
                "Aluft Aloft delivery service",
                true,
                listOf(
                    "Take an easy order.",
                    "Take a hard order.",
                    "Check or replace my delivery box.",
                    "Decline my current order.",
                    "Leave.",
                ),
            )
        ) {
            0 -> assign(false)
            1 -> assign(true)
            2 -> {
                if (vars[RestaurantBits.active] != 0 && RestaurantItems.box !in inv)
                    invAdd(inv, RestaurantItems.box)
                checkBox()
            }
            3 ->
                if (vars[RestaurantBits.active] != 0) {
                    if (vars[RestaurantBits.time] == 11) vars[RestaurantVars.refusal] = now() + 330
                    clearOrder()
                    mes("Gianne jnr. takes the order off the board.")
                }
        }
    }

    private fun ProtectedAccess.assign(hard: Boolean) {
        if (!qualified()) return
        expired()
        if (vars[RestaurantBits.active] != 0 && vars[RestaurantBits.meals] < 10) {
            checkBox()
            return
        }
        if (hard && now() < vars[RestaurantVars.refusal]) {
            mes(
                "You must wait a little longer for another hard delivery, or complete an easy delivery."
            )
            return
        }
        if (RestaurantItems.box !in inv && !invAdd(inv, RestaurantItems.box).success) return
        val options =
            customers.indices.filter {
                customers[it].hard == hard &&
                    (customers[it].name != "Hazelmere" || vars[RestaurantBits.wgs] < 900)
            }
        val index = random.pick(options)
        vars[RestaurantBits.customer] = index + 1
        vars[RestaurantBits.item] = random.of(RestaurantItems.orders.size) + 1
        vars[RestaurantBits.time] = if (hard) 11 else 6
        vars[RestaurantBits.active] = 1
        vars[RestaurantBits.played] = 1
        vars[RestaurantVars.started] = now()
        checkBox()
    }

    private fun ProtectedAccess.order() =
        RestaurantItems.orders.getOrNull(vars[RestaurantBits.item] - 1)

    private fun ProtectedAccess.clearOrder() {
        if (RestaurantItems.box in inv) invDel(inv, RestaurantItems.box)
        vars[RestaurantBits.active] = 0
        vars[RestaurantBits.customer] = 0
        vars[RestaurantBits.item] = 0
        vars[RestaurantBits.time] = 0
        vars[RestaurantVars.started] = 0
    }

    private fun ProtectedAccess.expired(): Boolean {
        if (
            vars[RestaurantBits.active] == 0 ||
                now() - vars[RestaurantVars.started] < vars[RestaurantBits.time] * 60
        )
            return false
        clearOrder()
        mes("Your delivery order has expired.")
        return true
    }

    private fun ProtectedAccess.checkBox() {
        if (expired()) return
        val customer = customers.getOrNull(vars[RestaurantBits.customer] - 1)
        val food = order()
        if (vars[RestaurantBits.active] == 0 || customer == null || food == null) {
            clearOrder()
            mes("You are not currently playing the game, so you don't need this box!")
            return
        }
        val seconds = vars[RestaurantVars.started] + vars[RestaurantBits.time] * 60 - now()
        mes(
            "Deliver ${items[food].name} to ${customer.name}. You have ${seconds/60}m ${seconds%60}s remaining."
        )
        mes(
            "The customer is near ${customer.at(player).x}, ${customer.at(player).z}, floor ${customer.at(player).level}."
        )
    }

    private fun ProtectedAccess.deliver(index: Int) {
        if (
            !player.members ||
                expired() ||
                vars[RestaurantBits.active] == 0 ||
                vars[RestaurantBits.customer] != index + 1
        )
            return
        val food = order() ?: return
        if (RestaurantItems.box !in inv || food !in inv) {
            mes("Bring your Aluft aloft box and the requested handmade ${items[food].name}.")
            return
        }
        val customer = customers[index]
        val reward =
            if (customer.hard) hardTip(customer.name) else objs.coins.id to random.of(2, 500)
        if (
            !player
                .invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, RestaurantItems.box.id, 1)
                    delete(bag, food.id, 1)
                    add(bag, reward.first, reward.second)
                }
                .success
        ) {
            mes("Make room for your customer's tip.")
            return
        }
        val count = if (customer.hard) RestaurantBits.hard else RestaurantBits.easy
        vars[count] = (vars[count] + 1).coerceAtMost(65535)
        if (!customer.hard) vars[RestaurantVars.refusal] = 0
        val total =
            (vars[RestaurantBits.meals] * 12 +
                    vars[RestaurantBits.credits] +
                    if (customer.hard) 3 else 1)
                .coerceAtMost(120)
        vars[RestaurantBits.meals] = total / 12
        vars[RestaurantBits.credits] = total % 12
        clearOrder()
        replaceToken()
        player.completeDiaryTask(DiaryTask.DeliverGnomeFood)
        mes("${customer.name} accepts your delivery and gives you a tip.")
    }

    private fun ProtectedAccess.replaceToken() {
        if (
            vars[RestaurantBits.meals] > 0 &&
                RestaurantItems.token !in inv &&
                RestaurantItems.token !in bank
        )
            invAdd(inv, RestaurantItems.token)
    }

    private suspend fun ProtectedAccess.redeem() {
        val count = vars[RestaurantBits.meals].coerceIn(0, 10)
        if (!player.members || count == 0 || RestaurantItems.token !in inv) return
        if (RestaurantRestrictedInterfaces.assault.any { it in player.ui }) {
            mes("The delivery gnome cannot reach you here.")
            return
        }
        if (inv.freeSpace() + 1 < count) {
            mes("Make room for $count items of gnome cuisine.")
            return
        }
        mes("A gnome courier is on the way.")
        delay(4)
        if (
            !player
                .invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, RestaurantItems.token.id, 1)
                    repeat(count) { add(bag, random.pick(RestaurantItems.orders).id, 1) }
                }
                .success
        )
            return
        vars[RestaurantBits.meals] = 0
        npcs.add(Npc(types[RestaurantNpcs.courier], coords), 5)
        mes("The courier brings you your order.")
    }

    private fun hardTip(customer: String): Pair<Int, Int> {
        val special =
            when (customer) {
                "Captain Ninto",
                "Captain Daerkin" -> listOf(9470 to 1, 9472 to 1)
                "Wingstone" -> listOf(9469 to 5, 9475 to 1, 4605 to 1)
                "Penwie" -> listOf(9469 to 5, 9475 to 1, 2337 to 1)
                "Brambickle" -> listOf(9469 to 5, 9475 to 1, 3261 to 1)
                "Professor Manglethorp" ->
                    listOf(9469 to 5, 445 to random.of(6, 11), 454 to random.of(12, 22))
                "Professor Imblewyn",
                "Professor Onglewip" ->
                    listOf(
                        563 to random.of(7, 18),
                        561 to random.of(9, 15),
                        562 to random.of(15, 27),
                        564 to random.of(10, 19),
                        560 to random.of(7, 13),
                        565 to random.of(6, 10),
                        566 to random.of(5, 11),
                        7937 to random.of(51, 97),
                    )
                else -> emptyList()
            }
        if (
            special.isNotEmpty() &&
                random.of(1000) < (if (customer.startsWith("Captain")) 60 else 45) * special.size
        )
            return random.pick(special)
        val roll = random.of(128)
        return when (roll) {
            in 0..9 -> 751 to 1
            in 10..19 -> 2998 to 3
            in 20..29 -> 3000 to 1
            in 30..39 -> 1624 to random.of(6, 16)
            in 40..49 -> 1622 to random.of(3, 5)
            in 50..59 -> 1620 to random.of(2, 4)
            in 60..69 -> 1617 to 1
            in 70..79 -> 1628 to random.of(2, 3)
            in 80..89 -> 1626 to 3
            in 90..99 -> 1629 to 1
            in 100..104 -> 5073 to 1
            in 105..108 -> 985 to 1
            in 109..112 -> 987 to 1
            in 113..120 -> 9142 to random.of(16, 35)
            in 121..125 -> 9143 to random.of(12, 26)
            else -> 9144 to random.of(7, 15)
        }
    }

    private fun now() = (System.currentTimeMillis() / 1000).toInt()
}
