package ma.enset.transactionservice.service.exception;

import java.util.UUID;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(UUID id, String status) {
        super("Le compte " + id + " n'est pas actif (statut : " + status + ")");
    }
}
