package ma.enset.transactionservice.service.exception;

/**
 * account-service a refusé l'opération (ex : compte bloqué ou solde insuffisant entre la vérification et le débit).
 */
public class AccountOperationRejectedException extends RuntimeException {

    public AccountOperationRejectedException(String message) {
        super(message);
    }
}
