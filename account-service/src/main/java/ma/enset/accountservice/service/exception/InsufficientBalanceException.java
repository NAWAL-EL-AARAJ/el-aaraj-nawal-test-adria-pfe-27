package ma.enset.accountservice.service.exception;

import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID id, double balance, double amount) {
        super(String.format("Solde insuffisant sur le compte %s (solde : %.2f, montant demandé : %.2f)",
                id, balance, amount));
    }
}
