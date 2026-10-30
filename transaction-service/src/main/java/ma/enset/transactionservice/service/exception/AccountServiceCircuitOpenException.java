package ma.enset.transactionservice.service.exception;

/**
 * Circuit breaker ouvert : account-service n'est même pas appelé.
 * Ignorée par le retry pour ne pas réessayer inutilement tant que le circuit est ouvert.
 */
public class AccountServiceCircuitOpenException extends AccountServiceUnavailableException {
}
