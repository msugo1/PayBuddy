package com.paybuddy.payment.application

import com.paybuddy.payment.PaymentApplicationTests
import com.paybuddy.payment.application.dto.SubmitCardPaymentCommand
import com.paybuddy.payment.application.dto.SubmitPaymentResult
import com.paybuddy.payment.application.dto.SubmitStatus
import com.paybuddy.payment.application.dto.AuthenticationType
import com.paybuddy.payment.application.dto.AuthenticationMethod
import com.paybuddy.payment.domain.*
import com.paybuddy.payment.domain.installment.Installment
import com.paybuddy.payment.domain.merchant.MerchantLimitExceededException
import com.paybuddy.payment.infrastructure.persistence.JpaPaymentRepository
import com.paybuddy.payment.infrastructure.persistence.JpaPaymentSessionRepository
import com.paybuddy.payment.infrastructure.persistence.JpaPromotionRepository
import com.github.f4b6a3.ulid.Ulid
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@Import(PaymentApplicationTests.TestConfig::class)
@DisplayName("CardPaymentUseCase 통합 테스트")
class CardPaymentUseCaseTest {

    @Autowired
    private lateinit var cardPaymentUseCase: CardPaymentUseCase

    @Autowired
    private lateinit var paymentRepository: JpaPaymentRepository

    @Autowired
    private lateinit var paymentSessionRepository: JpaPaymentSessionRepository

    @Autowired
    private lateinit var promotionRepository: JpaPromotionRepository

    @BeforeEach
    fun setUp() {
        paymentSessionRepository.deleteAll()
        paymentRepository.deleteAll()
        promotionRepository.deleteAll()
    }

    @Test
    fun `인증이 필요없는 카드로 결제를 제출하면 확인 대기 상태가 된다`() {
        // Given
        val session = createPaymentSession(amount = 50_000)
        paymentSessionRepository.save(session)

        val request = createSubmitRequest(
            paymentKey = session.id,
            installmentMonths = 0
        )

        // When
        val result = cardPaymentUseCase.submit(request)

        // Then - Result 검증
        assertThat(result).isEqualTo(
            SubmitPaymentResult(
                paymentKey = session.id,
                status = SubmitStatus.PENDING_CONFIRM,
                authentication = null,
                redirectUrl = session.redirectUrl.success
            )
        )

        // Then - Payment 엔티티 검증
        val payment = paymentRepository.findByPaymentKey(session.id)!!
        assertThat(payment.paymentKey).isEqualTo(session.id)
        assertThat(payment.merchantId).isEqualTo("mch_123")
        assertThat(payment.status).isEqualTo(PaymentStatus.PENDING_CONFIRM)
        assertThat(payment.paymentMethodType).isEqualTo(PaymentMethodType.CARD)
        assertThat(payment.originalAmount).isEqualTo(50_000)
        assertThat(payment.finalAmount).isEqualTo(50_000)
        assertThat(payment.paymentResult).isNull()
        assertThat(payment.effectivePromotions).isEmpty()
        assertThat(payment.cardPaymentDetails).isEqualTo(
            CardPaymentDetails(
                card = Card(
                    maskedNumber = "4532********0366",
                    expiryMonth = 12,
                    expiryYear = 30,
                    holderName = "홍길동",
                    bin = "453201",
                    brand = CardBrand.VISA,
                    issuerCode = "SHINHAN",
                    acquirerCode = "KB",
                    cardType = CardType.CREDIT,
                    ownerType = OwnerType.PERSONAL,
                    issuedCountry = "KR",
                    productCode = "GENERAL"
                ),
                installment = Installment(months = 0, isInterestFree = false)
            )
        )
    }

