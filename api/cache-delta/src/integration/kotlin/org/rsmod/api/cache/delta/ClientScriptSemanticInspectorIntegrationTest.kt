package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClientScriptSemanticInspectorIntegrationTest {
    @Test
    fun `cache 233 quest routing resolves X Marks to cluequest varbit`() {
        val questVar =
            ClientScriptSemanticInspector.inspect(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                id = 4024,
            )
        assertEquals(1, questVar.intArgumentCount)
        val switchInstruction = questVar.instructions.single { it.opcode == SWITCH }
        val xMarksOffset = questVar.switches.single().getValue(X_MARKS_DB_ROW)
        val targetIndex = switchInstruction.index + xMarksOffset + 1
        assertEquals(
            ClientScriptSemanticInspector.Instruction(
                index = 495,
                opcode = PUSH_VARBIT,
                intOperand = CLUEQUEST_VARBIT,
                stringOperand = null,
            ),
            questVar.instructions[targetIndex],
        )

        val questStatus =
            ClientScriptSemanticInspector.inspect(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                id = 4029,
            )
        assertEquals(
            ClientScriptSemanticInspector.Instruction(27, INVOKE, 4024, null),
            questStatus.instructions[27],
        )
    }

    @Test
    fun `cache 233 quest routing resolves gated rune altar quest variables`() {
        val questVar =
            ClientScriptSemanticInspector.inspect(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                id = 4024,
            )
        val switchInstruction = questVar.instructions.single { it.opcode == SWITCH }
        val routes =
            listOf(32, 88, 100, 134).associateWith { row ->
                val target = switchInstruction.index + questVar.switches.single().getValue(row) + 1
                questVar.instructions[target]
            }
        assertEquals(
            mapOf(
                32 to ClientScriptSemanticInspector.Instruction(438, PUSH_VARBIT, 6104, null),
                88 to ClientScriptSemanticInspector.Instruction(219, PUSH_VARBIT, 2448, null),
                100 to ClientScriptSemanticInspector.Instruction(240, PUSH_VARBIT, 1103, null),
                134 to ClientScriptSemanticInspector.Instruction(504, PUSH_VARBIT, 7255, null),
            ),
            routes,
        )
    }

    private companion object {
        const val X_MARKS_DB_ROW = 162
        const val CLUEQUEST_VARBIT = 8063
        const val SWITCH = 60
        const val PUSH_VARBIT = 25
        const val INVOKE = 40
    }
}
