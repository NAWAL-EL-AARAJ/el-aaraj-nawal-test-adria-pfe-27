package ma.enset.transactionservice.service.exception;

public class AccountServiceUnavailableException extends RuntimeException {

    public AccountServiceUnavailableException() {
        super("Service compte indisponible, réessayez plus tard");
    }
}
