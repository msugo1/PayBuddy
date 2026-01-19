package com.paybuddy.payment.infrastructure.stub

import com.paybuddy.payment.application.dto.AuthenticationMethod
import com.paybuddy.payment.application.dto.AuthenticationRedirect
import com.paybuddy.payment.application.dto.AuthenticationType
import com.paybuddy.payment.domain.Card
import com.paybuddy.payment.domain.authentication.AuthenticationPolicyProvider
import com.paybuddy.payment.domain.authentication.AuthenticationResult
import com.paybuddy.payment.domain.authentication.AuthenticationService
import org.springframework.stereotype.Component

@Component
class StubAuthenticationService(
    private val policyProvider: AuthenticationPolicyProvider
) : AuthenticationService {

    override fun prepareAuthentication(
        card: Card,
        amount: Long,
        merchantId: String
    ): AuthenticationResult {
        if (!requiresAuthentication(card, amount)) {
            return AuthenticationResult.NotRequired
        }

        val redirect = if (isDomesticCard(card)) {
            createIspAuthentication(card, amount, merchantId)
        } else {
            create3dsAuthentication(card, amount, merchantId)
        }

        return AuthenticationResult.Required(redirect)
    }

    private fun requiresAuthentication(card: Card, amount: Long): Boolean {
        if (card.issuedCountry !in policyProvider.exemptionCountries) {
            return true
        }

        if (amount >= policyProvider.highAmountThreshold) {
            return true
        }

        return false
    }

    private fun isDomesticCard(card: Card): Boolean {
        return card.issuedCountry in policyProvider.exemptionCountries
    }

    private fun createIspAuthentication(
        card: Card,
        amount: Long,
        merchantId: String
    ): AuthenticationRedirect {
        return AuthenticationRedirect(
            type = AuthenticationType.ISP,
            method = AuthenticationMethod.REDIRECT,
            url = "https://isp.example.com/auth", // TODO: 실제 ISP 인증 URL 생성
            data = mapOf(
                "merchantId" to merchantId,
                "amount" to amount.toString()
                // TODO: 실제 ISP 파라미터 추가
            )
        )
    }

    private fun create3dsAuthentication(
        card: Card,
        amount: Long,
        merchantId: String
    ): AuthenticationRedirect {
        return AuthenticationRedirect(
            type = AuthenticationType.THREE_DS,
            method = AuthenticationMethod.REDIRECT,
            url = "https://3ds.example.com/auth", // TODO: 실제 3DS 인증 URL 생성
            data = mapOf(
                "merchantId" to merchantId,
                "amount" to amount.toString()
                // TODO: 실제 3DS 파라미터 추가
            )
        )
    }
}
