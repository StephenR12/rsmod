package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public enum class PrayerRemainsKind {
    Bone,
    DemonicAsh,
}

public data class PrayerRemainsRecipe(
    val obj: ObjType,
    val xp: Double,
    val kind: PrayerRemainsKind,
    val members: Boolean = true,
    val prayerLevel: Int = 1,
    val confirmBurial: Boolean = false,
)

public object PrayerRemainsRecipes {
    public val all: List<PrayerRemainsRecipe> by lazy {
        listOf(
            bone(objs.bones, 4.5, members = false),
            bone(objs.burnt_bones, 4.5, members = false),
            bone(objs.bat_bones, 5.3),
            bone(objs.big_bones, 15.0, members = false),
            bone(objs.babydragon_bones, 30.0),
            bone(objs.dragon_bones, 72.0),
            bone(objs.strykewyrm_bones, 60.0),
            bone(objs.frost_dragon_bones, 100.0),
            bone(objs.newbie_bones, 4.5, members = false),
            bone(objs.wolf_bones, 4.5),
            bone(objs.shaikahan_bones, 25.0),
            bone(objs.jogre_bones, 15.0),
            bone(objs.small_ninja_monkey_bones, 16.0),
            bone(objs.medium_ninja_monkey_bones, 18.0),
            bone(objs.gorilla_bones, 18.0),
            bone(objs.bearded_gorilla_bones, 18.0),
            bone(objs.monkey_bones, 5.0, members = false),
            bone(objs.small_zombie_monkey_bones, 5.0),
            bone(objs.large_zombie_monkey_bones, 5.0),
            bone(objs.monkey_skeleton_bones, 3.0),
            bone(objs.zogre_bones, 22.5),
            bone(objs.fayrg_bones, 84.0),
            bone(objs.raurg_bones, 96.0),
            bone(objs.ourg_bones, 140.0),
            bone(objs.dagannoth_bones, 125.0),
            bone(objs.wyvern_bones, 72.0),
            bone(objs.long_bone, 15.1, confirmBurial = true),
            bone(objs.curved_bone, 15.1, confirmBurial = true),
            bone(objs.lava_dragon_bones, 85.0),
            bone(objs.superior_dragon_bones, 150.0, prayerLevel = 70),
            bone(objs.wyrm_bones, 50.0),
            bone(objs.drake_bones, 80.0),
            bone(objs.hydra_bones, 110.0),
            bone(objs.tutorial_bones, 4.5, members = false),
            bone(objs.bleached_bones, 4.5),
            ash(objs.fiendish_ashes, 10.0, members = false),
            ash(objs.vile_ashes, 25.0, members = false),
            ash(objs.malicious_ashes, 65.0),
            ash(objs.abyssal_ashes, 85.0),
            ash(objs.infernal_ashes, 110.0),
            bone(objs.wyrmling_bones, 21.0),
            bone(objs.alans_bones, 3.0),
        )
    }

    public val byObj: Map<Int, PrayerRemainsRecipe> by lazy { all.associateBy { it.obj.id } }

    private fun bone(
        obj: ObjType,
        xp: Double,
        members: Boolean = true,
        prayerLevel: Int = 1,
        confirmBurial: Boolean = false,
    ): PrayerRemainsRecipe =
        PrayerRemainsRecipe(obj, xp, PrayerRemainsKind.Bone, members, prayerLevel, confirmBurial)

    private fun ash(obj: ObjType, xp: Double, members: Boolean = true): PrayerRemainsRecipe =
        PrayerRemainsRecipe(obj, xp, PrayerRemainsKind.DemonicAsh, members)
}
