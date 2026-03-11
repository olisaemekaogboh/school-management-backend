package com.inkFront.schoolManagement.config;

import com.inkFront.schoolManagement.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/refresh-token",
                                "/api/auth/verify-email",
                                "/api/public/**",
                                "/uploads/**",
                                "/webjars/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/logout",
                                "/api/auth/change-password"
                        ).authenticated()

                        .requestMatchers(
                                "/api/teachers/me",
                                "/api/teachers/me/**",
                                "/api/teacher/**"
                        ).hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers(
                                "/api/students/me",
                                "/api/students/me/**",
                                "/api/student/**"
                        ).hasAnyRole("STUDENT", "ADMIN")

                        .requestMatchers(
                                "/api/parents/me",
                                "/api/parents/me/**",
                                "/api/parent/**"
                        ).hasAnyRole("PARENT", "ADMIN")

                        // Result endpoints
                        .requestMatchers("/api/results/me/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/results/**").authenticated()

                        // Attendance endpoints
                        .requestMatchers("/api/attendance/me/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/attendance/**").authenticated()

                        // Fee endpoints
                        .requestMatchers("/api/fees/me/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/fees/student/**").hasAnyRole("ADMIN", "STUDENT", "PARENT")
                        .requestMatchers("/api/fees/**").authenticated()

                        .requestMatchers(
                                "/api/announcements/**",
                                "/api/sessions/active"
                        ).authenticated()

                        .requestMatchers(
                                "/api/users/**",
                                "/api/admin/**",
                                "/api/students/**",
                                "/api/teachers/**",
                                "/api/parents/**",
                                "/api/classes/**",
                                "/api/transport/**",
                                "/api/library/**",
                                "/api/sessions/**",
                                "/api/timetable/**"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Cache-Control",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}