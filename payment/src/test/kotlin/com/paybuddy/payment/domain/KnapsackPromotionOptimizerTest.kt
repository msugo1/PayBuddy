package com.paybuddy.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
                maxDiscountLimit = 5000
            )

            // Then
            assertThat(result).isEmpty()
        }

        @ParameterizedTest
        @ValueSource(longs = [0, -100, -1000])
        fun `최대 할인 가능금액이 0 이하면 빈 리스트를 반환한다`(maxDiscountLimit: Long) {
            // Given
            val promotions = listOf(createPromotion(discountValue = 1000))

            // When
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                maxDiscountLimit = maxDiscountLimit
            )

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("모든 프로모션 초과")
    inner class AllPromotionsExceedTest {

        @Test
        fun `단일 프로모션이 최대 할인 가능금액을 초과하면 빈 리스트를 반환한다`() {
            // Given
            val promotion = createPromotion(discountValue = 6000)

            // When (최대 할인 가능금액 = 5000)
            val result = KnapsackPromotionOptimizer.optimize(
                listOf(promotion),
                originalAmount = 10000,
                maxDiscountLimit = 5000
            )

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        fun `여러 프로모션 모두 최대 할인 가능금액을 초과하면 빈 리스트를 반환한다`() {
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
                maxDiscountLimit = 2000
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
                // 조합1: promo1 + promo2 = 5000원 (카드사 1개)
                createPromotion(id = "promo1", discountValue = 2000, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "promo2", discountValue = 3000, provider = PromotionProvider.CARD_ISSUER),
                // 조합2: promo3 + promo4 = 5000원 (카드사 2개)
                createPromotion(id = "promo3", discountValue = 2000, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "promo4", discountValue = 3000, provider = PromotionProvider.CARD_ISSUER)
            )

            // When (최대 할인 가능금액 = 5000)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                maxDiscountLimit = 5000
            )

            // Then
            val totalDiscount = result.sumOf { it.calculateDiscount(10000) }
            assertThat(totalDiscount).isEqualTo(5000)
            assertThat(result.count { it.isIssuerDrivenPromotion() }).isEqualTo(2)
        }

        @Test
        fun `동일 할인에서 카드사 프로모션 1개 vs 플랫폼 프로모션 2개면 카드사 프로모션이 속한 조합을 선택한다`() {
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
                maxDiscountLimit = 3000
            )

            // Then
            val totalDiscount = result.sumOf { it.calculateDiscount(10000) }
            assertThat(totalDiscount).isEqualTo(3000)
            assertThat(result.count { it.isIssuerDrivenPromotion() }).isEqualTo(1)
        }

        @Test
        fun `할인 금액이 더 크면 카드사 우선순위를 무시한다`() {
            // Given
            val promotions = listOf(
                createPromotion(id = "issuer", discountValue = 3000, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "platform", discountValue = 3001, provider = PromotionProvider.PLATFORM)
            )

            // When (최대 할인 가능금액 = 3001)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 10000,
                maxDiscountLimit = 3001
            )

            // Then (할인 금액이 1원이라도 더 큰 플랫폼 선택)
            val totalDiscount = result.sumOf { it.calculateDiscount(10000) }
            assertThat(totalDiscount).isEqualTo(3001)
            assertThat(result.count { it.isIssuerDrivenPromotion() }).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("다수 프로모션 처리")
    inner class ManyPromotionsTest {

        @Test
        fun `혼합 프로모션 5개에서 최대 할인 가능금액에 가장 가까운 조합을 선택한다`() {
            // Given
            val promotions = listOf(
                createPromotion(id = "mixed1", discountType = DiscountType.FIXED, discountValue = 1001, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "mixed2", discountType = DiscountType.PERCENTAGE, discountValue = 10, maxDiscountAmount = 2003, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "mixed3", discountType = DiscountType.FIXED, discountValue = 1507, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "mixed4", discountType = DiscountType.PERCENTAGE, discountValue = 15, maxDiscountAmount = 3001, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "mixed5", discountType = DiscountType.FIXED, discountValue = 2009, provider = PromotionProvider.PLATFORM)
            )

            // When (최대 할인 가능금액 = 7000)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 50000,
                maxDiscountLimit = 7000
            )

            // Then (최적해: 6520원)
            val totalDiscount = result.sumOf { it.calculateDiscount(50000) }
            assertThat(totalDiscount).isEqualTo(6520)
        }

        @Test
        fun `혼합 프로모션 10개에서 최대 할인 가능금액에 가장 가까운 조합을 선택한다`() {
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
                maxDiscountLimit = 13000
            )

            // Then (최적해: 12527원)
            val totalDiscount = result.sumOf { it.calculateDiscount(50000) }
            assertThat(totalDiscount).isEqualTo(12527)
        }

        @Test
        fun `혼합 프로모션 20개에서 최대 할인 가능금액에 가장 가까운 조합을 선택한다`() {
            // Given (50000원 기준, 다양한 금액 조합)
            val promotions = listOf(
                createPromotion(id = "m1", discountType = DiscountType.FIXED, discountValue = 503, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m2", discountType = DiscountType.PERCENTAGE, discountValue = 5, maxDiscountAmount = 1009, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "m3", discountType = DiscountType.FIXED, discountValue = 1501, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m4", discountType = DiscountType.PERCENTAGE, discountValue = 8, maxDiscountAmount = 2007, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "m5", discountType = DiscountType.FIXED, discountValue = 2503, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m6", discountType = DiscountType.PERCENTAGE, discountValue = 12, maxDiscountAmount = 3001, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "m7", discountType = DiscountType.FIXED, discountValue = 3509, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m8", discountType = DiscountType.PERCENTAGE, discountValue = 15, maxDiscountAmount = 4003, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "m9", discountType = DiscountType.FIXED, discountValue = 4507, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m10", discountType = DiscountType.PERCENTAGE, discountValue = 18, maxDiscountAmount = 5001, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "m11", discountType = DiscountType.FIXED, discountValue = 1003, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m12", discountType = DiscountType.PERCENTAGE, discountValue = 6, maxDiscountAmount = 1507, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "m13", discountType = DiscountType.FIXED, discountValue = 2011, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m14", discountType = DiscountType.PERCENTAGE, discountValue = 9, maxDiscountAmount = 2509, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "m15", discountType = DiscountType.FIXED, discountValue = 3007, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m16", discountType = DiscountType.PERCENTAGE, discountValue = 13, maxDiscountAmount = 3503, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "m17", discountType = DiscountType.FIXED, discountValue = 4001, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m18", discountType = DiscountType.PERCENTAGE, discountValue = 16, maxDiscountAmount = 4509, provider = PromotionProvider.CARD_ISSUER),
                createPromotion(id = "m19", discountType = DiscountType.FIXED, discountValue = 5003, provider = PromotionProvider.PLATFORM),
                createPromotion(id = "m20", discountType = DiscountType.PERCENTAGE, discountValue = 20, maxDiscountAmount = 6007, provider = PromotionProvider.CARD_ISSUER)
            )

            // When (최대 할인 가능금액 = 25000)
            val result = KnapsackPromotionOptimizer.optimize(
                promotions,
                originalAmount = 50000,
                maxDiscountLimit = 25000
            )

            // Then (최적해: 24575원)
            val totalDiscount = result.sumOf { it.calculateDiscount(50000) }
            assertThat(totalDiscount).isEqualTo(24575)
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
