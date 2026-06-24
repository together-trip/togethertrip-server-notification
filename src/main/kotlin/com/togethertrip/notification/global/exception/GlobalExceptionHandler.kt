package com.togethertrip.notification.global.exception

import com.togethertrip.notification.global.response.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ErrorResponse> {
        val errorCode = exception.errorCode
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(code = errorCode.code, message = errorCode.message))
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingRequestHeaderException(exception: MissingRequestHeaderException): ResponseEntity<ErrorResponse> {
        log.warn("Missing required request header. header={}", exception.headerName)
        val errorCode = CommonErrorCode.AUTHENTICATION_REQUIRED
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(code = errorCode.code, message = errorCode.message))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(exception: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("Invalid request: {}", exception.message)
        val errorCode = CommonErrorCode.INVALID_INPUT
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(code = errorCode.code, message = exception.message ?: errorCode.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: CommonErrorCode.INVALID_INPUT.message
        val errorCode = CommonErrorCode.INVALID_INPUT
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(code = errorCode.code, message = message))
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailureException(exception: ObjectOptimisticLockingFailureException): ResponseEntity<ErrorResponse> {
        log.warn("Optimistic locking failure", exception)
        val errorCode = CommonErrorCode.CONCURRENT_MODIFICATION
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(code = errorCode.code, message = errorCode.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", exception)
        val errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(code = errorCode.code, message = errorCode.message))
    }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
