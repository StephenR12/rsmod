package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDropRolls
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class PrayerDevicesTest {
    class Drops @Inject constructor(val registry:NpcDropTableRegistry, val death:NpcDeath,val ground:ObjRepository)

    @Test
    fun GameTestState.`crusher applies remaining ancestral charges once at delivery and skips big bones`() =
        runGameTest(PrayerDevices::class) {
            player.members = true
            player.inv[0] = InvObj(objTypes.getValue(13116),vars=5)
            val start = player.statMap.getFineXP(stats.prayer)
            player.withProtectedAccess {
                vars[varbits.brut_prayer_total] = 2
                val skipped = RemainsDrop(player,InvObj(objs.big_bones))
                publish(skipped)
                skipped.commit()
                assertEquals(2,vars[varbits.brut_prayer_total])
                val dragons = RemainsDrop(player,InvObj(objs.dragon_bones,3))
                publish(dragons)
                assertEquals(2,vars[varbits.brut_prayer_total])
                dragons.commit()
                dragons.commit()
                assertEquals(0,vars[varbits.brut_prayer_total])
            }
            assertEquals(start + 75 + 7*360,player.statMap.getFineXP(stats.prayer))
            assertEquals(1,player.inv[0]?.vars)
        }

    @Test
    fun GameTestState.`one equipped charge crushes one of two dragon bones and preserves the secondary drop`() =
        runInjectedGameTest(Drops::class,null,PrayerDevices::class) { drops ->
            player.members = true
            player.setBaseLevel(stats.prayer,70)
            player.setCurrentLevel(stats.prayer,10)
            val npc = spawnNpc(player.coords,npcTypes.getValue(1838))
            player.observerUUID = player.uuid
            npc.heroPoints(player,1)
            drops.registry.register(npc.type,NpcDropTable(NpcDropRolls.sequence(NpcDropRolls.item(objs.dragon_bones,2..2),NpcDropRolls.item(objs.long_bone))))
            player.worn[2] = InvObj(objTypes.getValue(22986),vars=1)
            val xp = player.statMap.getFineXP(stats.prayer)
            drops.death.spawnDrops(npc)
            assertEquals(xp+360,player.statMap.getFineXP(stats.prayer))
            assertEquals(14,player.stat(stats.prayer))
            assertEquals(0,player.worn[2]?.vars)
            assertEquals(listOf(objs.dragon_bones.id,objs.long_bone.id),drops.ground.findAll(player.coords).map{it.type}.sorted().toList())
        }

    @Test
    fun GameTestState.`charge toggle and uncharge preserve leftover single charges and discarded reservations`() =
        runInjectedGameTest(HeldUInteractions::class,null,PrayerDevices::class) { held ->
            player.members = true
            player.inv[0] = InvObj(objTypes.getValue(13116))
            player.inv[1] = InvObj(objTypes.getValue(4278),2)
            player.withProtectedAccess {
                held.interact(this,inv,objTypes[objTypes.getValue(4278)],1,objTypes[objTypes.getValue(13116)],0)
            }
            assertEquals(50,player.inv[0]?.vars)
            assertEquals(0,player.count(objTypes.getValue(4278)))
            val xp = player.statMap.getFineXP(stats.prayer)
            player.withProtectedAccess {
                val abandoned = RemainsDrop(player,InvObj(objs.dragon_bones))
                publish(abandoned)
                assertEquals(0,abandoned.remaining)
            }
            assertEquals(50,player.inv[0]?.vars)
            assertEquals(xp,player.statMap.getFineXP(stats.prayer))
            player.withProtectedAccess {
                opHeld3(0)
                val disabled = RemainsDrop(player,InvObj(objs.dragon_bones))
                publish(disabled)
                assertEquals(1,disabled.remaining)
                opHeld3(0)
                vars[DiaryRegion.Morytania.tier(DiaryDifficulty.Elite).reward] = 1
                val acquired = RemainsDrop(player,InvObj(objs.dragon_bones))
                publish(acquired)
                acquired.commit()
                acquired.commit()
                opHeld4(0)
            }
            assertEquals(24,player.inv[0]?.vars)
            assertEquals(1,player.count(objTypes.getValue(4278)))
            assertEquals(xp+720,player.statMap.getFineXP(stats.prayer))
        }

    @Test
    fun GameTestState.`sanctifier scatters demonic ashes only and elite grants full experience`() =
        runGameTest(PrayerDevices::class) {
            player.members = true
            player.inv[0] = InvObj(objTypes.getValue(25781),vars=2)
            val xp = player.statMap.getFineXP(stats.prayer)
            player.withProtectedAccess {
                val ordinary = RemainsDrop(player,InvObj(objs.ashes))
                publish(ordinary)
                assertEquals(1,ordinary.remaining)
                val first = RemainsDrop(player,InvObj(objs.vile_ashes))
                publish(first)
                first.commit()
                vars[DiaryRegion.Kourend.tier(DiaryDifficulty.Elite).reward] = 1
                val last = RemainsDrop(player,InvObj(objs.abyssal_ashes))
                publish(last)
                last.commit()
            }
            assertEquals(xp+975,player.statMap.getFineXP(stats.prayer))
            assertEquals(0,player.inv[0]?.vars)
        }

    @Test
    fun GameTestState.`the ghost disciple issues only one bonecrusher across the inventory and bank`() =
        runGameTest(Ectofuntus::class) {
            player.members = true
            player.worn[2] = InvObj(objTypes.getValue(552))
            val npc = spawnNpc(player.coords.translate(1,0),npcTypes.getValue(2988))
            player.opNpc1(npc)
            advance()
            assertEquals(0,player.count(objTypes.getValue(13116)))
            player.withProtectedAccess { vars[DiaryRegion.Morytania.tier(DiaryDifficulty.Hard).reward] = 1 }
            player.opNpc1(npc)
            advance()
            assertEquals(1,player.count(objTypes.getValue(13116)))
            player.inv[0] = null
            player.withProtectedAccess { bank[0] = InvObj(objTypes.getValue(22986),vars=45) }
            player.opNpc1(npc)
            advance()
            assertEquals(0,player.count(objTypes.getValue(13116)))
        }
}
