package com.paybuddy.payment.domain

import org.springframework.stereotype.Component

/** 동일 할인 합계 시 카드사 프로모션 개수 우선 (고객 체감 혜택 극대화) */
@Component
class KnapsackPromotionOptimizer : PromotionOptimizer {
    override fun optimize(
        candidates: List<PromotionOptimizer.Candidate>,
        maxDiscount: Long
    ): List<Int> {
        if (maxDiscount <= 0L) {
            return emptyList()
        }

        require(maxDiscount <= Int.MAX_VALUE.toLong()) {
            "최대 할인 가능 금액이 너무 큽니다: $maxDiscount"
        }
        val capacity = maxDiscount.toInt()

        if (candidates.isEmpty()) {
            return emptyList()
        }

        data class OptimalPromotionPath(
            val issuerPromotionCount: Int,
            val previousDiscountSum: Int?,
            val lastSelectedPromotionIndex: Int?
        )

        val optimalPaths = HashMap<Int, OptimalPromotionPath>(1024)
        optimalPaths[0] = OptimalPromotionPath(
            issuerPromotionCount = 0,
            previousDiscountSum = null,
            lastSelectedPromotionIndex = null
        )

        // 0-1 Knapsack DP
        for (candidateIndex in candidates.indices) {
            val candidate = candidates[candidateIndex]
            val discountAmount = candidate.discountAmount
            val issuerDelta = if (candidate.isIssuerPromotion) 1 else 0

            // snapshot을 통해 각 프로모션을 한 번만 사용하도록 보장
            val currentPaths = optimalPaths.entries.toList()

            for ((discountSum, path) in currentPaths) {
                val nextDiscountSum = discountSum + discountAmount
                if (nextDiscountSum > capacity) {
                    continue
                }

                val candidateIssuerCount = path.issuerPromotionCount + issuerDelta
                val candidatePath = OptimalPromotionPath(
                    issuerPromotionCount = candidateIssuerCount,
                    previousDiscountSum = discountSum,
                    lastSelectedPromotionIndex = candidateIndex
                )

                val existingPath = optimalPaths[nextDiscountSum]

                // 동일 할인 합계일 때 카드사 프로모션 개수가 더 많은 경로 선택
                if (existingPath == null || candidatePath.issuerPromotionCount > existingPath.issuerPromotionCount) {
                    optimalPaths[nextDiscountSum] = candidatePath
                }
            }
        }

        val maxDiscountSum = optimalPaths.keys.maxOrNull() ?: 0

        // 경로 역추적
        val selectedPromotions = mutableListOf<Int>()
        var currentSum = maxDiscountSum

        while (currentSum != 0) {
            val path = optimalPaths[currentSum] ?: break
            val promotionIndex = path.lastSelectedPromotionIndex ?: break

            selectedPromotions.add(promotionIndex)
            currentSum = path.previousDiscountSum ?: break
        }

        return selectedPromotions
    }
}
