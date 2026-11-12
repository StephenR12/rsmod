package org.rsmod.content.skills.runecraft

import org.rsmod.game.type.obj.ObjType

public data class EssencePouchDefinition(
    public val displayName: String,
    public val normal: ObjType,
    public val degraded: ObjType?,
    public val requiredLevel: Int,
    public val baseCapacity: Int,
    public val degradationStages: List<DegradationStage> = emptyList(),
    public val scalesWithLevel: Boolean = false,
    public val deterministicDegradation: Boolean = false,
) {
    public val variants: List<ObjType> = listOfNotNull(normal, degraded)

    public fun capacity(runecraftLevel: Int, degradation: Int): Int {
        val unscaled =
            degradationStages.firstOrNull { degradation >= it.threshold }?.capacity ?: baseCapacity
        if (!scalesWithLevel || unscaled == 0) {
            return unscaled
        }
        val levelCapacity =
            when {
                runecraftLevel >= 85 -> 40
                runecraftLevel >= 75 -> 27
                runecraftLevel >= 50 -> 16
                else -> 8
            }
        return ((unscaled * levelCapacity) / 40).coerceAtLeast(1)
    }

    public fun isDegraded(degradation: Int): Boolean =
        degradationStages.lastOrNull()?.let { degradation >= it.threshold } == true
}

public data class DegradationStage(public val threshold: Int, public val capacity: Int)

public object EssencePouchDefinitions {
    public val small =
        EssencePouchDefinition(
            displayName = "small pouch",
            normal = RunecraftObjs.smallPouch,
            degraded = null,
            requiredLevel = 1,
            baseCapacity = 3,
        )

    public val medium =
        EssencePouchDefinition(
            displayName = "medium pouch",
            normal = RunecraftObjs.mediumPouch,
            degraded = RunecraftObjs.mediumPouchDegraded,
            requiredLevel = 25,
            baseCapacity = 6,
            degradationStages = listOf(DegradationStage(800, 0), DegradationStage(400, 3)),
        )

    public val large =
        EssencePouchDefinition(
            displayName = "large pouch",
            normal = RunecraftObjs.largePouch,
            degraded = RunecraftObjs.largePouchDegraded,
            requiredLevel = 50,
            baseCapacity = 9,
            degradationStages =
                listOf(
                    DegradationStage(1000, 0),
                    DegradationStage(800, 3),
                    DegradationStage(600, 5),
                    DegradationStage(400, 7),
                ),
        )

    public val giant =
        EssencePouchDefinition(
            displayName = "giant pouch",
            normal = RunecraftObjs.giantPouch,
            degraded = RunecraftObjs.giantPouchDegraded,
            requiredLevel = 75,
            baseCapacity = 12,
            degradationStages =
                listOf(
                    DegradationStage(1200, 0),
                    DegradationStage(1000, 3),
                    DegradationStage(800, 5),
                    DegradationStage(600, 6),
                    DegradationStage(400, 7),
                    DegradationStage(300, 8),
                    DegradationStage(200, 9),
                ),
        )

    public val colossal =
        EssencePouchDefinition(
            displayName = "colossal pouch",
            normal = RunecraftObjs.colossalPouch,
            degraded = RunecraftObjs.colossalPouchDegraded,
            requiredLevel = 25,
            baseCapacity = 40,
            degradationStages =
                listOf(
                    DegradationStage(1020, 0),
                    DegradationStage(1015, 5),
                    DegradationStage(995, 10),
                    DegradationStage(950, 15),
                    DegradationStage(870, 20),
                    DegradationStage(745, 25),
                    DegradationStage(565, 30),
                    DegradationStage(320, 35),
                ),
            scalesWithLevel = true,
            deterministicDegradation = true,
        )

    public val all: List<EssencePouchDefinition> = listOf(small, medium, large, giant, colossal)

    private val byObjId = all.flatMap { pouch -> pouch.variants.map { it.id to pouch } }.toMap()

    public fun find(objId: Int): EssencePouchDefinition? = byObjId[objId]
}
