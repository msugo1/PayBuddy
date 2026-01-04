package com.paybuddy.payment.infrastructure.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("RedisExclusivePaymentGate 통합 테스트")
class RedisExclusivePaymentGateTest {

    @Autowired
    private lateinit var paymentGate: RedisExclusivePaymentGate

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var properties: PaymentGateProperties

    @BeforeEach
    fun setUp() {
        redisTemplate.keys("payment:gate:*").forEach { redisTemplate.delete(it) }
    }

    @Test
    fun `멀티스레드 환경에서 동시 진입 시 하나만 성공한다`() {
        // Given
        val merchantId = "merchant_123"
        val orderId = "order_456"
        val threadCount = 10
        val successCount = AtomicInteger(0)
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        // When: 10개 스레드가 동시에 진입 시도
        repeat(threadCount) {
            executor.submit {
                latch.countDown()
                latch.await() // 모든 스레드가 준비될 때까지 대기
                if (paymentGate.tryEnter(merchantId, orderId)) {
                    successCount.incrementAndGet()
                }
            }
        }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        // Then: 정확히 하나만 성공
        assertThat(successCount.get()).isEqualTo(1)
    }

    @Test
    fun `exit 후 다시 진입 가능하다`() {
        // Given
        val merchantId = "merchant_123"
        val orderId = "order_456"

        // When
        val firstEnter = paymentGate.tryEnter(merchantId, orderId)
        paymentGate.exit(merchantId, orderId)
        val secondEnter = paymentGate.tryEnter(merchantId, orderId)

        // Then
        assertThat(firstEnter).isTrue()
        assertThat(secondEnter).isTrue()
    }

    @Test
    fun `락 생성 시 설정한 TTL 값이 적용된다`() {
        // Given
        val merchantId = "merchant_123"
        val orderId = "order_456"

        // When
        paymentGate.tryEnter(merchantId, orderId)
        val key = "payment:gate:$merchantId:$orderId"
        val ttl = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS)

        // Then: application-test.yml의 설정값이 적용됨
        val expectedTtl = properties.lockTtlSeconds
        assertThat(ttl).isBetween(expectedTtl - 1, expectedTtl)
    }

    @Test
    fun `서로 다른 주문은 동시에 락을 획득할 수 있다`() {
        // When
        val enter1 = paymentGate.tryEnter("merchant_1", "order_1")
        val enter2 = paymentGate.tryEnter("merchant_2", "order_2")

        // Then: 둘 다 성공
        assertThat(enter1).isTrue()
        assertThat(enter2).isTrue()
    }

    @Test
    fun `다른 스레드는 락을 해제할 수 없다`() {
        // Given
        val merchantId = "merchant_123"
        val orderId = "order_456"
        val key = "payment:gate:$merchantId:$orderId"
        val latchReady = CountDownLatch(1)
        val latchExit = CountDownLatch(1)
        val latchCleanup = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        // When: Thread-1이 락 획득하고 유지
        executor.submit {
            paymentGate.tryEnter(merchantId, orderId)
            latchReady.countDown()
            latchCleanup.await()  // 테스트 완료까지 대기
        }
        latchReady.await()

        // Redis에 락이 존재함을 확인
        val lockExistsBeforeExit = redisTemplate.hasKey(key)

        // Thread-2가 락을 해제 시도
        executor.submit {
            paymentGate.exit(merchantId, orderId)
            latchExit.countDown()
        }
        latchExit.await()

        // Thread-2의 exit 직후 확인 (TTL 만료 전)
        val lockExistsAfterExit = redisTemplate.hasKey(key)

        // Then: Thread-2의 exit 호출에도 락은 여전히 존재
        assertThat(lockExistsBeforeExit).isTrue()
        assertThat(lockExistsAfterExit).isTrue()

        // Cleanup
        latchCleanup.countDown()
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.SECONDS)
    }
}
