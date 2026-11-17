package com.alexandrelp7.finance.repository;

import com.alexandrelp7.finance.domain.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByIbanMasked(String ibanMasked);
}
