package ma.enset.transactionservice.repository;

import ma.enset.transactionservice.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    default List<Transaction> findByAccountId(UUID accountId) {
        return findByAccountIdOrderByCreatedAtDesc(accountId);
    }
}
