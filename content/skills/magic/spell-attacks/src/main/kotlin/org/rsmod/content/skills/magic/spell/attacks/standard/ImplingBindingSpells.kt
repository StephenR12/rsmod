package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.config.refs.objs
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.hunter.isImplingCreature
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onApNpcT
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.attack.attack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackRegistry
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object ImplingBindingNpcs:NpcReferences(){
    val types=(1..11).flatMap{listOf(find("ii_impling_type_$it"),find("ii_impling_type_${it}_maze"))}+listOf("johnny","junior","andy","joey","trouble","hingy","zolty","neil","yanny","matty","stace","ian","jamie","damo","xander","steveo","stewie").map{find("ii_impling_type_12_$it")}
}
/** Implings have no Attack operation; their native spell target uses a specific component AP. */
public class ImplingBindingSpells @Inject constructor(private val binding:BindingSpells,private val spells:MagicSpellRegistry,private val manager:SpellAttackManager,private val attacks:PlayerAttackManager){
    val definitions by lazy{listOf(objs.spell_bind,objs.spell_snare,objs.spell_entangle).map{checkNotNull(spells.getObjSpell(it))}}
    private val handlers by lazy{SpellAttackRegistry().also{val repo=SpellAttackRepository(it);with(binding){with(repo){register(manager)}}}}
    public suspend fun cast(access:ProtectedAccess,npc:Npc,spell:MagicSpell):Unit=with(access){
        if(!player.members||!npc.isImplingCreature()||!npc.isValidTarget()||!isWithinApRange(npc,10)||!lineOfSight(player.coords,npc.coords)||attacks.isAttackDelayed(player))return
        val handler=handlers[spell.obj]?:return
        attacks.setNextAttackDelay(player,5)
        handler.attack(this,npc,CombatAttack.Spell(player.righthand,spell,defensive=false))
    }
}
public class ImplingBindingScript @Inject constructor(private val spells:ImplingBindingSpells):PluginScript(){override fun ScriptContext.startup(){
    for(npc in ImplingBindingNpcs.types)for(spell in spells.definitions)onApNpcT(npc,spell.component){spells.cast(this,it.npc,spell)}
}}
