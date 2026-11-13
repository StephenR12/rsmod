package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.runes.combo.ComboRuneRepository
import org.rsmod.api.spells.runes.combo.ComboRuneScript
import org.rsmod.api.spells.runes.compact.CompactRuneRepository
import org.rsmod.api.spells.runes.compact.CompactRuneScript
import org.rsmod.api.spells.runes.fake.FakeRuneRepository
import org.rsmod.api.spells.runes.fake.FakeRuneScript
import org.rsmod.api.spells.runes.staves.StaffSubstituteRepository
import org.rsmod.api.spells.runes.staves.StaffSubstituteScript
import org.rsmod.api.spells.runes.subs.RuneSubstituteRepository
import org.rsmod.api.spells.runes.subs.RuneSubstituteScript
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneRepository
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneScript
import org.rsmod.api.spells.MagicSpellRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.magic.spell.utility.standard.HomeTeleportScript
import org.rsmod.content.skills.magic.spell.utility.standard.MagicTablets
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class TabletAndGroupingTest {
    @Test
    fun GameTestState.`the Arceuus lectern crafts an Ape Atoll tablet before Monkey Madness and using it requires the quest`() =
        runInjectedGameTest(MagicSpellRegistry::class,TestModule,ComboRuneScript::class,CompactRuneScript::class,
            FakeRuneScript::class,StaffSubstituteScript::class,RuneSubstituteScript::class,UnlimitedRuneScript::class,MagicTablets::class) {
            player.members = true
            player.teleport(CoordGrid(1680,3765))
            val lectern = placeMapLoc(CoordGrid(1679,3765),locTypes.getValue(28802))
            player.setCurrentLevel(stats.magic,90)
            player.setBaseLevel(stats.magic,90)
            val xp = player.statMap.getFineXP(stats.magic)
            val essence = objTypes.getValue(13446)
            val tablet = objTypes.getValue(19631)
            player.inv[0] = InvObj(essence)
            player.inv[1] = InvObj(objs.soul_rune,2)
            player.inv[2] = InvObj(objs.law_rune,2)
            player.inv[3] = InvObj(objs.blood_rune,2)
            player.opLoc1(lectern)
            advance(2)
            assertNull(player.ui.getModalOrNull(components.mainmodal))
            player.withProtectedAccess { vars[varbits.spellbook] = 3 }
            player.opLoc1(lectern)
            advance(2)
            assertNotNull(player.ui.getModalOrNull(components.mainmodal))
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,10))
            advance(4)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.MakeApeAtollTablet))
            assertEquals(xp+1000,player.statMap.getFineXP(stats.magic))
            assertEquals(1,player.count(tablet))
            assertEquals(0,player.count(essence))
            assertEquals(0,player.count(objs.soul_rune))
            val tabletSlot = player.inv.indices.single { player.inv[it]?.id == tablet.id }
            player.withProtectedAccess { opHeld1(tabletSlot) }
            advance(4)
            assertEquals(1,player.count(tablet))
            player.withProtectedAccess { vars[varps.monkey_madness] = 9 }
            player.setCurrentLevel(stats.magic,1)
            val arrive = CoordGrid(2771,9101)
            allocZoneCollision(arrive)
            player.withProtectedAccess { opHeld1(tabletSlot) }
            advance(6)
            assertEquals(arrive,player.coords)
            assertEquals(0,player.count(tablet))
            assertEquals(xp+1000,player.statMap.getFineXP(stats.magic))
        }

    @Test
    fun GameTestState.`a full inventory can convert bones with a tablet without unlocking the spell or the palace task`() =
        runGameTest(MagicTablets::class) {
            player.members = true
            player.teleport(CoordGrid(3293,3165))
            player.inv[0] = InvObj(objTypes.getValue(8015))
            player.inv[1] = InvObj(objs.animals_bones_4)
            for (slot in 2..27) player.inv[slot] = InvObj(objs.hammer)
            val xp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(2,player.count(objs.peach))
            assertEquals(xp,player.statMap.getFineXP(stats.magic))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.PeachesAlKharidPalace))
        }

    @Test
    fun GameTestState.`redirection requires the base Construction level and records the Pollnivneach arrival`() =
        runInjectedGameTest(HeldUInteractions::class,null,HouseTablets::class) { held ->
            player.members = true
            val scroll=objTypes.getValue(11740)
            val original=objTypes.getValue(8013)
            val redirected=objTypes.getValue(11743)
            player.inv[0] = InvObj(scroll)
            player.inv[1] = InvObj(original)
            fun choose() {
                player.withProtectedAccess { held.interact(this,inv,objTypes[scroll],0,objTypes[original],1) }
                advance()
                assertNotNull(player.ui.getModalOrNull(components.mainmodal))
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,2))
                advance()
            }
            player.setBaseLevel(stats.construction,19)
            player.setCurrentLevel(stats.construction,24)
            choose()
            assertEquals(1,player.count(scroll))
            player.setBaseLevel(stats.construction,20)
            choose()
            assertEquals(1,player.count(redirected))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.RedirectPollnivneachTablet))
            val destination=org.rsmod.content.skills.magic.spell.utility.standard.HouseLocation.Pollnivneach.outside
            allocZoneCollision(destination)
            val slot=player.inv.indices.single { player.inv[it]?.id == redirected.id }
            player.withProtectedAccess { opHeld1(slot) }
            advance(4)
            assertEquals(destination,player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.RedirectPollnivneachTablet))
        }

    @Test
    fun GameTestState.`the Grouping tab checks combat level and completes Pest Control only on arrival`() =
        runGameTest(GroupingTravel::class,HomeTeleportScript::class) {
            player.members = true
            player.ifOpenOverlay(interfaces.side_channels,components.toplevel_target_side7)
            player.ifButton(cacheTypes.components.getValue((707 shl 16) or 5))
            advance()
            assertTrue(cacheTypes.interfaces.getValue(76) in player.ui)
            player.appearance.combatLevel=39
            player.ifButton(cacheTypes.components.getValue((76 shl 16) or 32),14)
            advance(24)
            assertEquals(0,player.vars[cacheTypes.varps.getValue(65572)])
            player.appearance.combatLevel=40
            val destination=CoordGrid(2658,2663)
            allocZoneCollision(destination)
            player.ifButton(cacheTypes.components.getValue((76 shl 16) or 32),14)
            advance(6)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.GroupTeleportPestControl))
            advance(20)
            assertEquals(destination,player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.GroupTeleportPestControl))
            assertTrue(player.vars[cacheTypes.varps.getValue(65572)] > 0)
            assertEquals(0,player.vars[varps.aide_tele_timer])
            player.ifButton(cacheTypes.components.getValue((76 shl 16) or 32),4)
            advance(24)
            assertEquals(destination,player.coords)
        }
    private object TestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }

}
