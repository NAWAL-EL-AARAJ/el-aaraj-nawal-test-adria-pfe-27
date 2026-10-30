package ma.enset.transactionservice.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.enset.transactionservice.service.AccountGateway;
import ma.enset.transactionservice.service.dto.AccountDTO;
import ma.enset.transactionservice.service.exception.AccountNotFoundException;
import ma.enset.transactionservice.service.exception.AccountOperationRejectedException;
import ma.enset.transactionservice.service.exception.AccountServiceCircuitOpenException;
import ma.enset.transactionservice.service.exception.AccountServiceUnavailableException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accès à account-service via Feign, protégé par Resilience4J.
 * Les erreurs métier (404, 422) sont converties en exceptions métier et ignorées par le circuit breaker ;
 * les erreurs techniques déclenchent le fallback "Service compte indisponible" (503).
 * Le retry ne s'applique qu'à la lecture : rejouer un débit ou un crédit risquerait de le doubler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignAccountGateway implements AccountGateway {

    private static final String ACCOUNT_SERVICE = "accountService";
    private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    private final AccountRestClient accountRestClient;

    @Override
    @Retry(name = ACCOUNT_SERVICE, fallbackMethod = "getAccountFallback")
    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "getAccountFallback")
    public AccountDTO getAccount(UUID accountId) {
        try {
            return accountRestClient.getAccount(accountId);
        } catch (FeignException ex) {
            throw translate(ex, accountId);
        }
    }

    @Override
    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "operationFallback")
    public AccountDTO debit(UUID accountId, BigDecimal amount) {
        try {
            return accountRestClient.debit(accountId, new AccountRestClient.AmountRequest(amount.doubleValue()));
        } catch (FeignException ex) {
            throw translate(ex, accountId);
        }
    }

    @Override
    @CircuitBreaker(name = ACCOUNT_SERVICE, fallbackMethod = "operationFallback")
    public AccountDTO credit(UUID accountId, BigDecimal amount) {
        try {
            return accountRestClient.credit(accountId, new AccountRestClient.AmountRequest(amount.doubleValue()));
        } catch (FeignException ex) {
            throw translate(ex, accountId);
        }
    }

    private AccountDTO getAccountFallback(UUID accountId, Throwable ex) {
        return fallback(ex);
    }

    private AccountDTO operationFallback(UUID accountId, BigDecimal amount, Throwable ex) {
        return fallback(ex);
    }

    private AccountDTO fallback(Throwable ex) {
        if (ex instanceof AccountNotFoundException || ex instanceof AccountOperationRejectedException
                || ex instanceof AccountServiceUnavailableException) {
            throw (RuntimeException) ex;
        }
        if (ex instanceof CallNotPermittedException) {
            throw new AccountServiceCircuitOpenException();
        }
        log.warn("account-service indisponible : {}", ex.toString());
        throw new AccountServiceUnavailableException();
    }

    private RuntimeException translate(FeignException ex, UUID accountId) {
        return switch (ex.status()) {
            case 404 -> new AccountNotFoundException(accountId);
            case 400, 422 -> new AccountOperationRejectedException(extractMessage(ex));
            default -> ex;
        };
    }

    private String extractMessage(FeignException ex) {
        Matcher matcher = MESSAGE.matcher(ex.contentUTF8());
        return matcher.find() ? matcher.group(1) : "Opération refusée par le service compte";
    }
}
