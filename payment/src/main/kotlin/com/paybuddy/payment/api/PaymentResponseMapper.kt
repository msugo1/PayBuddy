package com.paybuddy.payment.api

import com.paybuddy.payment.api.model.PaymentSubmitResponse
import com.paybuddy.payment.api.model.PaymentSubmitResponseAuthentication
import com.paybuddy.payment.application.dto.AuthenticationMethod
import com.paybuddy.payment.application.dto.AuthenticationType
import com.paybuddy.payment.application.dto.SubmitPaymentResponse
import com.paybuddy.payment.application.dto.SubmitStatus
import java.net.URI

fun SubmitPaymentResponse.toApiResponse(): PaymentSubmitResponse {
    val apiResponse = PaymentSubmitResponse()
        .paymentKey(this.paymentKey)
        .status(this.status.toApiEnum())

    when (this.status) {
        SubmitStatus.AUTHENTICATION_REQUIRED -> {
            val auth = this.authentication!!
            apiResponse.authentication(
                PaymentSubmitResponseAuthentication()
                    .type(auth.type.toApiEnum())
                    .method(auth.method.toApiEnum())
                    .url(URI.create(auth.url))
                    .data(auth.data)
            )
        }
        SubmitStatus.PENDING_CONFIRM -> {
            apiResponse.redirectUrl(this.redirectUrl?.let { URI.create(it) })
        }
    }

    return apiResponse
}

private fun SubmitStatus.toApiEnum(): PaymentSubmitResponse.StatusEnum {
    return when (this) {
        SubmitStatus.AUTHENTICATION_REQUIRED -> PaymentSubmitResponse.StatusEnum.AUTHENTICATION_REQUIRED
        SubmitStatus.PENDING_CONFIRM -> PaymentSubmitResponse.StatusEnum.PENDING_CONFIRM
    }
}

private fun AuthenticationType.toApiEnum(): PaymentSubmitResponseAuthentication.TypeEnum {
    return when (this) {
        AuthenticationType.ISP -> PaymentSubmitResponseAuthentication.TypeEnum.ISP
        AuthenticationType.THREE_DS -> PaymentSubmitResponseAuthentication.TypeEnum.THREE_DS
        AuthenticationType.THREE_D_SECURE -> PaymentSubmitResponseAuthentication.TypeEnum.THREE_DS
        AuthenticationType.ACS_REDIRECT -> PaymentSubmitResponseAuthentication.TypeEnum.THREE_DS
    }
}

private fun AuthenticationMethod.toApiEnum(): PaymentSubmitResponseAuthentication.MethodEnum {
    return when (this) {
        AuthenticationMethod.GET -> PaymentSubmitResponseAuthentication.MethodEnum.GET
        AuthenticationMethod.POST -> PaymentSubmitResponseAuthentication.MethodEnum.POST
        AuthenticationMethod.REDIRECT -> PaymentSubmitResponseAuthentication.MethodEnum.REDIRECT
        AuthenticationMethod.IFRAME -> PaymentSubmitResponseAuthentication.MethodEnum.POST
    }
}
