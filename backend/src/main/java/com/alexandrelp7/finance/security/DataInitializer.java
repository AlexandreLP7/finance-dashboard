package com.alexandrelp7.finance.security;

import com.alexandrelp7.finance.domain.AppUser;
import com.alexandrelp7.finance.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

  private final AppUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String username;
  private final String password;

  public DataInitializer(
      AppUserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.security.user}") String username,
      @Value("${app.security.password}") String password) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.username = username;
    this.password = password;
  }

  @Override
  public void run(String... args) {
    if (userRepository.findByUsername(username).isEmpty()) {
      userRepository.save(new AppUser(username, passwordEncoder.encode(password)));
      log.info("Utilisateur initial '{}' cree (changez APP_PASSWORD au premier lancement)", username);
    }
  }
}
