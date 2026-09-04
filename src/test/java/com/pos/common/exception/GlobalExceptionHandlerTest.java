package com.pos.common.exception;

import com.pos.common.response.ApiResponse;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_shouldReturn404WithMessage() {

        ResponseEntity<ApiResponse<?>> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Product not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Product not found");
    }

    @Test
    void handleDuplicateResource_shouldReturn409WithMessage() {

        ResponseEntity<ApiResponse<?>> response =
                handler.handleDuplicateResource(new DuplicateResourceException("Email already exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Email already exists");
    }

    @Test
    void handleInsufficientStock_shouldReturn409WithMessage() {

        ResponseEntity<ApiResponse<?>> response =
                handler.handleInsufficientStock(new InsufficientStockException("Insufficient stock for Coke"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Insufficient stock for Coke");
    }

    @Test
    void handleAuthenticationException_shouldReturn401WithGenericMessage() {

        ResponseEntity<ApiResponse<?>> response =
                handler.handleAuthenticationException(new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
    }

    @Test
    void handleValidationException_shouldReturn400WithFieldErrorMessage() {

        MethodParameter parameter = mock(MethodParameter.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("registerRequest", "email", "Invalid email");

        when(bindingResult.getFieldError()).thenReturn(fieldError);

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<?>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email");
    }

    @Test
    void handleGenericException_shouldReturn500WithGenericMessage() {

        ResponseEntity<ApiResponse<?>> response =
                handler.handleGenericException(new RuntimeException("Something exploded"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
