package ma.enset.accountservice.service.exception;

import java.util.UUID;

public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException(UUID id) {
        super("Le compte " + id + " est bloqué");
    }
}
