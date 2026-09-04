package com.pos.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products/**")
                        .hasAnyRole("ADMIN", "CASHIER")

                        .requestMatchers("/api/products/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/seed/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/orders/**")
                        .hasAnyRole("ADMIN", "CASHIER")

                        .requestMatchers("/api/payments/**")
                        .hasAnyRole("ADMIN", "CASHIER")

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/categories/**")
                        .hasAnyRole("ADMIN", "CASHIER")

                        .requestMatchers("/api/categories/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/analytics/**")
                        .hasRole("ADMIN")

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}