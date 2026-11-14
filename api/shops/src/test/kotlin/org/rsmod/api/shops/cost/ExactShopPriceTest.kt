package org.rsmod.api.shops.cost

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExactShopPriceTest {
    @Test
    fun `exact default prices apply to every item in a bulk purchase`() {
        val result = StandardGpCostCalculations.calculateShopSellBulkParameters(
            initialStock = 10, currentStock = 10, baseCost = 600,
            requestedCount = 3, availableCurrency = 63_000,
            sellPercentage = 30.0, changePercentage = 0.0, basePriceOverride = 21_000,
        )
        assertEquals(3, result.count)
        assertEquals(63_000, result.totalValue)
    }

    @Test
    fun `exact prices retain the configured stock adjustment`() {
        val result = StandardGpCostCalculations.calculateShopBuyBulkParameters(
            initialStock = 10, currentStock = 10, baseCost = 100,
            requestedCount = 3, currencyCap = 1_000,
            buyPercentage = 40.0, changePercentage = 3.0, basePriceOverride = 50,
        )
        assertEquals(3, result.count)
        assertEquals(50 + 47 + 44, result.totalValue)
    }
}
