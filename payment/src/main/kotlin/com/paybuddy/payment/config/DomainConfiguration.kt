package com.paybuddy.payment.config

import com.paybuddy.payment.domain.KnapsackPromotionOptimizer
import com.paybuddy.payment.domain.PromotionOptimizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainConfiguration {

    @Bean
    fun promotionOptimizer(): PromotionOptimizer {
        return KnapsackPromotionOptimizer
    }
}
