package org.rsmod.content.bosses.modern

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.game.type.npc.NpcType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ModernBossDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            ModernBossNpcs.amoxliatl,
            NpcDropTable(
                sequence(
                    item(ModernBossObjs.frozenTear, 2..20),
                    item(objs.coins, 10_000..30_000),
                    independent(denominator = 100, roll = item(ModernBossObjs.glacialTemotli)),
                    independent(denominator = 25, roll = item(ModernBossObjs.pendantOfAtes)),
                    independent(denominator = 3_000, roll = item(ModernBossObjs.amoxliatlPet)),
                )
            ),
        )
        registerTitan(ModernBossNpcs.branda, ModernBossObjs.brandaPet)
        registerTitan(ModernBossNpcs.eldric, ModernBossObjs.eldricPet)
        registry.register(
            ModernBossNpcs.yama,
            NpcDropTable(
                sequence(
                    item(objs.coins, 100_000..300_000),
                    independent(denominator = 300, roll = item(ModernBossObjs.soulflameHorn)),
                    independent(denominator = 600, roll = item(ModernBossObjs.oathplateHelm)),
                    independent(denominator = 600, roll = item(ModernBossObjs.oathplateChest)),
                    independent(denominator = 600, roll = item(ModernBossObjs.oathplateLegs)),
                    independent(
                        denominator = 15,
                        roll = item(ModernBossObjs.oathplateShards, 1..5),
                    ),
                    independent(numerator = 700, denominator = 9511, roll = item(ModernBossObjs.diabolicWorms, 90..90)),
                    independent(denominator = 32, roll = item(ModernBossObjs.forgottenLockbox)),
                    independent(denominator = 12, roll = item(ModernBossObjs.yamaDossier)),
                    independent(denominator = 3_000, roll = item(ModernBossObjs.yamaPet)),
                )
            ),
        )
        registry.register(
            ModernBossNpcs.doom,
            NpcDropTable(
                sequence(
                    item(ModernBossObjs.demonTear, 50..300),
                    item(objs.coins, 100_000..300_000),
                    independent(denominator = 1_350, roll = item(ModernBossObjs.avernicTreads)),
                    independent(denominator = 2_000, roll = item(ModernBossObjs.eyeOfAyak)),
                    independent(denominator = 2_500, roll = item(ModernBossObjs.mokhaiotlCloth)),
                    independent(denominator = 1_000, roll = item(ModernBossObjs.doomPet)),
                )
            ),
        )
    }

    private fun registerTitan(npc: NpcType, pet: org.rsmod.game.type.obj.ObjType) {
        val ice = npc.id == ModernBossNpcs.eldric.id
        val scroll = if(ice)ModernBossObjs.deadeyeScroll else ModernBossObjs.mysticVigourScroll
        val unlock = if(ice)varbits.prayer_deadeye_unlocked else varbits.prayer_mystic_vigour_unlocked
        registry.register(
            npc,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 30_000..80_000),
                    conditional({ it.hero.vars[unlock]==0 },independent(denominator = 75, roll = item(scroll))),
                    independent(denominator = 300, roll = item(ModernBossObjs.twinflamePieceOne)),
                    independent(denominator = 300, roll = item(ModernBossObjs.twinflamePieceTwo)),
                    independent(denominator = 500, roll = item(ModernBossObjs.giantsoulAmulet)),
                    independent(denominator = 5_000, roll = item(pet)),
                )
            ),
        )
    }
}
