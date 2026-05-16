package com.pos.common.exception;

import com.pos.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR
        ).body(

                new ApiResponse<>(

                        false,
                        ex.getMessage(),
                        null
                )
        );
    }
}