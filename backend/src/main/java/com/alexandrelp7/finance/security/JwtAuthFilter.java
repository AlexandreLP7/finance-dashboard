package com.alexandrelp7.finance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      JwtService.TokenInfo info = jwtService.parse(header.substring(BEARER_PREFIX.length()));
      if (info != null) {
        List<SimpleGrantedAuthority> authorities =
            info.roles().stream().map(SimpleGrantedAuthority::new).toList();
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(info.subject(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    chain.doFilter(request, response);
  }
}