    @Test
    fun `인증이 필요한 카드로 결제를 제출하면 인증 단계로 진행된다`() {
        // Given
        val session = createPaymentSession(amount = 150_000)
        paymentSessionRepository.save(session)

        val request = createSubmitRequest(
            paymentKey = session.id,
            installmentMonths = 0
        )

        // When
        val result = cardPaymentUseCase.submit(request)

        // Then - Result 검증
        assertThat(result.paymentKey).isEqualTo(session.id)
        assertThat(result.status).isEqualTo(SubmitStatus.AUTHENTICATION_REQUIRED)
        assertThat(result.authentication).isNotNull()
        assertThat(result.authentication!!.type).isEqualTo(AuthenticationType.ISP)
        assertThat(result.authentication!!.method).isEqualTo(AuthenticationMethod.REDIRECT)
        assertThat(result.redirectUrl).isNull()

        // Then - Payment 엔티티 검증
        val payment = paymentRepository.findByPaymentKey(session.id)!!
        assertThat(payment.paymentKey).isEqualTo(session.id)
        assertThat(payment.merchantId).isEqualTo("mch_123")
        assertThat(payment.status).isEqualTo(PaymentStatus.AUTHENTICATION_REQUIRED)
        assertThat(payment.paymentMethodType).isEqualTo(PaymentMethodType.CARD)
        assertThat(payment.originalAmount).isEqualTo(150_000)
        assertThat(payment.finalAmount).isEqualTo(150_000)
        assertThat(payment.paymentResult).isNull()
        assertThat(payment.effectivePromotions).isEmpty()
        assertThat(payment.cardPaymentDetails!!.installment).isEqualTo(
            Installment(months = 0, isInterestFree = false)
        )
    }

    @Test
    fun `할부 정보와 함께 결제를 제출할 수 있다`() {
        // Given
        val session = createPaymentSession(amount = 100_000)
        paymentSessionRepository.save(session)

        val request = createSubmitRequest(
            paymentKey = session.id,
            installmentMonths = 3
        )

        // When
        cardPaymentUseCase.submit(request)

        // Then
        val payment = paymentRepository.findByPaymentKey(session.id)!!
        assertThat(payment.merchantId).isEqualTo("mch_123")
        assertThat(payment.status).isIn(PaymentStatus.PENDING_CONFIRM, PaymentStatus.AUTHENTICATION_REQUIRED)
        assertThat(payment.paymentMethodType).isEqualTo(PaymentMethodType.CARD)
        assertThat(payment.originalAmount).isEqualTo(100_000)
        assertThat(payment.finalAmount).isEqualTo(100_000)
        assertThat(payment.paymentResult).isNull()
        assertThat(payment.effectivePromotions).isEmpty()
        assertThat(payment.cardPaymentDetails).isNotNull()
        assertThat(payment.cardPaymentDetails!!.installment!!.months).isEqualTo(3)
    }

    @Test
    fun `가맹점 한도를 초과하면 결제가 실패한다`() {
        // Given
        val session = createPaymentSession(amount = 100_000_001)
        paymentSessionRepository.save(session)

        val request = createSubmitRequest(paymentKey = session.id)

        // When & Then
        assertThatThrownBy {
            cardPaymentUseCase.submit(request)
        }.isInstanceOf(MerchantLimitExceededException::class.java)

        // Payment.fail() 호출 확인
        val payment = paymentRepository.findByPaymentKey(session.id)!!
        assertThat(payment.merchantId).isEqualTo("mch_123")
        assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
        assertThat(payment.paymentMethodType).isEqualTo(PaymentMethodType.CARD)
        assertThat(payment.originalAmount).isEqualTo(100_000_001)
        assertThat(payment.finalAmount).isEqualTo(100_000_001)
        assertThat(payment.effectivePromotions).isEmpty()
        assertThat(payment.cardPaymentDetails).isNull()
        assertThat(payment.paymentResult).isNotNull()
        assertThat(payment.paymentResult!!.errorCode).isEqualTo("VALIDATION_FAILED")
        assertThat(payment.paymentResult!!.failureReason).isNotBlank()
    }

