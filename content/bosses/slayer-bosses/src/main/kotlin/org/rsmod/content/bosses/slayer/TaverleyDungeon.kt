package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
import org.rsmod.api.config.refs.*
import org.rsmod.api.death.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.map.obj.MapObjSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TaverleyDoors : LocReferences() {
    val cell = find("dungeonjail")
    val deep = find("deepdungeondoor")
}

internal object TaverleyKeys : ObjReferences() {
    val jail = find("jail_key")
    val dusty = find("dusty_key")
}

internal object TaverleyNpcs : NpcReferences() {
    val prisoner = find("velrak_the_explorer")
    val jailer = find("jailer")
}

internal object JailerStats : NpcEditor() {
    init {
        edit(TaverleyNpcs.jailer) {
            hitpoints = 47
            attack = 40
            strength = 40
            defence = 40
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
            param[params.defence_stab] = 79
            param[params.defence_slash] = 63
            param[params.defence_crush] = 47
        }
    }
}

internal object TaverleyPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<TaverleyDungeon>("taverley-npcs.toml")
    }
}

internal object TaverleyGround : MapObjSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<TaverleyDungeon>("taverley-objs.toml")
    }
}

public class TaverleyDungeon @Inject constructor(private val drops: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        drops.register(
            TaverleyNpcs.jailer,
            NpcDropTable(
                NpcDropRolls.sequence(
                    NpcDropRolls.item(objs.bones),
                    NpcDropRolls.item(TaverleyKeys.jail, duration = 100),
                )
            ),
        )
        onOpLoc1(TaverleyDoors.cell) { cell(it.loc.coords) }
        onOpLocU(TaverleyDoors.cell, TaverleyKeys.jail) { cell(it.loc.coords) }
        onOpNpc1(TaverleyNpcs.prisoner) {
            if (!player.members || coords.z !in 9683..9689) return@onOpNpc1
            if (
                choice2(
                    "Do you know anywhere good to explore?",
                    true,
                    "Can I have a reward?",
                    false,
                )
            ) {
                if (invAdd(inv, TaverleyKeys.dusty).success)
                    mes("Velrak gives you a dusty key to the deeper dungeon.")
                else mes("You need room in your inventory for the dusty key.")
            } else mes("Velrak has no reward to offer.")
        }
        onOpLoc1(TaverleyDoors.deep) { deepGate(it.loc.coords) }
        onOpLocU(TaverleyDoors.deep, TaverleyKeys.dusty) { deepGate(it.loc.coords) }
    }

    private fun ProtectedAccess.cell(door: CoordGrid) {
        if (!player.members) return
        val inside = if (door.z == 9690) coords.z < door.z else coords.z > door.z
        if (!inside && TaverleyKeys.jail !in inv) {
            mes("The cell is locked.")
            return
        }
        val outsideZ = if (door.z == 9690) 9690 else 9694
        val insideZ = if (door.z == 9690) 9689 else 9695
        telejump(CoordGrid(door.x, if (inside) outsideZ else insideZ))
    }

    private fun ProtectedAccess.deepGate(door: CoordGrid) {
        if (!player.members || TaverleyKeys.dusty !in inv) {
            mes("You need a dusty key to unlock the gate.")
            return
        }
        telejump(CoordGrid(if (coords.x >= door.x) door.x - 1 else door.x, door.z))
    }
}
