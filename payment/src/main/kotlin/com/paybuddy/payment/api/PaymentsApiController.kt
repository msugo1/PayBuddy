package com.paybuddy.payment.api

import com.paybuddy.payment.api.model.MerchantInfo
import com.paybuddy.payment.service.PaymentOperations
import com.paybuddy.payment.api.model.NextActionNone
import com.paybuddy.payment.api.model.PaymentConfirmRequest
import com.paybuddy.payment.api.model.PaymentConfirmResponse
import com.paybuddy.payment.api.model.PaymentDetailResponse
import com.paybuddy.payment.api.model.PaymentResponseFees
import com.paybuddy.payment.api.model.PaymentResponsePaymentMethod
import com.paybuddy.payment.api.model.PaymentResponsePaymentMethodCard
import com.paybuddy.payment.api.model.PaymentReadyRequest
import com.paybuddy.payment.api.model.PaymentReadyResponse
import com.paybuddy.payment.api.model.ReceiptResponse
import com.paybuddy.payment.api.model.ReceiptResponseMerchant
import com.paybuddy.payment.api.model.ReceiptResponseOrder
import com.paybuddy.payment.api.model.ReceiptResponsePayment
import com.paybuddy.payment.domain.OrderLine
import com.paybuddy.payment.domain.OrderLineItem
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
class PaymentsApiController(
    private val paymentOperations: PaymentOperations,
    private val idempotencyValidator: IdempotencyValidator
) : PaymentsApi {
    @ExceptionHandler(IdempotencyConflictException::class)
    fun handleIdempotencyConflict(
        e: IdempotencyConflictException
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        problem.type = URI.create("urn:paybuddy:problem:idempotency-conflict")
        problem.title = "Idempotency conflict"
        problem.detail = "The same Idempotency-Key was used with a different request payload."
        problem.setProperty("error_code", "IDEMPOTENCY_CONFLICT")

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(problem)
    }

    override fun getPayment(paymentKey: @NotNull String?): ResponseEntity<PaymentDetailResponse> {
        val response = PaymentDetailResponse()
            .paymentId("pay_1234567890")
            .paymentKey(paymentKey)
            .orderId("order-20251218-001")
            .status("CAPTURED")
            .requestedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10))
            .approvedAt(OffsetDateTime.now(ZoneOffset.UTC))
            .receiptUrl(URI.create("https://api.paybuddy.com/v1/payments/$paymentKey/receipt"))
            .totalAmount(50000)
            .supplyAmount(45455)
            .vatAmount(4545)
            .merchant(
                MerchantInfo()
                    .merchantId("mch_1234567890")
                    .merchantName("스타벅스 강남점")
                    .businessNumber("123-45-67890")
                    .mccCode("5814")
            )
            .paymentMethod(
                PaymentResponsePaymentMethod()
                    .type("CARD")
                    .card(
                        PaymentResponsePaymentMethodCard()
                            .issuer("신한카드")
                            .acquirer("KB국민카드")
                            .cardNumber("1234-56**-****-7890")
                            .cardType("CREDIT")
                            .approvalNumber("12345678")
                    )
            )
            .fees(
                PaymentResponseFees()
                    .pgFee(1500)
                    .cardFee(1000)
                    .totalFee(2500)
                    .settlementAmount(47500)
            )

        return ResponseEntity.ok(response)
    }

    override fun getPaymentReceipt(
        paymentKey: @NotNull String?,
        format: @Valid String?
    ): ResponseEntity<ReceiptResponse> {
        val response = ReceiptResponse()
            .receiptId("receipt_12345")
            .issuedAt(OffsetDateTime.now(ZoneOffset.UTC))
            .merchant(
                ReceiptResponseMerchant()
                    .name("스타벅스 강남점")
                    .businessNumber("123-45-67890")
                    .representative("홍길동")
                    .address("서울시 강남구 테헤란로 123")
                    .tel("02-1234-5678")
            )
            .order(
                ReceiptResponseOrder()
                    .orderId("order-20251218-001")
                    .orderName("아메리카노 외 2건")
                    .orderedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5))
            )
            .payment(
                ReceiptResponsePayment()
                    .method("신한카드")
                    .cardNumber("1234-56**-****-****")
                    .installment("일시불")
                    .approvalNumber("12345678")
                    .approvedAt(OffsetDateTime.now(ZoneOffset.UTC))
            )
            .printUrl(URI.create("https://api.paybuddy.com/receipts/receipt_12345/print"))
            .pdfUrl(URI.create("https://api.paybuddy.com/receipts/receipt_12345/pdf"))
            .totalAmount(15000)
            .supplyAmount(13636)
            .vatAmount(1364)

        return ResponseEntity.ok(response)
    }

    override fun readyPayment(
        idempotencyKey: @NotNull String,
        paymentReadyRequest: @Valid PaymentReadyRequest
    ): ResponseEntity<PaymentReadyResponse> {
        val requestHash = idempotencyValidator.hashRequest(
            paymentReadyRequest.merchantId,
            paymentReadyRequest.orderId,
            paymentReadyRequest.totalAmount
        )
        idempotencyValidator.validate(idempotencyKey, requestHash)

        val internalOrderLine = OrderLine(
            items = paymentReadyRequest.orderLine.items.map { apiItem ->
                OrderLineItem(
                    name = apiItem.name,
                    quantity = apiItem.quantity,
                    unitAmount = apiItem.unitAmount,
                    imageUrl = apiItem.imageUrl.toString()
                )
            }
        )

        val paymentSession = paymentOperations.prepare(
            merchantId = paymentReadyRequest.merchantId,
            orderId = paymentReadyRequest.orderId,
            orderLine = internalOrderLine,
            totalAmount = paymentReadyRequest.totalAmount,
            supplyAmount = paymentReadyRequest.supplyAmount ?: 0,
            vatAmount = paymentReadyRequest.vatAmount ?: 0,
            successUrl = paymentReadyRequest.successUrl.toString(),
            failUrl = paymentReadyRequest.failUrl.toString()
        )

        val response = PaymentReadyResponse()
            .paymentKey(paymentSession.paymentKey)
            .checkoutUrl("https://payment.paybuddy.com/checkout/${paymentSession.paymentKey}")
            .expiresAt(paymentSession.expiresAt)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    override fun confirmPayment(paymentConfirmRequest: @Valid PaymentConfirmRequest?): ResponseEntity<PaymentConfirmResponse> {
        // Mock implementation for contract testing
        val response = PaymentConfirmResponse(
            "pay_1234567890",
            "pay_test123",
            "order-20251210-001",
            "CAPTURED",
            50000,
            49900,
            100,
            OffsetDateTime.now(ZoneOffset.UTC)
        )
            .approvedAt(OffsetDateTime.now(ZoneOffset.UTC))
            .receiptUrl(URI.create("https://api.paybuddy.com/v1/payments/pay_test123/receipt"))
            .nextAction(NextActionNone("none"))

        return ResponseEntity.ok(response)
    }
}
