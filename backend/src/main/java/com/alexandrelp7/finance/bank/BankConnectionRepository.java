package com.alexandrelp7.finance.bank;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankConnectionRepository extends JpaRepository<BankConnection, Long> {

  Optional<BankConnection> findByProvider(String provider);

  boolean existsByProvider(String provider);
}
