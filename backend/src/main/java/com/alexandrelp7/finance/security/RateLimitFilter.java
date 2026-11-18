package com.alexandrelp7.finance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final String AUTH_PATH_PREFIX = "/api/auth/";
  private static final long WINDOW_MILLIS = 60_000L;

  private final boolean enabled;
  private final int maxRequestsPerMinute;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimitFilter(
      @Value("${app.rate-limit.enabled:true}") boolean enabled,
      @Value("${app.rate-limit.auth-requests-per-minute:20}") int maxRequestsPerMinute) {
    this.enabled = enabled;
    this.maxRequestsPerMinute = maxRequestsPerMinute;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (enabled && request.getRequestURI().startsWith(AUTH_PATH_PREFIX)) {
      String key = request.getRemoteAddr();
      long now = Instant.now().toEpochMilli();
      Window window =
          windows.compute(
              key,
              (k, current) -> {
                if (current == null || now - current.windowStart >= WINDOW_MILLIS) {
                  return new Window(now, 1);
                }
                current.count++;
                return current;
              });
      if (windows.size() > 10_000) {
        windows.entrySet().removeIf(e -> now - e.getValue().windowStart >= WINDOW_MILLIS);
      }
      if (window.count > maxRequestsPerMinute) {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response
            .getWriter()
            .write("{\"error\":\"Too Many Requests\",\"message\":\"Trop de tentatives, reessayez plus tard\"}");
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private static final class Window {
    private final long windowStart;
    private int count;

    private Window(long windowStart, int count) {
      this.windowStart = windowStart;
      this.count = count;
    }
  }
}
