package org.rsmod.game.type.util

/** A native ground-item, NPC, or scenery menu entry with a sub-option or a variable condition. */
public data class EntityOption(
    public val index: Int,
    public val text: String,
    public val subOption: Int?,
    public val condition: Condition?,
) {
    public data class Condition(
        public val varp: Int,
        public val varbit: Int,
        public val min: Int,
        public val max: Int,
    )
}
