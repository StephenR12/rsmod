package org.rsmod.api.account.character.main

import com.fasterxml.jackson.databind.ObjectMapper
import java.sql.DriverManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.RealmConfig
import org.rsmod.api.realm.WorldFlag
import org.rsmod.game.type.mod.ModLevelTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

class CharacterAccountRepositoryTest {
    @Test
    fun `accounts auto-created on a members world receive membership`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { jdbc ->
            jdbc.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        login_username TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        members BOOLEAN NOT NULL DEFAULT 0
                    )
                    """
                        .trimIndent()
                )
            }

            val realm = Realm("test")
            realm.updateConfig(realmConfig(worldFlags = WorldFlag.Members.mask))
            val repository =
                CharacterAccountRepository(
                    ObjectMapper(),
                    realm,
                    CharacterAccountApplier(ModLevelTypeList(mutableMapOf())),
                    VarpTypeList(mutableMapOf()),
                )

            repository.insertOrSelectAccountId(DatabaseConnection(jdbc), "member", "hash")

            jdbc.createStatement().use { statement ->
                statement
                    .executeQuery("SELECT members FROM accounts WHERE login_username = 'member'")
                    .use { result ->
                        assertTrue(result.next())
                        assertTrue(result.getBoolean("members"))
                    }
            }
        }
    }

    private fun realmConfig(worldFlags: Int): RealmConfig =
        RealmConfig(
            id = 1,
            loginMessage = null,
            loginBroadcast = null,
            baseXpRate = 1.0,
            globalXpRate = 1.0,
            spawnCoord = CoordGrid(0, 0, 0),
            respawnCoord = CoordGrid(0, 0, 0),
            devMode = true,
            requireRegistration = false,
            ignorePasswords = true,
            autoAssignDisplayNames = true,
            worldFlags = worldFlags,
        )
}
