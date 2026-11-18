package com.alexandrelp7.finance.web;

import com.alexandrelp7.finance.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/me")
  public MeResponse me(Authentication authentication) {
    return new MeResponse(
        authentication.getName(), authService.isTotpEnabled(authentication.getName()));
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    AuthService.LoginResult result =
        authService.login(request.username(), request.password());
    return toResponse(result);
  }

  @PostMapping("/2fa/verify")
  public LoginResponse verify2fa(@Valid @RequestBody Verify2faRequest request) {
    return toResponse(authService.verify2fa(request.token(), request.code()));
  }

  @PostMapping("/refresh")
  public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
    AuthService.TokenPair pair = authService.refresh(request.refreshToken());
    return new TokenPairResponse(pair.accessToken(), pair.refreshToken());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(Authentication authentication) {
    authService.logout(authentication.getName());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/2fa/setup")
  public Setup2faResponse setup2fa(Authentication authentication) {
    AuthService.TotpSetup setup = authService.setup2fa(authentication.getName());
    return new Setup2faResponse(setup.secret(), setup.otpauthUri());
  }

  @PostMapping("/2fa/enable")
  public ResponseEntity<Void> enable2fa(
      @Valid @RequestBody CodeRequest request, Authentication authentication) {
    authService.enable2fa(authentication.getName(), request.code());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/2fa/disable")
  public ResponseEntity<Void> disable2fa(
      @Valid @RequestBody CodeRequest request, Authentication authentication) {
    authService.disable2fa(authentication.getName(), request.code());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/change-password")
  public ResponseEntity<Void> changePassword(
      @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
    authService.changePassword(
        authentication.getName(), request.oldPassword(), request.newPassword());
    return ResponseEntity.ok().build();
  }

  private static LoginResponse toResponse(AuthService.LoginResult result) {
    return new LoginResponse(
        result.tokens() == null ? null : result.tokens().accessToken(),
        result.tokens() == null ? null : result.tokens().refreshToken(),
        result.totpRequired(),
        result.pre2faToken(),
        result.username());
  }

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  public record Verify2faRequest(@NotBlank String token, @NotBlank String code) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record CodeRequest(@NotBlank String code) {}

  public record ChangePasswordRequest(
      @NotBlank String oldPassword, @NotBlank @Size(min = 8) String newPassword) {}

  public record LoginResponse(
      String accessToken, String refreshToken, boolean totpRequired, String pre2faToken,
      String username) {}

  public record TokenPairResponse(String accessToken, String refreshToken) {}

  public record Setup2faResponse(String secret, String otpauthUri) {}

  public record MeResponse(String username, boolean totpEnabled) {}
}
