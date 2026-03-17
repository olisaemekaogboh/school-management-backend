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

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

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

                        .requestMatchers("/api/results/me/**")
                        .hasAnyRole("STUDENT", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/results/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/results/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers("/api/attendance/me/**")
                        .hasAnyRole("STUDENT", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/attendance/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/attendance/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers("/api/fees/me/**")
                        .hasAnyRole("STUDENT", "ADMIN")

                        .requestMatchers("/api/fees/student/**")
                        .hasAnyRole("ADMIN", "PARENT", "STUDENT")

                        .requestMatchers("/api/fees/**")
                        .hasAnyRole("ADMIN", "PARENT", "STUDENT")

                        .requestMatchers(HttpMethod.GET, "/api/announcements/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/announcements/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/sessions/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/sessions/**")
                        .hasRole("ADMIN")

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

                        .requestMatchers(HttpMethod.GET, "/api/subjects/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/subjects/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/classes/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers("/api/classes/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/students/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers("/api/students/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/teachers/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers("/api/teachers/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/timetable/me")
                        .hasAnyRole("TEACHER", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/timetable/student/me")
                        .hasAnyRole("STUDENT", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/timetable/parent/ward/**")
                        .hasAnyRole("PARENT", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/timetable/class/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers(HttpMethod.GET, "/api/timetable/school")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers(HttpMethod.GET, "/api/timetable/check-availability")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/timetable/{id}")
                        .hasAnyRole("ADMIN", "TEACHER")

                        .requestMatchers("/api/timetable/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/transport/routes/*/students")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/transport/statistics")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/transport/update-location/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/transport/assign")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/transport/remove/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/transport/routes")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/transport/routes/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/transport/routes/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/transport/routes/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        .requestMatchers(HttpMethod.GET, "/api/transport/location/**")
                        .hasAnyRole("ADMIN", "STUDENT", "PARENT")

                        .requestMatchers(HttpMethod.GET, "/api/transport/student/**")
                        .hasAnyRole("ADMIN", "STUDENT", "PARENT")

                        .requestMatchers(
                                "/api/users/**",
                                "/api/admin/**",
                                "/api/library/**"
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