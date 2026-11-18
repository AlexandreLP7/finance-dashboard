package com.alexandrelp7.finance.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  @Test
  void exceedsLimitReturns429() throws ServletException, IOException {
    RateLimitFilter filter = new RateLimitFilter(true, 3);
    MockHttpServletResponse response = new MockHttpServletResponse();
    for (int i = 0; i < 3; i++) {
      MockFilterChain chain = new MockFilterChain();
      filter.doFilter(authRequest(), new MockHttpServletResponse(), chain);
      assertThat(chain.getRequest()).as("requete %d acceptee", i).isNotNull();
    }
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(authRequest(), response, chain);
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(chain.getRequest()).isNull();
    assertThat(response.getContentAsString()).contains("Too Many Requests");
  }

  @Test
  void nonAuthPathsAreNotLimited() throws ServletException, IOException {
    RateLimitFilter filter = new RateLimitFilter(true, 1);
    for (int i = 0; i < 5; i++) {
      MockFilterChain chain = new MockFilterChain();
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts");
      request.setRemoteAddr("10.0.0.1");
      filter.doFilter(request, new MockHttpServletResponse(), chain);
      assertThat(chain.getRequest()).isNotNull();
    }
  }

  @Test
  void disabledFilterLetsEverythingThrough() throws ServletException, IOException {
    RateLimitFilter filter = new RateLimitFilter(false, 1);
    for (int i = 0; i < 5; i++) {
      MockFilterChain chain = new MockFilterChain();
      filter.doFilter(authRequest(), new MockHttpServletResponse(), chain);
      assertThat(chain.getRequest()).isNotNull();
    }
  }

  private static MockHttpServletRequest authRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr("192.168.1.10");
    return request;
  }
}
