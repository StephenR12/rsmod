package org.rsmod.content.skills.pets

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.content.interfaces.bank.deliverToBank
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object PetVars : VarpReferences() {
    val active = find("skill_pet_following_form")
    val guardian = find("rift_guardian_last_form")
    val forms = find("skill_pet_unlocked_forms")
    val clientFollower = find("follower_npc")
}
internal object PetSaves : VarpBuilder() {
    init {
        build("skill_pet_following_form") { permanent = true }
        build("skill_pet_unlocked_forms") { permanent = true }
        build("rift_guardian_last_form") { permanent = true }
    }
}
internal object PetBits : VarBitReferences() {
    val space = find("follower_space_track")
    val locked = find("skillpet_runecrafting_locked")
}

/** Index in native pet insurance enum 985, with its insurance and menagerie flags. */
internal enum class Species(val index: Int, val insured: Int, val menagerie: Int) {
    Chick(16, 6017, 4446),
    Heron(24,4846,4850),
    Golem(25,4847,4851),
    Beaver(26,4848,4852),
    Chinchompa(27,4849,4853),
    Squirrel(29,2169,2174),
    Tangleroot(30,2170,2175),
    Guardian(31,2171,5345),
    Rocky(32,2172,5346),
    Phoenix(33,5363,4353),
    Herbi(37,5735,5736),
    Smolcano(44,8440,8442),
    Tempor(47,11934,11935),
    Protector(49,13711,13712),
    Quetzin(58,9815,9817)
}
internal data class PetForm(val species: Species, val item: Int, val npc: Int)
internal val guardianForms =
    (0..13).map { PetForm(Species.Guardian, 20665 + 2 * it, 7354 + it) } +
        listOf(PetForm(Species.Guardian, 21990, 8028), PetForm(Species.Guardian, 26899, 11428))
internal val petForms = buildList {
    addAll(guardianForms)
    fun forms(species: Species, vararg pairs: Pair<Int,Int>) {
        pairs.forEach { (item,npc) -> add(PetForm(species,item,npc)) }
    }
    forms(Species.Chick,13071 to 4002)
    forms(Species.Protector,26901 to 11429)
    forms(Species.Phoenix,20693 to 7370,24483 to 3081,24484 to 3082,24485 to 3083,24486 to 3084)
    forms(Species.Herbi,21509 to 7760)
    forms(Species.Smolcano,23760 to 8739)
    forms(Species.Tempor,25602 to 10637)
    forms(Species.Quetzin,28962 to 12858)
    forms(Species.Squirrel,20659 to 7351,24701 to 9637,30151 to 14044)
    forms(Species.Tangleroot,20661 to 7352,24555 to 9497,24557 to 9498,24559 to 9499,24561 to 9500,24563 to 9501)
    forms(Species.Rocky,20663 to 7353,24847 to 9852,24849 to 9853)
    forms(Species.Heron,13320 to 6722,25600 to 10636)
    forms(Species.Golem,13321 to 7451,21187 to 7452,21188 to 7453,21189 to 7454,21190 to 7455,
        21191 to 7642,21192 to 7643,21193 to 7644,21194 to 7645,21195 to 7646,21196 to 7647,
        21197 to 7648,21340 to 7711,21358 to 7739,21359 to 7740,21360 to 7741)
    forms(Species.Beaver,13322 to 12181,28669 to 12549,28670 to 12550)
    for (i in 0..8) forms(Species.Beaver,(28229+i) to (12182+i))
    for (i in 0..3) forms(Species.Chinchompa,(13323+i) to (6756+i))
}

