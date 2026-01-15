package com.paybuddy.payment.application

import com.paybuddy.payment.application.dto.AuthenticationRedirect
import com.paybuddy.payment.application.dto.SubmitCardPaymentCommand
import com.paybuddy.payment.application.dto.SubmitPaymentResponse
import com.paybuddy.payment.application.dto.SubmitStatus
import com.paybuddy.payment.domain.*
import com.paybuddy.payment.domain.authentication.AuthenticationResult
import com.paybuddy.payment.domain.authentication.AuthenticationService
import com.paybuddy.payment.domain.fraud.FraudDetectionService
import com.paybuddy.payment.domain.installment.Installment
import com.paybuddy.payment.domain.installment.InstallmentCalculator
import com.paybuddy.payment.domain.installment.IssuerInstallmentPolicyRepository
import com.paybuddy.payment.domain.merchant.MerchantContractService
import com.paybuddy.payment.domain.merchant.MerchantContractValidator
import com.paybuddy.payment.domain.merchant.MerchantLimitService
import com.paybuddy.payment.domain.service.BinLookupService
import com.paybuddy.payment.domain.service.CardCredentials
import com.paybuddy.payment.domain.service.CardVaultService
import com.paybuddy.payment.service.PaymentOperations
import org.springframework.stereotype.Service

@Service
class CardPaymentUseCase(
    private val paymentSessionService: PaymentSessionService,
    private val paymentRepository: PaymentRepository,
    private val binLookupService: BinLookupService,
    private val merchantContractValidator: MerchantContractValidator,
    private val merchantContractService: MerchantContractService,
    private val merchantLimitService: MerchantLimitService,
    private val fraudDetectionService: FraudDetectionService,
    private val installmentCalculator: InstallmentCalculator,
    private val issuerInstallmentPolicyRepository: IssuerInstallmentPolicyRepository,
    private val promotionRepository: PromotionRepository,
    private val promotionOptimizer: PromotionOptimizer,
    private val cardVaultService: CardVaultService,
    private val authenticationService: AuthenticationService,
    private val paymentPolicy: PaymentPolicy,
) : PaymentOperations {

    override val paymentMethodType: PaymentMethodType = PaymentMethodType.CARD

    fun submit(request: SubmitCardPaymentCommand): SubmitPaymentResponse {
        val ongoingPaymentSession = paymentSessionService.getOngoingSession(request.paymentKey)

        val ongoingPayment = paymentRepository.findByPaymentKey(request.paymentKey)
        if (ongoingPayment != null) {
            throw PaymentAlreadySubmittedException(request.paymentKey)
        }

        val payment = Payment.initialize(
            paymentKey = request.paymentKey,
            merchantId = ongoingPaymentSession.merchantId,
            paymentMethodType = PaymentMethodType.CARD,
            originalAmount = ongoingPaymentSession.amount.total
        )

        paymentRepository.save(payment)

        val bin = binLookupService.lookup(request.cardNumber)

        val card: Card
        val installment: Installment
        try {
            card = Card.create(
                cardNumber = request.cardNumber,
                expiryMonth = request.expiryMonth,
                expiryYear = request.expiryYear,
                holderName = request.holderName,
                bin = bin
            )

            val contract = merchantContractService.getActiveContract(ongoingPaymentSession.merchantId)

            merchantContractValidator.validate(
                contract = contract,
                paymentMethod = PaymentMethodType.CARD,
                amount = payment.originalAmount,
                minPaymentAmount = paymentPolicy.minPaymentAmount
            )

            merchantLimitService.check(
                merchantId = ongoingPaymentSession.merchantId,
                paymentMethod = PaymentMethodType.CARD,
                amount = payment.originalAmount
            )

            fraudDetectionService.check(
                merchantId = ongoingPaymentSession.merchantId,
                card = card,
                amount = payment.originalAmount
            )

            val issuerPolicy = issuerInstallmentPolicyRepository.findByIssuerCode(card.issuerCode)
            val installmentOptions = installmentCalculator.calculateAvailableOptions(
                merchantPolicy = contract.installmentPolicy,
                issuerPolicy = issuerPolicy,
                cardType = card.cardType,
                finalPaymentAmount = payment.originalAmount
            )

            installment = installmentOptions.createInstallment(request.installmentMonths)
        } catch (e: Exception) {
            payment.fail(
                errorCode = "VALIDATION_FAILED",
                failureReason = e.message ?: "검증 실패"
            )

            paymentRepository.save(payment)
            throw e
        }

        val paymentDetails = CardPaymentDetails(
            card = card,
            installment = installment
        )

        val promotions = promotionRepository.findActivePromotions(payment.paymentMethodType)
        payment.addEffectivePromotions(
            promotions = promotions,
            minPaymentAmount = paymentPolicy.minPaymentAmount,
            optimizer = promotionOptimizer
        )

        cardVaultService.store(
            paymentKey = payment.paymentKey,
            credentials = CardCredentials(
                cardNumber = request.cardNumber,
                expiryMonth = request.expiryMonth,
                expiryYear = request.expiryYear,
                cvc = request.cvc,
                holderName = request.holderName
            )
        )

        val authenticationResult = authenticationService.prepareAuthentication(
            card = card,
            amount = payment.originalAmount,
            merchantId = ongoingPaymentSession.merchantId
        )

        return when (authenticationResult) {
            is AuthenticationResult.Required -> submitWithAuthentication(
                payment,
                paymentDetails,
                authenticationResult.redirect
            )
            is AuthenticationResult.NotRequired -> submitWithoutAuthentication(
                payment,
                paymentDetails,
                ongoingPaymentSession.redirectUrl.success
            )
        }
    }

    private fun submitWithAuthentication(
        payment: Payment,
        paymentDetails: CardPaymentDetails,
        authenticationRedirect: AuthenticationRedirect
    ): SubmitPaymentResponse {
        payment.submit(paymentDetails)
        payment.requestAuthentication()
        paymentRepository.save(payment)
        return SubmitPaymentResponse(
            paymentKey = payment.paymentKey,
            status = SubmitStatus.AUTHENTICATION_REQUIRED,
            authentication = authenticationRedirect,
            redirectUrl = null
        )
    }

    private fun submitWithoutAuthentication(
        payment: Payment,
        paymentDetails: CardPaymentDetails,
        successUrl: String
    ): SubmitPaymentResponse {
        payment.submit(paymentDetails)
        payment.completeWithoutAuthentication()
        paymentRepository.save(payment)
        return SubmitPaymentResponse(
            paymentKey = payment.paymentKey,
            status = SubmitStatus.PENDING_CONFIRM,
            authentication = null,
            redirectUrl = successUrl
        )
    }
}
