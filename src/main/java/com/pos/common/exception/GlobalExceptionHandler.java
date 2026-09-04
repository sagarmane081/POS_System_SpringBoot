package com.pos.common.exception;

import com.pos.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>>
    handleResourceNotFound(
            ResourceNotFoundException ex
    ) {

        return ResponseEntity.status(
                HttpStatus.NOT_FOUND
        ).body(

                new ApiResponse<>(

                        false,
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<?>>
    handleDuplicateResource(
            DuplicateResourceException ex
    ) {

        return ResponseEntity.status(
                HttpStatus.CONFLICT
        ).body(

                new ApiResponse<>(

                        false,
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<?>>
    handleInsufficientStock(
            InsufficientStockException ex
    ) {

        return ResponseEntity.status(
                HttpStatus.CONFLICT
        ).body(

                new ApiResponse<>(

                        false,
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>>
    handleDataIntegrityViolation(
            DataIntegrityViolationException ex
    ) {

        log.warn("Data integrity violation", ex);

        return ResponseEntity.status(
                HttpStatus.CONFLICT
        ).body(

                new ApiResponse<>(

                        false,
                        "The request could not be completed because it violates a data constraint " +
                                "(e.g. a duplicate value, or a record still referenced by another resource)",
                        null
                )
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<?>>
    handleInvalidRefreshToken(
            InvalidRefreshTokenException ex
    ) {

        return ResponseEntity.status(
                HttpStatus.UNAUTHORIZED
        ).body(

                new ApiResponse<>(

                        false,
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>>
    handleAuthenticationException(
            AuthenticationException ex
    ) {

        return ResponseEntity.status(
                HttpStatus.UNAUTHORIZED
        ).body(

                new ApiResponse<>(

                        false,
                        "Invalid email or password",
                        null
                )
        );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiResponse<?>>
    handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        String errorMessage =
                ex.getBindingResult()
                        .getFieldError()
                        .getDefaultMessage();

        return ResponseEntity.badRequest()
                .body(

                        new ApiResponse<>(

                                false,
                                errorMessage,
                                null
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>>
    handleGenericException(
            Exception ex
    ) {

        log.error("Unhandled exception", ex);

        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR
        ).body(

                new ApiResponse<>(

                        false,
                        "An unexpected error occurred",
                        null
                )
        );
    }
}