package com.paybuddy.payment.application

import com.paybuddy.payment.api.model.PaymentSubmitRequest
import com.paybuddy.payment.api.model.PaymentSubmitRequest.PaymentMethodTypeEnum
import com.paybuddy.payment.application.dto.SubmitCardPaymentCommand
import com.paybuddy.payment.application.dto.SubmitPaymentResponse
import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.service.PaymentOperations
import org.springframework.stereotype.Component

@Component
class PaymentOperationsRouter(
    useCases: List<PaymentOperations>
) {
    private val routes = useCases.associateBy { it.paymentMethodType }

    fun submit(request: PaymentSubmitRequest): SubmitPaymentResponse {
        return when (request.paymentMethodType) {
            PaymentMethodTypeEnum.CARD -> submitCard(request)
            PaymentMethodTypeEnum.VIRTUAL_ACCOUNT -> TODO()
        }
    }

    private fun submitCard(request: PaymentSubmitRequest): SubmitPaymentResponse {
        val card = request.card ?: error("Card information is required")

        val command = SubmitCardPaymentCommand(
            paymentKey = request.paymentKey,
            cardNumber = card.cardNumber ?: error("Card number is required"),
            expiryMonth = card.expiryMonth?.toInt() ?: error("Expiry month is required"),
            expiryYear = card.expiryYear?.toInt() ?: error("Expiry year is required"),
            cvc = card.cvc ?: error("CVC is required"),
            holderName = card.holderName,
            installmentMonths = card.installmentMonths ?: 0
        )

        val useCase = routes[PaymentMethodType.CARD] as CardPaymentUseCase
        return useCase.submit(command)
    }
}
