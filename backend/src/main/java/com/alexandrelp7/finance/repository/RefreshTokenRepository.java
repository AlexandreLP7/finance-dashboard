package com.alexandrelp7.finance.repository;

import com.alexandrelp7.finance.domain.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findByUserId(Long userId);
}
