package org.rsmod.content.skills.slayer

/** Normal monsters and their corresponding combat forms in cache 233.4. */
internal val superiorMonsters = buildMap {
    fun creatures(superior: Int, vararg standard: Int) {
        standard.forEach { put(it, superior) }
    }
    creatures(7388, 448, 449, 450, 451, 452, 453, 454, 455, 456, 457) // Crushing hand
    creatures(7389, 406, 407, 408, 409) // Chasm Crawler
    creatures(14031, 13800, 13801) // Chasm Crawler
    creatures(7390, 414) // Screaming banshee
    creatures(7391, 7272) // Screaming twisted banshee
    creatures(7392, 421, 422, 14423) // Giant rockslug
    creatures(7393, 419, 420) // Cockathrice
    creatures(7393, 13030) // Cockathrice
    creatures(7394, 433, 434, 435, 436, 3139) // Flaming pyrelord
    creatures(9465, 6762, 6795) // Infernal pyrelord
    creatures(7395, 417, 418) // Monstrous basilisk
    creatures(7396, 443, 444, 445, 446, 447) // Malevolent Mage
    creatures(7397, 484, 485, 486, 487, 3138) // Insatiable Bloodveld
    creatures(7398, 7276) // Insatiable mutated Bloodveld
    creatures(
        7399,
        437,
        438,
        439,
        440,
        441,
        442,
        11241,
        11242,
        11243,
        11244,
        11245,
    ) // Vitreous Jelly
    creatures(7400, 7277) // Vitreous warped Jelly
    creatures(7400, 13799) // Vitreous warped Jelly
    creatures(10397, 427, 428, 429, 430) // Spiked Turoth
    creatures(12464, 12491, 12492, 12493, 12494, 12495, 12496, 12497, 12498) // Mutated Terrorbird
    creatures(12465, 12490) // Mutated Tortoise
    creatures(7401, 1047, 1048, 1049, 1050, 1051) // Cave abomination
    creatures(7402, 2, 3, 4, 5, 6, 7) // Abhorrent spectre
    creatures(7403, 7279) // Repugnant spectre
    creatures(9258, 9293) // Basilisk Sentinel
    creatures(10398, 8610) // Shadow Wyrm
    creatures(10399, 8611) // Shadow Wyrm
    creatures(7404, 423, 7249, 11238) // Choke devil
    creatures(7405, 410, 411) // King kurask
    creatures(7407, 412, 1543) // Marble gargoyle
    creatures(14520, 14704) // Ancient Custodian
    creatures(7411, 8, 11) // Nechryarch
    creatures(7411, 7278, 11240) // Nechryarch
    creatures(10400, 8612) // Guardian Drake
    creatures(7410, 415, 416, 7241, 11239) // Greater abyssal demon
    creatures(7409, 4005, 7250) // Night beast
    creatures(13680, 11175, 11176, 13666, 13667) // Dreadborn Araxyte
    creatures(7406, 498) // Nuclear smoke devil
    creatures(10402, 8609) // Colossal Hydra
}
