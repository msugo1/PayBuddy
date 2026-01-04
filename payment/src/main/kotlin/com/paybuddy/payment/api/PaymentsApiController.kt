package com.paybuddy.payment.api

import com.paybuddy.payment.api.model.NextActionNone
import com.paybuddy.payment.api.model.PaymentConfirmRequest
import com.paybuddy.payment.api.model.PaymentConfirmResponse
import com.paybuddy.payment.api.model.PaymentDetailResponse
import com.paybuddy.payment.api.model.PaymentReadyRequest
import com.paybuddy.payment.api.model.PaymentReadyResponse
import com.paybuddy.payment.api.model.ReceiptResponse
import com.paybuddy.payment.domain.DuplicatePaymentRequestException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
class PaymentsApiController : PaymentsApi {

    @ExceptionHandler(DuplicatePaymentRequestException::class)
    fun handleDuplicatePaymentRequest(
        e: DuplicatePaymentRequestException
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        problem.type = URI.create("urn:paybuddy:problem:duplicate-payment-request")
        problem.title = "Duplicate payment request"
        problem.detail = "Another payment request is already processing this order."
        problem.setProperty("error_code", "DUPLICATE_PAYMENT_REQUEST")

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(problem)
    }

    @ExceptionHandler(IdempotencyConflictException::class)
    fun handleIdempotencyConflict(
        e: IdempotencyConflictException
    ): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatus(HttpStatus.CONFLICT)
        problem.type = URI.create("urn:paybuddy:problem:idempotency-conflict")
        problem.title = "Idempotency conflict"
        problem.detail = "The same Idempotency-Key was used with a different request payload."
        problem.setProperty("error_code", "PAYMENT_REQUEST_MISMATCH")

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(problem)
    }

    private val idempotencyStorage = mutableMapOf<String, String>()

    override fun getPayment(paymentKey: String): ResponseEntity<PaymentDetailResponse> {
        TODO("Not yet implemented")
    }

    override fun getPaymentReceipt(
        paymentKey: String,
        format: String
    ): ResponseEntity<ReceiptResponse> {
        TODO("Not yet implemented")
    }

    override fun readyPayment(
        idempotencyKey: String,
        paymentReadyRequest: PaymentReadyRequest
    ): ResponseEntity<PaymentReadyResponse> {
        verifyIdempotentRequest(idempotencyKey, paymentReadyRequest)

        val response = PaymentReadyResponse(
            "01JGQR5K8Y9Z2N4M3X7P6W5V1T",
            "https://payment.paybuddy.com/checkout",
            OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10)
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(response)
    }

    private fun verifyIdempotentRequest(idempotencyKey: String, request: com.paybuddy.payment.api.model.PaymentReadyRequest) {
        // FIXME: 일단 제대로 구현하기 전까지는 임의로 hash 되었다고 간주한다.
        val currentPaymentRequestHash = "${request.merchantId}:${request.orderId}:${request.totalAmount}"

        val previousPaymentRequestHash = idempotencyStorage[idempotencyKey]
        if (previousPaymentRequestHash == null) {
            idempotencyStorage[idempotencyKey] = currentPaymentRequestHash
            return
        }

        if (currentPaymentRequestHash == previousPaymentRequestHash) {
            return
        }

        throw IdempotencyConflictException(idempotencyKey)
    }

    override fun confirmPayment(paymentConfirmRequest: PaymentConfirmRequest?): ResponseEntity<PaymentConfirmResponse> {
        // Mock implementation for contract testing
        val response = PaymentConfirmResponse(
            "pay_1234567890",
            "01JGQR5K8Y9Z2N4M3X7P6W5V1T",
            "order-20251210-001",
            "CAPTURED",
            50000,
            49900,
            100,
            OffsetDateTime.now(ZoneOffset.UTC)
        )
            .approvedAt(OffsetDateTime.now(ZoneOffset.UTC))
            .receiptUrl(URI.create("https://api.paybuddy.com/v1/payments/01JGQR5K8Y9Z2N4M3X7P6W5V1T/receipt"))
            .nextAction(NextActionNone("none"))

        return ResponseEntity.ok(response)
    }
}
