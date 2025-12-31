package com.paybuddy.payment.api

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi
import com.atlassian.oai.validator.report.LevelResolver
import com.atlassian.oai.validator.report.ValidationReport.Level
import com.paybuddy.payment.config.StubPaymentService
import com.paybuddy.payment.service.PaymentOperations
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

// TODO: web mvc test 가 slice test 어노테이션이라 그런지 내부에 test config 를 두어도 import 없이는 인식하지 못함.
//  현재 API 테스트는 향후 spring boot test 로 전환해야 하는데,
//  작업 범위에서 벗어나기도 하고 openapi spec validator 의 경우 mockmvc 로 검증하고 있어서 이 부분에 대한 처리 고민이 필요하다.
//  향후 테스트 전환 예정
@Import(PaymentsApiTest.PaymentTestConfig::class)
@WebMvcTest(PaymentsApiController::class)
class PaymentsApiTest {

    @TestConfiguration
    class PaymentTestConfig {

        @Bean
        @Primary
        fun paymentOperations(): PaymentOperations {
            return StubPaymentService()
        }

        @Bean
        fun idempotencyValidator(): IdempotencyValidator {
            return IdempotencyValidator()
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val specFile = File("../docs/openapi/api/payment.yaml").absoluteFile
    private val validator = OpenApiInteractionValidator
        .createForSpecificationUrl(specFile.toURI().toString())
        .withLevelResolver(
            LevelResolver.create()
                // swagger-request-validator는 요청/응답을 함께 검증한다.
                // 본 테스트에서는 요청 유효성은 서버(@Valid) 책임으로 두고,
                // 응답 계약 검증에 집중하기 위해 request body 검증을 무시한다.
                .withLevel("validation.request.body", Level.IGNORE)
                .build()
        )
        .build()

    @Nested
    @DisplayName("결제 준비 API (/payments/ready)")
    inner class PaymentsReadyApiTest {

        @Test
        fun `결제 준비 성공 - 201 Created`() {
            mockMvc.perform(
                post("/payments/ready")
                    .header("Idempotency-Key", "test-key-201")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "merchant_id": "mch_1234567890",
                            "order_id": "order-20251210-001",
                            "order_line": {
                                "items": [
                                    {
                                        "name": "아메리카노",
                                        "quantity": 2,
                                        "unit_amount": 4500,
                                        "image_url": "https://cdn.example.com/products/americano.jpg"
                                    }
                                ]
                            },
                            "total_amount": 50000,
                            "supply_amount": 45455,
                            "vat_amount": 4545,
                            "success_url": "https://merchant.example.com/pay/success",
                            "fail_url": "https://merchant.example.com/pay/fail"
                        }
                        """.trimIndent()
                    )
            )
                .andExpect(status().isCreated)
                .andExpect(openApi().isValid(validator))
        }

        @Test
        fun `API 스펙(Contract)에 어긋난 payload 를 전달한 경우 - 400 Bad Request`() {
            mockMvc.perform(
                post("/payments/ready")
                    .header("Idempotency-Key", "test-key-400")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "merchant_id": "mch_1234567890",
                            "order_id": "order-20251210-001",
                            "order_line": {
                                "items": [
                                    {
                                        "name": "아메리카노",
                                        "quantity": 2,
                                        "unit_amount": 4500,
                                        "image_url": "https://cdn.example.com/products/americano.jpg"
                                    }
                                ]
                            },
                            "total_amount": -1000,
                            "supply_amount": -990,
                            "vat_amount": -10,
                            "success_url": "https://merchant.example.com/pay/success",
                            "fail_url": "https://merchant.example.com/pay/fail"
                        }
                        """.trimIndent()
                    )
            )
                .andExpect(status().isBadRequest)
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(openApi().isValid(validator))
        }

        @Disabled("인증/보안 필터가 아직 추가되지 않아 향후 401 추가필요")
        @Test
        fun `결제 준비 실패 - 401 Unauthorized (인증 실패)`() {
        }

        @Disabled("비즈니스 로직이 아직 구현되지 않아 향후 422 추가필요")
        @Test
        fun `결제 준비 실패 - 422 Unprocessable Entity (처리 불가능)`() {
        }

