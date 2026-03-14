package com.inkFront.schoolManagement.config;

import com.inkFront.schoolManagement.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

                        // allow preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // PUBLIC ENDPOINTS
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

                        // AUTHENTICATED USER ENDPOINTS
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/logout",
                                "/api/auth/change-password"
                        ).authenticated()

                        // TEACHER SELF-SERVICE
                        .requestMatchers(
                                "/api/teachers/me",
                                "/api/teachers/me/**",
                                "/api/teacher/**"
                        ).hasAnyRole("TEACHER", "ADMIN")

                        // STUDENT SELF-SERVICE
                        .requestMatchers(
                                "/api/students/me",
                                "/api/students/me/**",
                                "/api/student/**"
                        ).hasAnyRole("STUDENT", "ADMIN")

                        // PARENT SELF-SERVICE
                        .requestMatchers(
                                "/api/parents/me",
                                "/api/parents/me/**",
                                "/api/parent/**"
                        ).hasAnyRole("PARENT", "ADMIN")

                        // RESULTS
                        .requestMatchers("/api/results/me/**")
                        .hasAnyRole("STUDENT", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/results/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/results/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        // ATTENDANCE
                        .requestMatchers("/api/attendance/me/**")
                        .hasAnyRole("STUDENT", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/attendance/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/attendance/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        // FEES
                        .requestMatchers("/api/fees/me/**")
                        .hasAnyRole("STUDENT", "ADMIN")

                        .requestMatchers("/api/fees/student/**")
                        .hasAnyRole("ADMIN", "PARENT", "STUDENT")

                        .requestMatchers("/api/fees/**")
                        .hasAnyRole("ADMIN", "PARENT", "STUDENT")

                        // ANNOUNCEMENTS
                        .requestMatchers(HttpMethod.GET, "/api/announcements/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/announcements/**")
                        .hasRole("ADMIN")

                        // SESSIONS
                        .requestMatchers(HttpMethod.GET, "/api/sessions/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        // SESSION RESULTS
                        .requestMatchers("/api/session-results/me/**")
                        .hasAnyRole("STUDENT", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/session-results/student/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers(HttpMethod.GET, "/api/session-results/report/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers(HttpMethod.GET, "/api/session-results/class/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers(HttpMethod.GET, "/api/session-results/rankings/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers("/api/session-results/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/sessions/**")
                        .hasRole("ADMIN")

                        // SUBJECTS
                        .requestMatchers(HttpMethod.GET, "/api/subjects/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/subjects/**")
                        .hasRole("ADMIN")

                        // CLASSES
                        .requestMatchers(HttpMethod.GET, "/api/classes/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers("/api/classes/**")
                        .hasRole("ADMIN")

                        // STUDENTS
                        .requestMatchers(HttpMethod.GET, "/api/students/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/students/**")
                        .hasRole("ADMIN")

                        // TEACHERS
                        .requestMatchers(HttpMethod.GET, "/api/teachers/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers("/api/teachers/**")
                        .hasRole("ADMIN")

                        // OTHER ADMIN MODULES
                        .requestMatchers(
                                "/api/users/**",
                                "/api/admin/**",
                                "/api/transport/**",
                                "/api/library/**",
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
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
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