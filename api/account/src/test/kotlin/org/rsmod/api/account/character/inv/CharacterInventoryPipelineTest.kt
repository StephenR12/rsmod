package org.rsmod.api.account.character.inv

import java.sql.DriverManager
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.account.character.main.CharacterAccountApplier
import org.rsmod.api.account.character.main.CharacterAccountData
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.inv.UnpackedInvType
import org.rsmod.game.type.mod.ModLevelTypeList
import org.rsmod.game.type.mod.UnpackedModLevelType
import org.rsmod.game.type.util.UncheckedType

@OptIn(UncheckedType::class)
class CharacterInventoryPipelineTest {
    @Test
    fun `account metadata applies the persisted character creation time`() {
        val createdAt = LocalDateTime.of(2024, 8, 20, 11, 12, 13)
        val player = Player()
        val playerLevel =
            UnpackedModLevelType(
                clientCode = 0,
                accessFlags = 0,
                internalId = 0,
                internalName = "player",
            )

        CharacterAccountApplier(ModLevelTypeList(mutableMapOf(0 to playerLevel)))
            .apply(player, accountData().copy(createdAt = createdAt))

        assertEquals(createdAt, player.createdAt)
    }

    @Test
    fun `save and load preserve independent exact item vars`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { jdbc ->
            jdbc.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE inventories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        character_id INTEGER NOT NULL,
                        inv_type INTEGER NOT NULL,
                        UNIQUE (character_id, inv_type)
                    )
                    """
                        .trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE inventory_objs (
                        inventories_id INTEGER NOT NULL,
                        slot INTEGER NOT NULL,
                        obj INTEGER NOT NULL,
                        count INTEGER NOT NULL,
                        vars INTEGER NOT NULL,
                        PRIMARY KEY (inventories_id, slot)
                    )
                    """
                        .trimIndent()
                )
            }

            val invType =
                UnpackedInvType(
                    scope = InvScope.Perm,
                    stack = InvStackType.Never,
                    size = 4,
                    flags = 0,
                    stock = null,
                    internalId = 42,
                    internalName = "exact_state_test",
                )
            val typeList = InvTypeList(mutableMapOf(invType.id to invType))
            val applier = CharacterInventoryApplier(typeList)
            val pipeline = CharacterInventoryPipeline(applier)
            val database = DatabaseConnection(jdbc)
            val first = InvObj(id = 4_880, count = 1, vars = 733)
            val second = InvObj(id = 4_880, count = 1, vars = 612)
            val source = Player()
            source.invMap.getOrPut(invType).apply {
                this[0] = first
                this[3] = second
            }

            pipeline.save(database, source, CHARACTER_ID)

            val metadata = CharacterMetadataList(accountData(), mutableListOf())
            pipeline.append(database, metadata)
            val loaded = Player()
            metadata.single().apply(loaded)

            assertEquals(first, loaded.invMap.getValue(invType)[0])
            assertEquals(second, loaded.invMap.getValue(invType)[3])
        }
    }

    private fun accountData(): CharacterAccountData =
        CharacterAccountData(
            realm = 1,
            accountId = 1,
            characterId = CHARACTER_ID,
            loginName = "exact-state-test",
            displayName = null,
            hashedPassword = "",
            email = null,
            members = true,
            modLevel = null,
            twofaEnabled = false,
            twofaSecret = null,
            twofaLastVerified = null,
            knownDevice = null,
            worldId = 1,
            coordX = 0,
            coordZ = 0,
            coordLevel = 0,
            varps = emptyMap(),
            createdAt = null,
            lastLogin = null,
            lastLogout = null,
            mutedUntil = null,
            bannedUntil = null,
            runEnergy = 10_000,
            xpRate = 1.0,
        )

    private companion object {
        const val CHARACTER_ID: Int = 7
    }
}
