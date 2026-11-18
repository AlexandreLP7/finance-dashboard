package com.alexandrelp7.finance.web;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.alexandrelp7.finance.domain.AppUser;
import com.alexandrelp7.finance.repository.AppUserRepository;
import com.alexandrelp7.finance.repository.RefreshTokenRepository;
import com.alexandrelp7.finance.security.TotpService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AppUserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TotpService totpService;

  @BeforeEach
  void cleanUsers() {
    refreshTokenRepository.deleteAll();
    userRepository.findByUsername("alice").ifPresent(userRepository::delete);
  }

  @Test
  void meReturnsUsernameAndTotpFlag() throws Exception {
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", AuthTestSupport.bearer(mockMvc)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", org.hamcrest.Matchers.is("testuser")))
        .andExpect(jsonPath("$.totpEnabled", org.hamcrest.Matchers.is(false)));
  }

  @Test
  void loginOkReturnsTokens() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"testpassword\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.refreshToken", notNullValue()))
        .andExpect(jsonPath("$.totpRequired", org.hamcrest.Matchers.is(false)))
        .andExpect(jsonPath("$.username", org.hamcrest.Matchers.is("testuser")));
  }

  @Test
  void loginBadPasswordReturns401() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", org.hamcrest.Matchers.is("Unauthorized")));
  }

  @Test
  void protectedEndpointRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
  }

  @Test
  void accessTokenGrantsAccessToApi() throws Exception {
    mockMvc
        .perform(get("/api/accounts").header("Authorization", AuthTestSupport.bearer(mockMvc)))
        .andExpect(status().isOk());
  }

  @Test
  void pre2faTokenCannotAccessProtectedApi() throws Exception {
    createAliceWithTotp("alicepass");
    MvcResult login = login("alice", "alicepass");
    String pre2fa = JsonPath.read(login.getResponse().getContentAsString(), "$.pre2faToken");
    mockMvc
        .perform(get("/api/accounts").header("Authorization", "Bearer " + pre2fa))
        .andExpect(status().isForbidden());
  }

  @Test
  void verify2faWithValidCodeIssuesTokens() throws Exception {
    String secret = createAliceWithTotp("alicepass");
    String pre2fa =
        JsonPath.read(login("alice", "alicepass").getResponse().getContentAsString(), "$.pre2faToken");
    String code = totpService.totp(secret, Instant.now().toEpochMilli() / 1000 / 30, 6);
    mockMvc
        .perform(
            post("/api/auth/2fa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + pre2fa + "\",\"code\":\"" + code + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.totpRequired", org.hamcrest.Matchers.is(false)));
  }

  @Test
  void verify2faWithWrongCodeReturns401() throws Exception {
    createAliceWithTotp("alicepass");
    String pre2fa =
        JsonPath.read(login("alice", "alicepass").getResponse().getContentAsString(), "$.pre2faToken");
    mockMvc
        .perform(
            post("/api/auth/2fa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + pre2fa + "\",\"code\":\"000000\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshRotatesTokenAndRevokesOldOne() throws Exception {
    MvcResult login = login("testuser", "testpassword");
    String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");
    MvcResult refreshed = refresh(refreshToken);
    String newRefresh =
        JsonPath.read(refreshed.getResponse().getContentAsString(), "$.refreshToken");
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()));
  }

  @Test
  void logoutRevokesRefreshTokens() throws Exception {
    MvcResult login = login("testuser", "testpassword");
    String bearer = "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");
    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", bearer))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void setupThenEnableThenDisable2fa() throws Exception {
    createAlice("alicepass");
    String bearer = AuthTestSupport.bearerFor(mockMvc, "alice", "alicepass");
    MvcResult setup =
        mockMvc
            .perform(post("/api/auth/2fa/setup").header("Authorization", bearer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.secret", notNullValue()))
            .andExpect(jsonPath("$.otpauthUri", org.hamcrest.Matchers.startsWith("otpauth://totp/")))
            .andReturn();
    String secret = JsonPath.read(setup.getResponse().getContentAsString(), "$.secret");
    String code = totpService.totp(secret, Instant.now().toEpochMilli() / 1000 / 30, 6);
    mockMvc
        .perform(
            post("/api/auth/2fa/enable")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"alicepass\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totpRequired", org.hamcrest.Matchers.is(true)))
        .andExpect(jsonPath("$.accessToken", nullValue()));

    String disableCode = totpService.totp(secret, Instant.now().toEpochMilli() / 1000 / 30, 6);
    mockMvc
        .perform(
            post("/api/auth/2fa/disable")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + disableCode + "\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"alicepass\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totpRequired", org.hamcrest.Matchers.is(false)));
  }

  @Test
  void enable2faWithWrongCodeReturns401() throws Exception {
    createAlice("alicepass");
    String bearer = AuthTestSupport.bearerFor(mockMvc, "alice", "alicepass");
    mockMvc
        .perform(post("/api/auth/2fa/setup").header("Authorization", bearer))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/2fa/enable")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void changePasswordThenOldPasswordFails() throws Exception {
    createAlice("alicepass");
    String bearer = AuthTestSupport.bearerFor(mockMvc, "alice", "alicepass");
    mockMvc
        .perform(
            post("/api/auth/change-password")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"alicepass\",\"newPassword\":\"newpassword123\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"alicepass\"}"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"newpassword123\"}"))
        .andExpect(status().isOk());
  }

  private MvcResult login(String username, String password) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn();
  }

  private MvcResult refresh(String refreshToken) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
        .andExpect(status().isOk())
        .andReturn();
  }

  private void createAlice(String password) {
    userRepository.save(new AppUser("alice", passwordEncoder.encode(password)));
  }

  private String createAliceWithTotp(String password) {
    String secret = totpService.generateSecret();
    AppUser alice = new AppUser("alice", passwordEncoder.encode(password));
    alice.setTotpSecret(secret);
    alice.setTotpEnabled(true);
    userRepository.save(alice);
    return secret;
  }
}
