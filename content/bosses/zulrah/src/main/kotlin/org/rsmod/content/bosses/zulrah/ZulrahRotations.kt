package org.rsmod.content.bosses.zulrah

public data class ZulrahPhase(
    val form: ZulrahForm,
    val position: ZulrahPosition,
    val attacks: Int = 5,
    val jad: Boolean = false,
    val clouds: Boolean = false,
    val snakelings: Boolean = false,
)

/**
 * Four stable rotations. A session chooses one at entry and loops it until Zulrah dies. Keeping
 * phase data immutable makes transitions deterministic and easy to regression-test.
 */
public object ZulrahRotations {
    public val all: List<List<ZulrahPhase>> =
        listOf(
            listOf(
                phase(ZulrahForm.Ranged, ZulrahPosition.North, clouds = true),
                phase(ZulrahForm.Melee, ZulrahPosition.West),
                phase(ZulrahForm.Magic, ZulrahPosition.North, snakelings = true),
                phase(ZulrahForm.Ranged, ZulrahPosition.South, clouds = true),
                phase(ZulrahForm.Magic, ZulrahPosition.West, snakelings = true),
                jad(ZulrahPosition.North),
            ),
            listOf(
                phase(ZulrahForm.Ranged, ZulrahPosition.North, clouds = true),
                phase(ZulrahForm.Magic, ZulrahPosition.East, snakelings = true),
                phase(ZulrahForm.Melee, ZulrahPosition.South),
                phase(ZulrahForm.Ranged, ZulrahPosition.West, clouds = true),
                phase(ZulrahForm.Magic, ZulrahPosition.North, snakelings = true),
                jad(ZulrahPosition.East),
            ),
            listOf(
                phase(ZulrahForm.Ranged, ZulrahPosition.North, clouds = true),
                phase(ZulrahForm.Melee, ZulrahPosition.East),
                phase(ZulrahForm.Ranged, ZulrahPosition.South, snakelings = true),
                phase(ZulrahForm.Magic, ZulrahPosition.West, clouds = true),
                phase(ZulrahForm.Melee, ZulrahPosition.North),
                jad(ZulrahPosition.South),
            ),
            listOf(
                phase(ZulrahForm.Ranged, ZulrahPosition.North, clouds = true),
                phase(ZulrahForm.Magic, ZulrahPosition.West, snakelings = true),
                phase(ZulrahForm.Ranged, ZulrahPosition.East, clouds = true),
                phase(ZulrahForm.Melee, ZulrahPosition.South),
                phase(ZulrahForm.Magic, ZulrahPosition.North, snakelings = true),
                jad(ZulrahPosition.West),
            ),
        )

    private fun phase(
        form: ZulrahForm,
        position: ZulrahPosition,
        clouds: Boolean = false,
        snakelings: Boolean = false,
    ) = ZulrahPhase(form, position, clouds = clouds, snakelings = snakelings)

    private fun jad(position: ZulrahPosition) =
        ZulrahPhase(ZulrahForm.Ranged, position, attacks = 8, jad = true)
}
