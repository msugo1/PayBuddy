package com.paybuddy.payment.domain

/** 동일 할인 합계 시 카드사 프로모션 개수 우선 (고객 체감 혜택 극대화) */
object KnapsackPromotionOptimizer : PromotionOptimizer {
    override fun optimize(
        promotions: List<Promotion>,
        originalAmount: Long,
        capacity: Long  // 최대할인가능금액
    ): List<Promotion> {
        if (capacity <= 0L || promotions.isEmpty()) {
            return emptyList()
        }

        val discounts = promotions.map {
            it.calculateDiscount(originalAmount)
        }

        val optimalPromotionPaths = HashMap<Long, Path>()
        optimalPromotionPaths[0] = Path(
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
            val snapshot = optimalPromotionPaths.entries.toList()

            for ((currentSum, path) in snapshot) {
                val nextSum = currentSum + discount
                if (nextSum > capacity) {
                    continue
                }

                val newPath = Path(
                    issuerPromotionCount = path.issuerPromotionCount + issuerDelta,
                    prevDiscountSum = currentSum,
                    lastPromotionIndex = index
                )

                val existing = optimalPromotionPaths[nextSum]

                // 동일 할인 시 카드사 프로모션 개수 우선
                if (existing == null || newPath.issuerPromotionCount > existing.issuerPromotionCount) {
                    optimalPromotionPaths[nextSum] = newPath
                }
            }
        }

        val maxSum = optimalPromotionPaths.keys.maxOrNull() ?: return emptyList()

        // 경로 역추적
        val result = mutableListOf<Promotion>()
        var sum = maxSum

        while (sum > 0) {
            val path = optimalPromotionPaths[sum] ?: break
            val index = path.lastPromotionIndex ?: break

            result.add(promotions[index])
            sum = path.prevDiscountSum ?: break
        }

        return result
    }

    private data class Path(
        val issuerPromotionCount: Int,
        val prevDiscountSum: Long?,
        val lastPromotionIndex: Int?
    )
}
