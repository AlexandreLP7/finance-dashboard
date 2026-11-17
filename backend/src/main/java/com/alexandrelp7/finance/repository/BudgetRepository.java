package com.alexandrelp7.finance.repository;

import com.alexandrelp7.finance.domain.Budget;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

  List<Budget> findByAccountId(Long accountId);

  Optional<Budget> findByAccountIdAndCategory(Long accountId, String category);
}
