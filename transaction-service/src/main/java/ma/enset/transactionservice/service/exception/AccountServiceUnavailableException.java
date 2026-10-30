package ma.enset.transactionservice.service.exception;

public class AccountServiceUnavailableException extends RuntimeException {

    public AccountServiceUnavailableException() {
        super("account-service est indisponible, réessayez plus tard");
    }
}
