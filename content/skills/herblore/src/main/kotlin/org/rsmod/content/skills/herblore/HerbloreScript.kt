package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.timers
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onPlayerSoftQueueWithArgs
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class HerbloreScript
@Inject
constructor(
    private val cleaning: HerbCleaning,
    private val ingredientCrushing: IngredientCrushing,
    private val crystalShardCrushing: CrystalShardCrushing,
    private val lavaScaleCrushing: LavaScaleCrushing,
    private val gorakClawCrushing: GorakClawCrushing,
    private val suqahToothCrushing: SuqahToothCrushing,
    private val oneShotIngredientCrushing: OneShotIngredientCrushing,
    private val karambwanjiPasteCrushing: KarambwanjiPasteCrushing,
    private val blackDyeMaking: BlackDyeMaking,
    private val blamishSnailSlimeMaking: BlamishSnailSlimeMaking,
    private val oilyFishingRodMaking: OilyFishingRodMaking,
    private val unfinishedPotions: UnfinishedPotionMixing,
    private val finishedPotions: FinishedPotionMixing,
    private val equipment: HerbloreEquipment,
    private val standardPotions: StandardPotionDrinking,
    private val goadingPotion: GoadingPotionDrinking,
    private val surgePotion: SurgePotionDrinking,
    private val surgeCooldown: SurgePotionCooldownRepository,
    private val forgottenBrewUpgrading: ForgottenBrewUpgrading,
    private val barbarianMixing: BarbarianMixing,
    private val barbarianMixDrinking: BarbarianMixDrinking,
    private val swampTarMixing: SwampTarMixing,
    private val coconutMilkMaking: CoconutMilkMaking,
    private val doseScaledPotionMixing: DoseScaledPotionMixing,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in HerbCleaningRecipes.all) {
            onOpHeld1(recipe.grimy) { cleaning.start(this, it.slot, recipe) }
        }
        for (recipe in IngredientCrushingRecipes.all) {
            onOpHeldU(objs.pestle_and_mortar, recipe.input) {
                ingredientCrushing.start(this, it.secondSlot, recipe)
            }
        }
        onOpHeldU(objs.knife, objs.chocolate_bar) {
            ingredientCrushing.start(
                this,
                it.secondSlot,
                IngredientCrushingRecipes.byInput.getValue(objs.chocolate_bar.id),
                objs.knife,
            )
        }
        onOpHeldU(objs.pestle_and_mortar, objs.crystal_shard) {
            crystalShardCrushing.start(this, it.secondSlot)
        }
        onOpHeldU(objs.pestle_and_mortar, objs.lava_scale) {
            lavaScaleCrushing.start(this, it.secondSlot)
        }
        onOpHeldU(objs.pestle_and_mortar, objs.gorak_claws) {
            gorakClawCrushing.start(this, it.secondSlot)
        }
        onOpHeldU(objs.pestle_and_mortar, objs.suqah_tooth) {
            suqahToothCrushing.start(this, it.secondSlot)
        }
        for (recipe in OneShotIngredientCrushingRecipes.all) {
            onOpHeldU(objs.pestle_and_mortar, recipe.input) {
                oneShotIngredientCrushing.start(this, it.secondSlot, recipe)
            }
        }
        onOpHeldU(objs.pestle_and_mortar, objs.raw_karambwanji) {
            karambwanjiPasteCrushing.start(this, it.secondSlot)
        }
        onOpHeldU(objs.pestle_and_mortar, objs.black_mushroom) {
            blackDyeMaking.start(this, it.secondSlot)
        }
        for (snail in BlamishSnailSlimeMaking.snails) {
            onOpHeldU(objs.pestle_and_mortar, snail) {
                blamishSnailSlimeMaking.start(this, it.secondSlot, snail)
            }
        }
        for (recipe in OilyFishingRodRecipes.all) {
            onOpHeldU(objs.blamish_oil, recipe.rod) {
                oilyFishingRodMaking.start(this, it.firstSlot, it.secondSlot, recipe)
            }
        }
        onOpHeldU(objs.hammer, objs.coconut) { coconutMilkMaking.crack(this, it.secondSlot) }
        onOpHeldU(objs.coconut_half, objs.empty_vial) {
            coconutMilkMaking.fill(this, it.firstSlot, it.secondSlot)
        }
        for (recipe in UnfinishedPotionRecipes.all) {
            onOpHeldU(recipe.base, recipe.herb) { unfinishedPotions.select(this, recipe) }
        }
        for (recipe in FinishedPotionRecipes.all) {
            onOpHeldU(recipe.unfinished, recipe.secondary) { finishedPotions.select(this, recipe) }
        }
        for (recipe in DoseScaledPotionRecipes.all) {
            onOpHeldU(recipe.potion, recipe.secondary) {
                doseScaledPotionMixing.select(this, recipe)
            }
        }
        for (recipe in ForgottenBrewUpgradeRecipes.all) {
            onOpHeldU(recipe.ancientBrew, objs.ancient_essence) {
                forgottenBrewUpgrading.start(this, it.firstSlot, it.secondSlot, recipe)
            }
        }
        for (recipe in BarbarianMixRecipes.all) {
            onOpHeldU(recipe.potion, objs.caviar) {
                barbarianMixing.select(this, recipe, objs.caviar)
            }
            if (recipe.acceptsRoe) {
                onOpHeldU(recipe.potion, objs.roe) {
                    barbarianMixing.select(this, recipe, objs.roe)
                }
            }
            onOpHeld1(recipe.output) { barbarianMixDrinking.drink(this, it.slot, recipe, 2) }
            onOpHeld1(recipe.oneDose) { barbarianMixDrinking.drink(this, it.slot, recipe, 1) }
        }
        for (recipe in SwampTarRecipes.all) {
            onOpHeldU(recipe.herb, objs.swamp_tar) { swampTarMixing.select(this, recipe) }
        }
        for (dose in GoadingPotionDoses.all) {
            onOpHeld1(dose.potion) { goadingPotion.drink(this, it.slot, dose) }
        }
        for (dose in SurgePotionDoses.all) {
            onOpHeld1(dose.potion) { surgePotion.drink(this, it.slot, dose) }
        }
        for (dose in
            AntipoisonPotionDoses.all +
                HigherPoisonRemedyPotionDoses.all +
                SanfewSerumPotionDoses.all +
                AntifirePotionDoses.all +
                StatRestorePotionDoses.all +
                PrayerRegenerationPotionDoses.all +
                MenaphiteRemedyPotionDoses.all +
                StatBoostPotionDoses.all +
                DirectOnlyStatBoostPotionDoses.all +
                DirectBrewPotionDoses.all) {
            onOpHeld1(dose.potion) { standardPotions.drink(this, it.slot, dose) }
        }
        onOpHeld4(objs.amulet_of_chemistry) { equipment.breakChemistryAmulet(this, it.slot) }
        onOpHeld4(objs.alchemists_amulet_uncharged) {
            equipment.chargeAlchemistsAmulet(this, it.slot)
        }
        onOpHeld3(objs.alchemists_amulet_charged) { equipment.checkAlchemistsAmulet(this) }
        onOpHeld4(objs.alchemists_amulet_charged) {
            equipment.chargeAlchemistsAmulet(this, it.slot)
        }
        onOpHeld5(objs.alchemists_amulet_charged) {
            equipment.unchargeAlchemistsAmulet(this, it.slot)
        }
        onOpHeld3(objs.prescription_goggles) { equipment.changePrescriptionGoggles(this, it.slot) }
        onOpHeld3(objs.prescription_goggles_focused) {
            equipment.changePrescriptionGoggles(this, it.slot)
        }
        onOpHeldU(objs.amulet_of_chemistry, objs.alchemists_amulet_uncharged) {
            equipment.chargeAlchemistsAmulet(this, it.secondSlot)
        }
        onOpHeldU(objs.amulet_of_chemistry, objs.alchemists_amulet_charged) {
            equipment.chargeAlchemistsAmulet(this, it.secondSlot)
        }
        onPlayerQueueWithArgs<HerbCleaningRequest>(herblore_queues.herbCleaning) {
            cleaning.process(this, it.args)
        }
        onPlayerQueueWithArgs<IngredientCrushingRequest>(herblore_queues.ingredientCrushing) {
            ingredientCrushing.process(this, it.args)
        }
        onPlayerQueue(herblore_queues.lavaScaleCrushing) { lavaScaleCrushing.process(this) }
        onPlayerQueue(herblore_queues.gorakClawCrushing) { gorakClawCrushing.process(this) }
        onPlayerQueueWithArgs<SuqahToothCrushingRequest>(herblore_queues.suqahToothCrushing) {
            suqahToothCrushing.process(this, it.args)
        }
        onPlayerQueue(herblore_queues.karambwanjiPasteCrushing) {
            karambwanjiPasteCrushing.process(this)
        }
        onPlayerQueueWithArgs<BlamishSnailSlimeRequest>(herblore_queues.blamishSnailSlime) {
            blamishSnailSlimeMaking.process(this, it.args)
        }
        onPlayerQueueWithArgs<OilyFishingRodRequest>(herblore_queues.oilyFishingRod) {
            oilyFishingRodMaking.process(this, it.args)
        }
        onPlayerQueueWithArgs<UnfinishedPotionRequest>(herblore_queues.unfinishedPotions) {
            unfinishedPotions.process(this, it.args)
        }
        onPlayerQueueWithArgs<FinishedPotionRequest>(herblore_queues.finishedPotions) {
            finishedPotions.process(this, it.args)
        }
        onPlayerQueueWithArgs<ForgottenBrewUpgradeRequest>(herblore_queues.forgottenBrewUpgrade) {
            forgottenBrewUpgrading.process(this, it.args)
        }
        onPlayerQueueWithArgs<BarbarianMixRequest>(herblore_queues.barbarianMix) {
            barbarianMixing.process(this, it.args)
        }
        onPlayerQueueWithArgs<SwampTarRequest>(herblore_queues.swampTar) {
            swampTarMixing.process(this, it.args)
        }
        onPlayerQueue(herblore_queues.coconutMilk) { coconutMilkMaking.process(this) }
        onPlayerQueueWithArgs<DoseScaledPotionRequest>(herblore_queues.doseScaledPotions) {
            doseScaledPotionMixing.process(this, it.args)
        }
        onPlayerSoftQueueWithArgs<SurgePotionRestore>(herblore_queues.surgePotionRestore) {
            surgePotion.restoreAfterPvpDelay(player, args)
        }
        onPlayerSoftTimer(timers.surge_potion_cooldown) { surgeCooldown.tick(player) }
        onEvent<PlayerDeathEvent.Completed> {
            surgePotion.cancelPendingRestore(player)
            surgeCooldown.clear(player)
        }
        onPlayerLogin { surgeCooldown.resume(player) }
        onPlayerLogout {
            surgePotion.cancelPendingRestore(player)
            surgeCooldown.pause(player)
        }
    }
}
