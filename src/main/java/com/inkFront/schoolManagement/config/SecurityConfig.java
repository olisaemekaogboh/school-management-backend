package com.inkFront.schoolManagement.config;

import com.inkFront.schoolManagement.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public auth + static/public resources
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/refresh-token",
                                "/api/auth/verify-email",
                                "/api/public/**",
                                "/uploads/**",
                                "/webjars/**",
                                "/error"
                        ).permitAll()

                        // Public read-only endpoints
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/announcements/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sessions/active").permitAll()

                        // Authenticated user self-service
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/logout",
                                "/api/auth/change-password",
                                "/api/users/me"
                        ).authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/me").authenticated()

                        // Events
                        .requestMatchers(HttpMethod.POST, "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/**").hasRole("ADMIN")

                        // Announcements
                        .requestMatchers(HttpMethod.POST, "/api/announcements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/announcements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/announcements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/announcements/**").hasRole("ADMIN")

                        // Sessions
                        .requestMatchers(HttpMethod.GET, "/api/sessions/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers("/api/sessions/**").hasRole("ADMIN")

                        // Teacher self routes
                        .requestMatchers(
                                "/api/teachers/me",
                                "/api/teachers/me/**",
                                "/api/teacher/**"
                        ).hasAnyRole("TEACHER", "ADMIN")

                        // Student self routes
                        .requestMatchers(
                                "/api/students/me",
                                "/api/students/me/**",
                                "/api/student/**"
                        ).hasAnyRole("STUDENT", "ADMIN")

                        // Parent self routes
                        .requestMatchers(
                                "/api/parents/me",
                                "/api/parents/me/**",
                                "/api/parent/**"
                        ).hasAnyRole("PARENT", "ADMIN")

                        // Results
                        .requestMatchers("/api/results/me/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/results/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers("/api/results/**").hasAnyRole("ADMIN", "TEACHER")

                        // Attendance
                        .requestMatchers("/api/attendance/me/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/attendance/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers("/api/attendance/**").hasAnyRole("ADMIN", "TEACHER")

                        // Fees
                        .requestMatchers("/api/fees/me/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/fees/student/**").hasAnyRole("ADMIN", "PARENT", "STUDENT")
                        .requestMatchers("/api/fees/**").hasAnyRole("ADMIN", "PARENT", "STUDENT")

                        // Session results
                        .requestMatchers(HttpMethod.GET, "/api/session-results/me/**")
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

                        // Subjects
                        .requestMatchers(HttpMethod.GET, "/api/subjects/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers("/api/subjects/**").hasRole("ADMIN")

                        // Classes
                        .requestMatchers(HttpMethod.GET, "/api/classes/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/classes/**").hasRole("ADMIN")

                        // Students
                        .requestMatchers(HttpMethod.GET, "/api/students/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers("/api/students/**").hasRole("ADMIN")

                        // Teachers
                        .requestMatchers(HttpMethod.GET, "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/teachers/**").hasRole("ADMIN")

                        // Support
                        .requestMatchers(HttpMethod.POST, "/api/support/tickets")
                        .hasAnyRole("PARENT", "TEACHER", "STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/support/tickets/my")
                        .hasAnyRole("PARENT", "TEACHER", "STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/support/tickets").hasRole("ADMIN")
                        .requestMatchers("/api/support/**")
                        .hasAnyRole("ADMIN", "PARENT", "TEACHER", "STUDENT")

                        // Timetable
                        .requestMatchers(HttpMethod.GET, "/api/timetable/me").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/timetable/student/me").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/timetable/parent/ward/**").hasAnyRole("PARENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/timetable/class/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/timetable/school").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/timetable/check-availability").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/timetable/{id}").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/timetable/**").hasRole("ADMIN")

                        // Transport
                        .requestMatchers(HttpMethod.GET, "/api/transport/routes/*/students").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/transport/statistics").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/transport/update-location/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/transport/assign").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/transport/remove/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/transport/routes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/transport/routes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/transport/routes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/transport/routes/**")
                        .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/transport/location/**")
                        .hasAnyRole("ADMIN", "STUDENT", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/transport/student/**")
                        .hasAnyRole("ADMIN", "STUDENT", "PARENT")

                        // Email queue
                        .requestMatchers("/api/email-queue/**").hasRole("ADMIN")

                        // Admin-only modules
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

        List<String> allowedOrigins = Stream.of(frontendUrl, "http://localhost:3000", "https://localhost:3000")
                .filter(origin -> origin != null && !origin.isBlank())
                .distinct()
                .collect(Collectors.toList());

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Cache-Control",
                "Origin"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "Set-Cookie"
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
        authProvider.setHideUserNotFoundExceptions(false);
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