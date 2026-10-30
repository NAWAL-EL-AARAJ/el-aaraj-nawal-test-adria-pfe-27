package ma.enset.transactionservice.client;

import ma.enset.transactionservice.service.AccountGateway;
import ma.enset.transactionservice.service.dto.AccountDTO;
import ma.enset.transactionservice.service.exception.AccountServiceUnavailableException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implémentation provisoire : sera remplacée par le client Feign vers account-service.
 */
@Component
public class PendingAccountGateway implements AccountGateway {

    @Override
    public AccountDTO getAccount(UUID accountId) {
        throw new AccountServiceUnavailableException();
    }

    @Override
    public AccountDTO debit(UUID accountId, BigDecimal amount) {
        throw new AccountServiceUnavailableException();
    }

    @Override
    public AccountDTO credit(UUID accountId, BigDecimal amount) {
        throw new AccountServiceUnavailableException();
    }
}
