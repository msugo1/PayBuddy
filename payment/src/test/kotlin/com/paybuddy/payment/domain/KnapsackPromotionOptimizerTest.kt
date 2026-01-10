package com.paybuddy.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class KnapsackPromotionOptimizerTest {

    @Nested
    @DisplayName("경계 조건")
    inner class EdgeCaseTest {

        @Test
        fun `프로모션 목록이 비어있으면 빈 리스트를 반환한다`() {
            // Given
            val promotions = emptyList<Promotion>()

            // When
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                capacity = 5000
            )

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        fun `최대 할인 가능금액이 0이면 빈 리스트를 반환한다`() {
            // Given
            val promotions = listOf(createPromotion(discountValue = 1000))

            // When
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                capacity = 0
            )

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        fun `최대 할인 가능금액이 음수면 빈 리스트를 반환한다`() {
            // Given
            val promotions = listOf(createPromotion(discountValue = 1000))

            // When
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                capacity = -100
            )

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("단일 프로모션")
    inner class SinglePromotionTest {

        @Test
        fun `FIXED 타입이 최대 할인 가능금액 이내면 선택된다`() {
            // Given
            val promotion = createPromotion(
                id = "fixed_3000",
                discountType = DiscountType.FIXED,
                discountValue = 3000
            )

            // When (최대 할인 가능금액 = 5000)
            val result = KnapsackPromotionOptimizer.optimize(
                listOf(promotion),
                originalAmount = 10000,
                capacity = 5000
            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(promotion)
        }

        @Test
        fun `PERCENTAGE 타입이 최대 할인 가능금액 이내면 선택된다`() {
            // Given
            val promotion = createPromotion(
                id = "percent_10",
                discountType = DiscountType.PERCENTAGE,
                discountValue = 10,  // 10%
                maxDiscountAmount = 2000
            )

            // When (50000원의 10% = 5000원이지만 max = 2000원, 최대 할인 가능금액 = 3000)
            val result = KnapsackPromotionOptimizer.optimize(
                listOf(promotion),
                originalAmount = 50000,
                capacity = 3000
            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(promotion)
        }

        @Test
        fun `최대 할인 가능금액을 초과하면 선택되지 않는다`() {
            // Given
            val promotion = createPromotion(discountValue = 6000)

            // When (최대 할인 가능금액 = 5000)
            val result = KnapsackPromotionOptimizer.optimize(
                listOf(promotion),
                originalAmount = 10000,
                capacity = 5000
            )

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("여러 프로모션 모두 초과")
    inner class AllPromotionsExceedTest {

        @Test
        fun `모든 프로모션이 최대 할인 가능금액을 초과하면 빈 리스트를 반환한다`() {
            // Given
            val promotions = listOf(
                createPromotion(id = "p1", discountValue = 3000),
                createPromotion(id = "p2", discountValue = 4000),
                createPromotion(id = "p3", discountValue = 5000)
            )

            // When (최대 할인 가능금액 = 2000, 모든 프로모션이 초과)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                capacity = 2000
            )

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("카드사 프로모션 우선순위")
    inner class IssuerPriorityTest {

        @Test
        fun `동일 할인 금액이면 카드사 프로모션 개수가 많은 조합을 선택한다`() {
            // Given
            val promotions = listOf(
                // 조합1: promo1 + promo2 = 5000원 (카드사 0개)
                createPromotion(id = "promo1", discountValue = 2000, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "promo2", discountValue = 3000, provider = PromotionProvider.PLATFORM),
                // 조합2: promo3 + promo4 = 5000원 (카드사 2개)
                createPromotion(id = "promo3", discountValue = 2000, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "promo4", discountValue = 3000, provider = PromotionProvider.CARD_ISSUER)
            )

            // When (최대 할인 가능금액 = 5000)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                capacity = 5000
            )

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.id }).containsExactlyInAnyOrder("promo3", "promo4")
            assertThat(result.all { it.isIssuerDrivenPromotion() }).isTrue()
        }

        @Test
        fun `동일 할인에서 카드사 1개 vs 플랫폼 2개면 카드사를 선택한다`() {
            // Given
            val promotions = listOf(
                // 조합1: 카드사 1개 = 3000원
                createPromotion(id = "issuer1", discountValue = 3000, provider = PromotionProvider.CARD_ISSUER),
                // 조합2: 플랫폼 2개 = 3000원
                createPromotion(id = "platform1", discountValue = 1000, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "platform2", discountValue = 2000, provider = PromotionProvider.PLATFORM)
            )

            // When (최대 할인 가능금액 = 3000)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                capacity = 3000
            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo("issuer1")
        }

        @Test
        fun `할인 금액이 더 크면 카드사 우선순위를 무시한다`() {
            // Given
            val promotions = listOf(
                // 카드사 3000원
                createPromotion(id = "issuer", discountValue = 3000, provider = PromotionProvider.CARD_ISSUER),
                // 플랫폼 5000원
                createPromotion(id = "platform", discountValue = 5000, provider = PromotionProvider.PLATFORM)
            )

            // When (최대 할인 가능금액 = 5000)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                capacity = 5000
            )

            // Then (할인 금액이 더 큰 플랫폼 선택)
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo("platform")
            assertThat(result[0].calculateDiscount(10000)).isEqualTo(5000)
        }
    }

    @Nested
    @DisplayName("다수 프로모션 처리")
    inner class ManyPromotionsTest {

        @Test
        fun `FIXED 타입 프로모션 10개에서 최대 할인 가능금액에 가장 가까운 조합을 선택한다`() {
            // Given (홀수 금액으로 구성하여 12000원을 정확히 만들 수 없게 함)
            val promotions = listOf(
                createPromotion(id = "fixed1", discountValue = 501, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "fixed2", discountValue = 1003, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "fixed3", discountValue = 1507, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "fixed4", discountValue = 2011, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "fixed5", discountValue = 2503, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "fixed6", discountValue = 3001, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "fixed7", discountValue = 3509, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "fixed8", discountValue = 4007, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "fixed9", discountValue = 4501, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "fixed10", discountValue = 5003, provider = PromotionProvider.CARD_ISSUER)
            )

            // When (최대 할인 가능금액 = 12000, 정확히 12000을 만들 수 없음)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 50000,
                capacity = 12000
            )

            // Then (최적해: 11535원)
            assertThat(result).isNotEmpty()
            val totalDiscount = result.sumOf { it.calculateDiscount(50000) }
            assertThat(totalDiscount).isEqualTo(11535)
        }

        @Test
        fun `PERCENTAGE 타입 프로모션 10개에서 최대 할인 가능금액에 가장 가까운 조합을 선택한다`() {
            // Given (50000원 기준, 홀수 금액으로 계산되도록 maxDiscountAmount 설정)
            val promotions = listOf(
                createPromotion(id = "percent1", discountType = DiscountType.PERCENTAGE, discountValue = 5, maxDiscountAmount = 1001, provider = PromotionProvider.PLATFORM),    // 1001원
                createPromotion(id = "percent2", discountType = DiscountType.PERCENTAGE, discountValue = 10, maxDiscountAmount = 2003, provider = PromotionProvider.CARD_ISSUER),  // 2003원
                createPromotion(id = "percent3", discountType = DiscountType.PERCENTAGE, discountValue = 15, maxDiscountAmount = 3007, provider = PromotionProvider.PLATFORM),    // 3007원
                createPromotion(id = "percent4", discountType = DiscountType.PERCENTAGE, discountValue = 20, maxDiscountAmount = 4001, provider = PromotionProvider.CARD_ISSUER),  // 4001원
                createPromotion(id = "percent5", discountType = DiscountType.PERCENTAGE, discountValue = 25, maxDiscountAmount = 5003, provider = PromotionProvider.PLATFORM),    // 5003원
                createPromotion(id = "percent6", discountType = DiscountType.PERCENTAGE, discountValue = 30, maxDiscountAmount = 6007, provider = PromotionProvider.CARD_ISSUER),  // 6007원
                createPromotion(id = "percent7", discountType = DiscountType.PERCENTAGE, discountValue = 35, maxDiscountAmount = 7001, provider = PromotionProvider.PLATFORM),    // 7001원
                createPromotion(id = "percent8", discountType = DiscountType.PERCENTAGE, discountValue = 40, maxDiscountAmount = 8009, provider = PromotionProvider.CARD_ISSUER),  // 8009원
                createPromotion(id = "percent9", discountType = DiscountType.PERCENTAGE, discountValue = 45, maxDiscountAmount = 9003, provider = PromotionProvider.PLATFORM),    // 9003원
                createPromotion(id = "percent10", discountType = DiscountType.PERCENTAGE, discountValue = 50, maxDiscountAmount = 10007, provider = PromotionProvider.CARD_ISSUER) // 10007원
            )

            // When (최대 할인 가능금액 = 15000, 정확히 15000을 만들 수 없음)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 50000,
                capacity = 15000
            )

            // Then (최적해: 14020원)
            assertThat(result).isNotEmpty()
            val totalDiscount = result.sumOf { it.calculateDiscount(50000) }
            assertThat(totalDiscount).isEqualTo(14020)
        }

        @Test
        fun `FIXED와 PERCENTAGE 혼합 프로모션 10개에서 최대 할인 가능금액에 가장 가까운 조합을 선택한다`() {
            // Given (50000원 기준, 홀수 금액으로 구성)
            val promotions = listOf(
                createPromotion(id = "mixed1", discountType = DiscountType.FIXED, discountValue = 1001, provider = PromotionProvider.PLATFORM),                                      // 1001원
                createPromotion(id = "mixed2", discountType = DiscountType.PERCENTAGE, discountValue = 10, maxDiscountAmount = 2003, provider = PromotionProvider.CARD_ISSUER),    // 2003원
                createPromotion(id = "mixed3", discountType = DiscountType.FIXED, discountValue = 1507, provider = PromotionProvider.PLATFORM),                                      // 1507원
                createPromotion(id = "mixed4", discountType = DiscountType.PERCENTAGE, discountValue = 15, maxDiscountAmount = 3001, provider = PromotionProvider.CARD_ISSUER),    // 3001원
                createPromotion(id = "mixed5", discountType = DiscountType.FIXED, discountValue = 2009, provider = PromotionProvider.PLATFORM),                                      // 2009원
                createPromotion(id = "mixed6", discountType = DiscountType.PERCENTAGE, discountValue = 20, maxDiscountAmount = 4003, provider = PromotionProvider.CARD_ISSUER),    // 4003원
                createPromotion(id = "mixed7", discountType = DiscountType.FIXED, discountValue = 2501, provider = PromotionProvider.PLATFORM),                                      // 2501원
                createPromotion(id = "mixed8", discountType = DiscountType.PERCENTAGE, discountValue = 25, maxDiscountAmount = 5007, provider = PromotionProvider.CARD_ISSUER),    // 5007원
                createPromotion(id = "mixed9", discountType = DiscountType.FIXED, discountValue = 3003, provider = PromotionProvider.PLATFORM),                                      // 3003원
                createPromotion(id = "mixed10", discountType = DiscountType.PERCENTAGE, discountValue = 30, maxDiscountAmount = 6001, provider = PromotionProvider.CARD_ISSUER)    // 6001원
            )

            // When (최대 할인 가능금액 = 13000, 정확히 13000을 만들 수 없음)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 50000,
                capacity = 13000
            )

            // Then (최적해: 12527원)
            assertThat(result).isNotEmpty()
            val totalDiscount = result.sumOf { it.calculateDiscount(50000) }
            assertThat(totalDiscount).isEqualTo(12527)
        }
    }

    private fun createPromotion(
        id: String = "promo_test",
        name: String = "테스트 프로모션",
        provider: PromotionProvider = PromotionProvider.PLATFORM,
        discountType: DiscountType = DiscountType.FIXED,
        discountValue: Long = 1000,
        maxDiscountAmount: Long? = null,
        cardBrand: CardBrand? = CardBrand.VISA,
        validFrom: Instant = Instant.now(),
        validUntil: Instant = Instant.now().plusSeconds(86400)
    ): Promotion {
        return Promotion(
            id = id,
            name = name,
            provider = provider,
            discountType = discountType,
            discountValue = discountValue,
            maxDiscountAmount = maxDiscountAmount,
            cardBrand = cardBrand,
            cardType = null,
            issuerCode = null,
            minAmount = null,
            validFrom = validFrom,
            validUntil = validUntil
        )
    }
}