    @Test
    fun `이미 제출된 결제는 다시 제출할 수 없다`() {
        // Given
        val session = createPaymentSession(amount = 10_000)
        paymentSessionRepository.save(session)

        val request = createSubmitRequest(paymentKey = session.id)

        cardPaymentUseCase.submit(request)

        // When & Then
        assertThatThrownBy {
            cardPaymentUseCase.submit(request)
        }.isInstanceOf(PaymentAlreadySubmittedException::class.java)
    }

    @Test
    fun `만료된 결제세션으로는 결제를 제출할 수 없다`() {
        // Given
        val session = createPaymentSession(
            amount = 10_000,
            expiresAt = OffsetDateTime.now().minusMinutes(1)
        )
        paymentSessionRepository.save(session)

        val request = createSubmitRequest(paymentKey = session.id)

        // When & Then
        assertThatThrownBy {
            cardPaymentUseCase.submit(request)
        }.isInstanceOf(PaymentSessionExpiredException::class.java)
    }

    @Test
    fun `적용 가능한 프로모션은 자동으로 결제에 반영된다`() {
        // Given
        val session = createPaymentSession(amount = 50_000)
        paymentSessionRepository.save(session)

        val promotion = Promotion(
            id = "promo_1",
            name = "VISA 3천원 할인",
            provider = PromotionProvider.CARD_ISSUER,
            discountType = DiscountType.FIXED,
            discountValue = 3_000,
            maxDiscountAmount = null,
            cardBrand = CardBrand.VISA,
            cardType = null,
            issuerCode = null,
            minAmount = null,
            validFrom = Instant.now().minusSeconds(86400),
            validUntil = Instant.now().plusSeconds(86400)
        )
        promotionRepository.save(promotion)

        val request = createSubmitRequest(paymentKey = session.id)

        // When
        cardPaymentUseCase.submit(request)

        // Then
        val payment = paymentRepository.findByPaymentKey(session.id)!!
        assertThat(payment.merchantId).isEqualTo("mch_123")
        assertThat(payment.status).isIn(PaymentStatus.PENDING_CONFIRM, PaymentStatus.AUTHENTICATION_REQUIRED)
        assertThat(payment.paymentMethodType).isEqualTo(PaymentMethodType.CARD)
        assertThat(payment.originalAmount).isEqualTo(50_000)
        assertThat(payment.finalAmount).isEqualTo(47_000)
        assertThat(payment.paymentResult).isNull()
        assertThat(payment.cardPaymentDetails).isNotNull()
        assertThat(payment.effectivePromotions).isEqualTo(
            listOf(
                EffectivePromotion(
                    name = "VISA 3천원 할인",
                    provider = PromotionProvider.CARD_ISSUER,
                    amount = 3_000
                )
            )
        )
    }

    private fun createPaymentSession(
        amount: Long = 10_000,
        expiresAt: OffsetDateTime = OffsetDateTime.now().plusMinutes(15)
    ): PaymentSession {
        return PaymentSession(
            id = Ulid.fast().toString(),
            merchantId = "mch_123",
            orderId = "order_456",
            orderLine = OrderLine(
                listOf(
                    OrderLineItem(
                        name = "테스트상품",
                        quantity = 1,
                        unitAmount = amount,
                        imageUrl = "https://example.com/product.jpg"
                    )
                )
            ),
            amount = PaymentAmount(amount, amount, 0),
            expiresAt = expiresAt,
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        )
    }

    private fun createSubmitRequest(
        paymentKey: String,
        cardNumber: String = "4532015112830366",
        installmentMonths: Int = 0
    ): SubmitCardPaymentCommand {
        return SubmitCardPaymentCommand(
            paymentKey = paymentKey,
            cardNumber = cardNumber,
            expiryMonth = 12,
            expiryYear = 30,
            holderName = "홍길동",
            cvc = "123",
            installmentMonths = installmentMonths
        )
    }
}
