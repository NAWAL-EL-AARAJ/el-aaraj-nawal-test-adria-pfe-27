package ma.enset.transactionservice.service.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID accountId, BigDecimal balance, BigDecimal amount) {
        super("Solde insuffisant sur le compte " + accountId
                + " (solde : " + balance.toPlainString() + ", montant demandé : " + amount.toPlainString() + ")");
    }
}
