package ma.enset.transactionservice.service;

import ma.enset.transactionservice.service.dto.AccountDTO;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Accès aux comptes gérés par account-service.
 * Les implémentations lèvent AccountNotFoundException si le compte n'existe pas
 * et AccountServiceUnavailableException si account-service est injoignable.
 */
public interface AccountGateway {

    AccountDTO getAccount(UUID accountId);

    AccountDTO debit(UUID accountId, BigDecimal amount);

    AccountDTO credit(UUID accountId, BigDecimal amount);
}
