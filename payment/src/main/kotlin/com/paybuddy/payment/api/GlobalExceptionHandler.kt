package com.paybuddy.payment.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.BindException
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
        req: HttpServletRequest
    ): ResponseEntity<ProblemDetail> {
        val problem = e.body

        problem.setProperty("error_code", "INVALID_REQUEST_FIELD")
        problem.setProperty(
            "errors",
            e.bindingResult.fieldErrors
                .groupBy(
                    { it.field },
                    { it.defaultMessage }
                )
                .mapValues { (it, msg) -> msg.distinct() }
        )

        return ResponseEntity
            .badRequest()
            .contentType(MediaType.valueOf("application/problem+json"))
            .body(problem)
    }
}