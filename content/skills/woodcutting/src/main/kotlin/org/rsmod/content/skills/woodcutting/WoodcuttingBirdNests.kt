package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingObjs
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public class WoodcuttingBirdNests
@Inject
constructor(private val random: GameRandom, private val objRepo: ObjRepository, private val gear: org.rsmod.content.skills.woodcutting.forestry.ForestryGear) {
    /**
     * Rolls the standard Woodcutting nest table after an otherwise-successful log roll.
     *
     * @return `true` when the log and its experience should be replaced by a bird nest.
     */
    public fun tryDrop(access: ProtectedAccess, product: ObjType): Boolean {
        with(access) {
            if (
                !player.members || product == objs.redwood_logs || product == objs.blisterwood_logs
            ) {
                return false
            }

            val denominator = chanceDenominator(player)
            if (random.of(denominator) != 0) {
                return false
            }

            val tableSize = tableSize(player)
            val preference=org.rsmod.content.skills.woodcutting.forestry.TwitchersGloves.preference(player)
            val nest=if(preference in 1..3 && random.of(5)==0)when(preference){
                1->WoodcuttingObjs.birdNestRing
                2->selectNest(random.of(3))
                else->WoodcuttingObjs.birdNestSeeds
            }else selectNest(random.of(tableSize))
            val dropCoords = adjacentDropCoords(player.coords, random.of(ADJACENT_OFFSETS.size))
            objRepo.add(
                type = nest,
                coords = dropCoords,
                duration = Constants.lootdrop_duration,
                receiver = player,
            )
            mes("<col=ff0000>A bird's nest falls out of the tree.</col>")
            return true
        }
    }

    public fun chanceDenominator(player: Player): Int =
        chanceDenominator(player.hasWoodcuttingCape())

    public fun tableSize(player: Player): Int =
        tableSize(player.worn.contains(WoodcuttingObjs.strungRabbitFoot))

    private fun Player.hasWoodcuttingCape(): Boolean =
        worn.contains(objs.woodcutting_skillcape) ||
            worn.contains(objs.woodcutting_skillcape_t) ||
            worn.contains(objs.max_cape_worn) || gear.capeEquipped(this)

    public companion object {
        public const val BASE_DENOMINATOR: Int = 256
        public const val CAPE_DENOMINATOR: Int = 230
        public const val ADJACENT_TILE_COUNT: Int = 9

        public fun chanceDenominator(hasWoodcuttingCape: Boolean): Int =
            if (hasWoodcuttingCape) CAPE_DENOMINATOR else BASE_DENOMINATOR

        public fun tableSize(hasStrungRabbitFoot: Boolean): Int =
            if (hasStrungRabbitFoot) 95 else 100

        /** The rabbit-foot table removes five seed slots without changing any other slot. */
        public fun selectNest(roll: Int): ObjType =
            when (roll) {
                0 -> WoodcuttingObjs.birdNestEggRed
                1 -> WoodcuttingObjs.birdNestEggBlue
                2 -> WoodcuttingObjs.birdNestEggGreen
                in 3..34 -> WoodcuttingObjs.birdNestRing
                in 35..99 -> WoodcuttingObjs.birdNestSeeds
                else ->
                    throw IllegalArgumentException("Bird-nest table roll must be in 0..99: $roll")
            }

        /**
         * The live game places a nest on a random adjacent tile with a slight westward bias. The
         * extra west entry preserves that documented bias while never selecting the player's tile.
         */
        public fun adjacentDropCoords(coords: CoordGrid, roll: Int): CoordGrid {
            require(roll in ADJACENT_OFFSETS.indices) { "Adjacent roll out of bounds: $roll" }
            val (x, z) = ADJACENT_OFFSETS[roll]
            return coords.translate(xOffset = x, zOffset = z)
        }

        private val ADJACENT_OFFSETS =
            listOf(-1 to -1, -1 to 0, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
    }
}
