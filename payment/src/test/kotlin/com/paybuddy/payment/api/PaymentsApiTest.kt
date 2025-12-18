package com.paybuddy.payment.api

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi
import com.atlassian.oai.validator.report.LevelResolver
import com.atlassian.oai.validator.report.ValidationReport.Level
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

@WebMvcTest(PaymentsApiController::class)
class PaymentsApiTest {

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
                            "total_amount": 50000,
                            "supply_amount": 45455,
                            "vat_amount": 4545,
                            "redirect_url": "https://merchant.example.com/pay/return"
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
                            "total_amount": -1000,
                            "supply_amount": -990,
                            "vat_amount": -10,
                            "redirect_url": "https://merchant.example.com/pay/return"
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
                        "total_amount": 50000,
                        "supply_amount": 45455,
                        "vat_amount": 4545,
                        "redirect_url": "https://merchant.example.com/pay/return"
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
                        "total_amount": 50001,
                        "supply_amount": 45455,
                        "vat_amount": 4545,
                        "redirect_url": "https://merchant.example.com/pay/return"
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
                            "payment_key": "pay_test123",
                            "payment_method": {
                                "type": "CARD",
                                "card": {
                                    "card_number": "1234567890123456",
                                    "expiry_month": "12",
                                    "expiry_year": "2025",
                                    "cvc": "123"
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
}