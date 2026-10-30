package ma.enset.transactionservice.service.exception;

import java.util.UUID;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(UUID accountId, String accountCurrency, String transactionCurrency) {
        super("Devise " + transactionCurrency + " différente de celle du compte " + accountId
                + " (" + accountCurrency + ")");
    }
}
