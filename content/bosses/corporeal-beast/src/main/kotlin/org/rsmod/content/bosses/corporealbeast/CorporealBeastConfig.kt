package org.rsmod.content.bosses.corporealbeast

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.map.CoordGrid

public object CorporealBeastNpcs : NpcReferences() {
    val boss = find("corp_beast")
    val darkCore = find("dark_core")
}

public object CorporealBeastLocs : LocReferences() {
    val wildernessEntrance = find("corp_beast_entrance")
    val lobbyEntrance = find("corp_cave_entrance")
    val lobbyExit = find("corp_cave_exit")
}

public object CorporealBeastObjs : ObjReferences() {
    val spiritShield = find("spirit_shield")
    val holyElixir = find("holy_elixir")
    val spectralSigil = find("spectral_sigil")
    val arcaneSigil = find("arcane_sigil")
    val elysianSigil = find("elysian_sigil")
    val jarOfSpirits = find("jar_of_spirits")
    val pet = find("corppet")
}

public object CorporealBeastMap {
    val bossRoom: CoordGrid = CoordGrid(2966, 4382, 2)
    val lobby: CoordGrid = CoordGrid(2967, 4387, 2)
    val wilderness: CoordGrid = CoordGrid(3204, 3681, 0)
}

public object CorporealBeastStats {
    public const val HITPOINTS: Int = 2_000
    public const val ATTACK: Int = 320
    public const val STRENGTH: Int = 320
    public const val DEFENCE: Int = 310
    public const val MAGIC: Int = 350
    public const val RANGED: Int = 150
    public const val ATTACK_RATE: Int = 4
    public const val MELEE_MAX_HIT: Int = 33
    public const val MAGIC_MAX_HIT: Int = 65
    public const val STOMP_MAX_HIT: Int = 51
}
