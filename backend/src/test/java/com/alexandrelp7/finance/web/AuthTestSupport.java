package com.alexandrelp7.finance.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public final class AuthTestSupport {

  private AuthTestSupport() {}

  public static String bearer(MockMvc mockMvc) throws Exception {
    return bearerFor(mockMvc, "testuser", "testpassword");
  }

  public static String bearerFor(MockMvc mockMvc, String username, String password)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\""
                            + username
                            + "\",\"password\":\""
                            + password
                            + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
    String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    return "Bearer " + accessToken;
  }
}
