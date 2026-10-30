package ma.enset.transactionservice.service.event;

import ma.enset.transactionservice.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Événement publié dans Kafka (topic bank.transactions.created) après le commit d'une transaction.
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

    public static final String TYPE = "TransactionCreated";

    public static TransactionCreatedEvent from(Transaction transaction) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(),
                TYPE,
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getCreatedAt()
        );
    }
}
