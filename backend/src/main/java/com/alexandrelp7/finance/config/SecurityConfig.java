package com.alexandrelp7.finance.config;

import com.alexandrelp7.finance.repository.AppUserRepository;
import com.alexandrelp7.finance.security.JwtAuthFilter;
import com.alexandrelp7.finance.security.JwtService;
import com.alexandrelp7.finance.security.RateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final RateLimitFilter rateLimitFilter;

  public SecurityConfig(JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.rateLimitFilter = rateLimitFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            handling ->
                handling.authenticationEntryPoint(
                    (request, response, exception) -> {
                      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      response
                          .getWriter()
                          .write(
                              "{\"error\":\"Unauthorized\",\"message\":\"Authentification requise\"}");
                    }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/2fa/verify")
                    .permitAll()
                    .anyRequest()
                    .hasAuthority(JwtService.ROLE_USER))
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService(AppUserRepository userRepository) {
    return username ->
        userRepository
            .findByUsername(username)
            .map(
                user ->
                    User.withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles("USER")
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException(username));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