@Singleton
public class SkillPets @Inject constructor(
    private val repository: NpcRepository,
    private val npcs: NpcTypeList,
    private val objects: ObjTypeList,
    private val bits: org.rsmod.game.type.varbit.VarBitTypeList,
    private val collision: CollisionFlagMap,
    private val keeps: org.rsmod.api.death.DeathKeeps,
    private val inventories: org.rsmod.game.type.inv.InvTypeList,
) {
    private val houseNpcByItem by lazy {
        val byName = npcs.values.associateBy { it.internalName }
        petForms.associate { form ->
            val name = npcs.getValue(form.npc).internalName
            val houseName = if (form.species == Species.Chick) "poh_chompybird_pet" else "poh_$name"
            form.item to byName.getValue(houseName).id
        }
    }
    private val followers = mutableMapOf<Player,Npc>()

    internal fun active(player: Player): PetForm? = petForms.firstOrNull { it.item == player.vars[PetVars.active] }
    internal fun owns(player: Player, npc: Npc): Boolean = followers[player] === npc
    internal fun insured(player: Player, species: Species): Boolean = player.vars[bits.getValue(species.insured)] != 0

    internal fun location(player: Player, species: Species): Int {
        if (active(player)?.species == species) return 3
        if (player.vars[bits.getValue(species.menagerie)] != 0) return 4
        val forms = petForms.filter { it.species == species }
        if (forms.any { objects.getValue(it.item) in player.inv }) return 2
        val arena = player.invMap[PetStorage.arena]
        if (arena != null && forms.any { objects.getValue(it.item) in arena }) return 4
        val bank = player.invMap[invs.bank]
        if (bank != null && forms.any { objects.getValue(it.item) in bank }) return 1
        // Death storages and other saved containers also keep the pet in the player's possession.
        return if (player.invMap.values.any { bag -> forms.any { objects.getValue(it.item) in bag } }) 1 else 0
    }
    private fun save(player: Player, form: PetForm) {
        set(player,bits.getValue(form.species.insured),1)
        if (form.species == Species.Guardian) set(player,PetVars.guardian,form.item)
    }
    public fun award(player: Player, item: Int) {
        val form = petForms.first { it.item == item }
        if (!player.members) return
        if (location(player,form.species) != 0) {
            set(player,bits.getValue(form.species.insured),1)
            player.mes("You have a funny feeling like you would have been followed...")
            return
        }
        save(player,form)
        if (active(player) == null && player.vars[PetBits.space] == 0) {
            follow(player, form)
            player.mes("You have a funny feeling like you're being followed.")
        } else if (player.invAdd(player.inv,form.item,1).success) {
            player.mes("You feel something weird sneaking into your backpack.")
        } else {
            player.mes("You didn't have room for your pet. You can reclaim it from Probita.")
        }
    }
    internal fun release(player: Player, form: PetForm, slot: Int) {
        if (!player.members || player.inv[slot]?.id != form.item) return
        if (player.vars[PetBits.space] != 0 || active(player) != null) {
            player.mes("You already have a follower.")
            return
        }
        if (player.invDel(player.inv,objects.getValue(form.item),slot=slot).success) {
            save(player,form)
            follow(player,form)
        }
    }
    internal fun pickup(player: Player, npc: Npc) {
        val form = active(player) ?: return
        if (!owns(player,npc)) return
        if (!player.invAdd(player.inv,form.item,1).success) {
            player.mes("You don't have enough inventory space to pick up your pet.")
            return
        }
        clear(player)
    }
    private fun follow(player: Player, form: PetForm) {
        removeNpc(player)
        val npc = Npc(npcs.getValue(form.npc),player.coords)
        repository.add(npc,Int.MAX_VALUE)
        npc.respawns = false
        npc.playerFollow(player)
        followers[player] = npc
        set(player,PetVars.active,form.item)
        set(player,PetVars.clientFollower,npc.slotId)
        set(player,PetBits.space,1)
    }
    public fun ownsItemFamily(player: Player, item: Int): Boolean =
        petForms.firstOrNull { it.item == item }?.let { location(player,it.species) != 0 } == true

    /** Item and corresponding house NPC, including the colour owned by this player. */
    public data class HousePet(val item: Int, val npc: Int, val index: Int)
    private fun PetForm.housePet() = HousePet(item,houseNpcByItem.getValue(item),species.index)

    public fun housePet(item: Int): HousePet? = petForms.firstOrNull { it.item == item }?.housePet()

    public fun availableForMenagerie(player: Player): List<HousePet> =
        petForms.filter { it == active(player) || objects.getValue(it.item) in player.inv }.map { it.housePet() }

    public fun storedInMenagerie(player: Player): List<HousePet> {
        val saved = player.invMap[PetStorage.forms]
        return Species.entries.mapNotNull { species ->
            val forms = petForms.filter { it.species == species }
            val form = forms.firstOrNull { saved != null && objects.getValue(it.item) in saved }
            // Accounts imported with a native occupancy bit can withdraw the species' original form.
            (form ?: forms.first().takeIf { player.vars[bits.getValue(species.menagerie)] != 0 })?.housePet()
        }
    }

    public fun depositInMenagerie(player: Player, item: Int, capacity: Int): Boolean {
        val form = petForms.firstOrNull { it.item == item } ?: return false
        val stored = storedInMenagerie(player)
        if (!player.members || stored.size >= capacity || stored.any { storedPet -> petForms.first { it.item == storedPet.item }.species == form.species }) return false
        val storage = player.invMap.getOrPut(inventories[PetStorage.forms])
        val following = active(player) == form
        val held = player.inv.indexOfFirst { it?.id == item }
        if (!following && held == -1) return false
        val result = player.invTransaction(player.inv,storage) {
            if (!following) delete(select(player.inv),item,1,slot=held)
            add(select(storage),item,1)
        }
        if (!result.success) return false
        if (following) clear(player)
        save(player,form)
        set(player,bits.getValue(form.species.menagerie),1)
        return true
    }

    public fun withdrawFromMenagerie(player: Player, item: Int): Boolean {
        if (!player.members || storedInMenagerie(player).none { it.item == item }) return false
        val form = petForms.first { it.item == item }
        val storage = player.invMap.getOrPut(inventories[PetStorage.forms])
        val result = player.invTransaction(player.inv,storage) {
            if (objects.getValue(item) in storage) delete(select(storage),item,1)
            add(select(player.inv),item,1)
        }
        if (!result.success) return false
        set(player,bits.getValue(form.species.menagerie),0)
        save(player,form)
        return true
    }

    internal fun morph(player: Player, form: PetForm, slot: Int?) {
        if (slot == null) {
            if (active(player)?.species != form.species) return
            follow(player,form)
        } else {
            if (petForms.none { it.item == player.inv[slot]?.id && it.species == form.species }) return
            player.inv[slot] = InvObj(objects.getValue(form.item))
        }
        save(player,form)
    }
    internal fun changeGuardian(player: Player, form: PetForm) {
        if (active(player)?.species == Species.Guardian && active(player) != form) {
            save(player,form)
            follow(player,form)
        }
    }
    internal fun transformHeld(player: Player, slot: Int) {
        if (guardianForms.none { it.item == player.inv[slot]?.id }) return
        val form = guardianForms.last()
        player.inv[slot] = InvObj(objects.getValue(form.item))
        save(player,form)
    }
    public fun call(player: Player): Boolean {
        val form = active(player) ?: return false
        if (!player.members) return false
        val npc = followers[player]
        if (npc == null || !npc.isSlotAssigned) follow(player,form)
        else {
            npc.teleport(collision,player.coords)
            npc.playerFollow(player)
        }
        return true
    }
    internal fun login(player: Player) {
        val form = active(player) ?: return
        if (player.members) { follow(player,form); return }
        clear(player)
        if (player.invAdd(player.inv,form.item,1).success) {
            player.mes("Your pet is scared into your backpack.")
        } else {
            val bank = player.invMap.getOrPut(inventories[invs.bank])
            if (player.deliverToBank(bank,objects.getValue(form.item),1))
                player.mes("Your pet has been sent to your bank.")
            else player.mes("Probita is looking after your pet.")
        }
    }
    internal fun logout(player: Player) {
        if (active(player) != null) {
            removeNpc(player)
            set(player,PetVars.clientFollower,-1)
        }
    }
    private fun removeNpc(player: Player) {
        followers.remove(player)?.let { if (it.isSlotAssigned) repository.del(it,Int.MAX_VALUE) }
    }
    private fun clear(player: Player) {
        removeNpc(player)
        set(player,PetVars.active,0)
        set(player,PetVars.clientFollower,-1)
        set(player,PetBits.space,0)
    }
    internal fun death(player: Player) {
        val lost = mutableSetOf<PetForm>()
        active(player)?.let { lost += it; clear(player) }
        val keepCount = (if (player.skullIcon == null) 3 else 0) + player.vars[org.rsmod.api.config.refs.varbits.protect_item]
        val protectedItems = keeps.kept(player,keepCount).toMutableList()
        for (slot in player.inv.indices) {
            val form = petForms.firstOrNull { it.item == player.inv[slot]?.id } ?: continue
            if (protectedItems.remove(player.inv[slot])) continue
            lost += form
            player.inv[slot] = null
        }
        for (form in lost) {
            save(player,form)
            val npc = Npc(npcs.getValue(form.npc),player.coords)
            repository.add(npc,50)
            npc.mode = NpcMode.Wander
        }
    }
    internal fun reclaim(player: Player,species: Species) {
        if (!player.members || !insured(player,species) || location(player,species) != 0) return
        val item = if (species == Species.Guardian) player.vars[PetVars.guardian] else 0
        val form = petForms.firstOrNull { it.species == species && it.item == item } ?: petForms.first { it.species == species }
        if (!player.invAdd(player.inv,form.item,1).success) player.mes("Make room in your inventory for your pet.")
        else player.mes("Probita returns your pet.")
    }
}
