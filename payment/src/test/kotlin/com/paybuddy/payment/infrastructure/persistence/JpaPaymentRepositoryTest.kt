package com.paybuddy.payment.infrastructure.persistence

import com.paybuddy.payment.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JpaPaymentRepository 테스트")
class JpaPaymentRepositoryTest {

    @Autowired
    private lateinit var sut: JpaPaymentRepository

    @BeforeEach
    fun tearDown() {
        sut.deleteAll()
    }

    @Test
    fun `JSONB와 Embeddable 복합 타입 저장 및 조회`() {
        // Given
        val payment = Payment(
            id = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
            paymentKey = "01JGXM9K3V7N2P8Q4R5S6T7U9W",
            merchantId = "mch_123",
            status = PaymentStatus.INITIALIZED,
            originalAmount = 10000
        )

        // JSONB: effectivePromotions
        payment.addPromotion(
            EffectivePromotion(
                name = "신규 회원 할인",
                provider = PromotionProvider.PLATFORM,
                amount = 1000
            ),
            minPaymentAmount = 1000
        )

        // @Embeddable: CardPaymentDetails > Card
        val cardDetails = CardPaymentDetails(
            card = Card(
                maskedNumber = "1234-56**-****-7890",
                bin = "123456",
                brand = CardBrand.VISA,
                issuerCode = "04",
                acquirerCode = "04",
                cardType = CardType.CREDIT,
                ownerType = OwnerType.PERSONAL,
                issuedCountry = "KR",
                productCode = null
            ),
            installmentMonths = 0
        )

        payment.submit(cardDetails)
        payment.fail("CARD_DECLINED", "카드사 승인 거절")
        sut.save(payment)

        // When
        val entity = sut.findByIdOrNull(payment.id)

        // Then
        assertThat(entity).isNotNull
        requireNotNull(entity)

        // JSONB 검증
        assertThat(entity.finalAmount).isEqualTo(9000)

        // @Embeddable Card 검증
        assertThat(entity.cardPaymentDetails?.card?.maskedNumber).isEqualTo("1234-56**-****-7890")
        assertThat(entity.cardPaymentDetails?.card?.brand).isEqualTo(CardBrand.VISA)
        assertThat(entity.cardPaymentDetails?.card?.cardType).isEqualTo(CardType.CREDIT)
        assertThat(entity.cardPaymentDetails?.card?.ownerType).isEqualTo(OwnerType.PERSONAL)

        // @Embeddable PaymentResult 검증
        assertThat(entity.status).isEqualTo(PaymentStatus.FAILED)
        assertThat(entity.cardPaymentDetails?.result?.errorCode).isEqualTo("CARD_DECLINED")
        assertThat(entity.cardPaymentDetails?.result?.failureReason).isEqualTo("카드사 승인 거절")
    }

    @Test
    fun `paymentKey로 조회`() {
        // Given
        val payment = Payment(
            id = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
            paymentKey = "01JGXM9K3V7N2P8Q4R5S6T7U9W",
            merchantId = "mch_123",
            status = PaymentStatus.INITIALIZED,
            originalAmount = 10000
        )

        sut.save(payment)

        // When
        val entity = sut.findByPaymentKey("01JGXM9K3V7N2P8Q4R5S6T7U9W")

        // Then
        assertThat(entity).isNotNull
        assertThat(entity?.id).isEqualTo(payment.id)
    }
}
