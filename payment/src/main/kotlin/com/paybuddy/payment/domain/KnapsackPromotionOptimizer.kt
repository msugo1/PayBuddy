package com.paybuddy.payment.domain

/** 동일 할인 합계 시 카드사 프로모션 개수 우선 (고객 체감 혜택 극대화) */
object KnapsackPromotionOptimizer : PromotionOptimizer {
    override fun optimize(
        promotions: List<Promotion>,
        originalAmount: Long,
        capacity: Long
    ): List<Promotion> {
        if (capacity <= 0L || promotions.isEmpty()) {
            return emptyList()
        }

        val capacityInt = capacity.toInt()
        val discounts = promotions.map { it.calculateDiscount(originalAmount).toInt() }

        val optimalPaths = HashMap<Int, Path>(1024)
        optimalPaths[0] = Path(
            issuerPromotionCount = 0,
            prevDiscountSum = null,
            lastPromotionIndex = null
        )

        // 0-1 Knapsack DP
        for (index in promotions.indices) {
            val promotion = promotions[index]
            val discount = discounts[index]
            val issuerDelta = if (promotion.isIssuerDrivenPromotion()) 1 else 0

            // snapshot으로 각 프로모션 중복 사용 방지
            val snapshot = optimalPaths.entries.toList()

            for ((currentSum, path) in snapshot) {
                val nextSum = currentSum + discount
                if (nextSum > capacityInt) {
                    continue
                }

                val newPath = Path(
                    issuerPromotionCount = path.issuerPromotionCount + issuerDelta,
                    prevDiscountSum = currentSum,
                    lastPromotionIndex = index
                )

                val existing = optimalPaths[nextSum]

                // 동일 할인 시 카드사 프로모션 개수 우선
                if (existing == null || newPath.issuerPromotionCount > existing.issuerPromotionCount) {
                    optimalPaths[nextSum] = newPath
                }
            }
        }

        val maxSum = optimalPaths.keys.maxOrNull() ?: return emptyList()

        // 경로 역추적
        val result = mutableListOf<Promotion>()
        var sum = maxSum

        while (sum > 0) {
            val path = optimalPaths[sum] ?: break
            val index = path.lastPromotionIndex ?: break

            result.add(promotions[index])
            sum = path.prevDiscountSum ?: break
        }

        return result
    }

    private data class Path(
        val issuerPromotionCount: Int,
        val prevDiscountSum: Int?,
        val lastPromotionIndex: Int?
    )
}