        @Test
        fun `동일한 결제 키로 다른 요청을 보낸 경우 (Idempotency Key 충돌) - 409 Conflict`() {
            val idempotencyKey = "test-key-409"

            // 첫 번째 요청 - 성공(201)
            mockMvc.perform(
                post("/payments/ready")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(
                        """
                    {
                        "merchant_id": "mch_1234567890",
                        "order_id": "order-20251210-001",
                        "order_line": {
                            "items": [
                                {
                                    "name": "아메리카노",
                                    "quantity": 2,
                                    "unit_amount": 4500,
                                    "image_url": "https://cdn.example.com/products/americano.jpg"
                                }
                            ]
                        },
                        "total_amount": 50000,
                        "supply_amount": 45455,
                        "vat_amount": 4545,
                        "success_url": "https://merchant.example.com/pay/success",
                        "fail_url": "https://merchant.example.com/pay/fail"
                    }
                    """.trimIndent()
                    )
            )
                .andExpect(status().isCreated)
                .andExpect(openApi().isValid(validator))

            // 두 번째 요청 - 동일 키, 다른 payload → 409
            mockMvc.perform(
                post("/payments/ready")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                    {
                        "merchant_id": "mch_1234567890",
                        "order_id": "order-20251210-001",
                        "order_line": {
                            "items": [
                                {
                                    "name": "아메리카노",
                                    "quantity": 2,
                                    "unit_amount": 4500,
                                    "image_url": "https://cdn.example.com/products/americano.jpg"
                                }
                            ]
                        },
                        "total_amount": 50001,
                        "supply_amount": 45455,
                        "vat_amount": 4545,
                        "success_url": "https://merchant.example.com/pay/success",
                        "fail_url": "https://merchant.example.com/pay/fail"
                    }
                    """.trimIndent()
                    )
            )
                .andExpect(status().isConflict)
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(openApi().isValid(validator))
        }
    }

    @Nested
    @DisplayName("결제 승인 API (/payments/confirm)")
    inner class PaymentConfirmApiTest {

        @Test
        fun `결제 승인 성공 - 200 OK`() {
            mockMvc.perform(
                post("/payments/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "payment_key": "01JGQR5K8Y9Z2N4M3X7P6W5V1T",
                            "payment_method": {
                                "type": "CARD",
                                "card": {
                                    "card_number": "1234567890123456",
                                    "expiry_month": "12",
                                    "expiry_year": "2025",
                                    "cvc": "123",
                                    "card_holder_name": "홍길동",
                                    "installment_months": 0
                                }
                            }
                        }
                        """.trimIndent()
                    )
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(openApi().isValid(validator))
        }

        @Test
        fun `잘못된 요청 (필수 필드 누락) - 400 Bad Request`() {
            mockMvc.perform(
                post("/payments/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "payment_key": "pay_test123"
                        }
                        """.trimIndent()
                    )
            )
                .andExpect(status().isBadRequest)
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(openApi().isValid(validator))
        }

        @Disabled("인증/보안 필터가 아직 추가되지 않아 향후 401 추가필요")
        @Test
        fun `결제 승인 실패 - 401 Unauthorized (인증 실패)`() {
        }

        @Disabled("비즈니스 로직이 아직 구현되지 않아 향후 422 추가필요 - 예: CANCELED 상태의 결제를 confirm 시도")
        @Test
        fun `결제 승인 실패 - 422 Unprocessable Entity (처리 불가능)`() {
        }
    }

    @Nested
    @DisplayName("결제 조회 API")
    inner class PaymentQueryApiTest {

        @Test
        fun `결제 상세 조회 - 200 OK`() {
            mockMvc.perform(
                get("/payments/{payment_key}", "pay_test123")
                    .accept(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(openApi().isValid(validator))
        }

        @Test
        fun `영수증 조회 - 200 OK`() {
            mockMvc.perform(
                get("/payments/{payment_key}/receipt", "pay_test123")
                    .accept(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(openApi().isValid(validator))
        }

        @Test
        fun `영수증 조회 (format=json) - 200 OK`() {
            mockMvc.perform(
                get("/payments/{payment_key}/receipt", "pay_test123")
                    .param("format", "json")
                    .accept(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(openApi().isValid(validator))
        }
    }
}