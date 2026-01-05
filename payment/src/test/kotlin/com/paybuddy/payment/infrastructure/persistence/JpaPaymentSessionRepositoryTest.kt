package com.paybuddy.payment.infrastructure.persistence

import com.paybuddy.payment.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JpaPaymentSessionRepository 테스트")
class JpaPaymentSessionRepositoryTest {

    @Autowired
    private lateinit var sut: JpaPaymentSessionRepository

    @BeforeEach
    fun tearDown() {
        sut.deleteAll()
    }

    @Test
    fun `진행중인 세션이 있으면 같은 merchantId와 orderId로 새 세션 생성 시 unique constraint 위반이 발생한다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"

        val ongoingSession = PaymentSession(
            id = "01JGXM9K3V7N2P8Q4R5S6T7U9A",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = OrderLine(items = listOf(OrderLineItem("상품", 1, 10000, "url"))),
            amount = PaymentAmount(10000, 9091, 909),
            expiresAt = OffsetDateTime.parse("2025-12-31T23:59:59+09:00"),
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        )

        sut.saveAndFlush(ongoingSession)

        // When & Then
        val duplicateSession = PaymentSession(
            id = "02JGXM9K3V7N2P8Q4R5S6T7U9A",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = OrderLine(items = listOf(OrderLineItem("상품", 1, 10000, "url"))),
            amount = PaymentAmount(10000, 9091, 909),
            expiresAt = OffsetDateTime.parse("2025-12-31T23:59:59+09:00"),
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        )

        assertThatThrownBy {
            sut.saveAndFlush(duplicateSession)
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `만료된 세션이 있으면 같은 merchantId와 orderId로 새 세션을 생성할 수 있다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val expiredSession = PaymentSession(
            id = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = OrderLine(items = listOf(OrderLineItem("상품", 1, 10000, "url"))),
            amount = PaymentAmount(10000, 9091, 909),
            expiresAt = OffsetDateTime.parse("2025-12-31T23:59:59+09:00"),
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        ).apply {
            expire()
        }

        sut.save(expiredSession)

        // When
        val newSession = PaymentSession(
            id = "01JGXM9K3V7N2P8Q4R5S6T7U9W",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = OrderLine(items = listOf(OrderLineItem("상품", 1, 10000, "url"))),
            amount = PaymentAmount(10000, 9091, 909),
            expiresAt = OffsetDateTime.parse("2025-12-31T23:59:59+09:00"),
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        )

        val entity = sut.save(newSession)

        // Then
        assertThat(entity.id).isEqualTo(newSession.id)
        assertThat(entity.merchantId).isEqualTo(merchantId)
        assertThat(entity.orderId).isEqualTo(orderId)
        assertThat(entity.expired).isFalse()
    }

    @Test
    fun `findOngoingPaymentSession은 expired가 false인 세션을 조회한다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val session = PaymentSession(
            id = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = OrderLine(items = listOf(OrderLineItem("상품", 1, 10000, "url"))),
            amount = PaymentAmount(10000, 9091, 909),
            expiresAt = OffsetDateTime.parse("2025-12-31T23:59:59+09:00"),
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        )

        sut.save(session)

        // When
        val ongoingPaymentSession = sut.findByMerchantIdAndOrderIdAndExpiredFalse(merchantId, orderId)

        // Then
        assertThat(ongoingPaymentSession).isNotNull
        checkNotNull(ongoingPaymentSession)

        assertThat(ongoingPaymentSession.id).isEqualTo(session.id)
        assertThat(ongoingPaymentSession.merchantId).isEqualTo(merchantId)
        assertThat(ongoingPaymentSession.orderId).isEqualTo(orderId)
        assertThat(ongoingPaymentSession.expired).isFalse()
    }

    @Test
    fun `findOngoingPaymentSession은 expired가 true인 세션을 제외한다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val expiredSession = PaymentSession(
            id = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = OrderLine(items = listOf(OrderLineItem("상품", 1, 10000, "url"))),
            amount = PaymentAmount(10000, 9091, 909),
            expiresAt = OffsetDateTime.parse("2025-12-31T23:59:59+09:00"),
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        ).apply { expire() }

        sut.save(expiredSession)

        // When
        val entity = sut.findByMerchantIdAndOrderIdAndExpiredFalse(merchantId, orderId)

        // Then
        assertThat(entity).isNull()
    }

    @Test
    fun `JSONB 타입인 OrderLine 정상적 저장 및 조회`() {
        // Given
        val orderLine = OrderLine(
            items = listOf(
                OrderLineItem("프리미엄 구독", 1, 9000, "https://img1.png"),
                OrderLineItem("부가 서비스", 2, 500, "https://img2.png")
            )
        )

        val session = PaymentSession(
            id = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
            merchantId = "mch_123",
            orderId = "order_456",
            orderLine = orderLine,
            amount = PaymentAmount(10000, 9091, 909),
            expiresAt = OffsetDateTime.parse("2025-12-31T23:59:59+09:00"),
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        )

        sut.save(session)

        // When
        val entity = sut.findByIdOrNull(session.id)


        // Then
        assertThat(entity).isNotNull
        requireNotNull(entity)

        assertThat(entity.orderLine.items).hasSize(2)
        assertThat(entity.orderLine).isEqualTo(orderLine)
    }

    @Test
    @DisplayName("새 세션 save 시 불필요한 SELECT 쿼리 발생 여부 확인 (Persistable 필요성 검증)")
    fun `새 세션 저장 시 SELECT 쿼리가 먼저 실행되는지 확인`() {
        // Given
        val newSession = PaymentSession(
            id = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
            merchantId = "mch_new",
            orderId = "order_new",
            orderLine = OrderLine(items = listOf(OrderLineItem("상품", 1, 10000, "url"))),
            amount = PaymentAmount(10000, 9091, 909),
            expiresAt = OffsetDateTime.parse("2025-12-31T23:59:59+09:00"),
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        )

        // When
        // SQL 로그를 보고 SELECT가 먼저 나오는지 확인
        // application-test.yml에서 show-sql: true 설정 필요
        val savedSession = sut.save(newSession)

        // Then
        assertThat(savedSession.id).isEqualTo(newSession.id)
        // 로그 확인:
        // 1. Persistable 없으면: SELECT -> INSERT 순서
        // 2. Persistable 있으면: INSERT만 실행
    }
}
