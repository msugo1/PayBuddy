package com.paybuddy.payment.config

import com.paybuddy.payment.domain.CardBrand
import com.paybuddy.payment.domain.CardType
import com.paybuddy.payment.domain.OwnerType
import com.paybuddy.payment.domain.service.Bin
import com.paybuddy.payment.domain.service.BinLookupService

/**
 * Contract Test용 Stub 구현체
 * 고정된 BIN 정보를 반환합니다.
 */
class StubBinLookupService : BinLookupService {
    override fun lookup(cardNumber: String): Bin {
        return Bin(
            number = cardNumber.take(6),
            brand = CardBrand.VISA,
            issuerCode = "SHINHAN",
            acquirerCode = "KB",
            cardType = CardType.CREDIT,
            ownerType = OwnerType.PERSONAL,
            issuedCountry = "KR",
            productCode = "GENERAL"
        )
    }
}
