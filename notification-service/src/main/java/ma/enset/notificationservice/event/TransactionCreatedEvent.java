package ma.enset.notificationservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Copie locale du contrat de l'événement publié par transaction-service (topic bank.transactions.created).
 */
public record TransactionCreatedEvent(
        UUID eventId,
        String eventType,
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
}
