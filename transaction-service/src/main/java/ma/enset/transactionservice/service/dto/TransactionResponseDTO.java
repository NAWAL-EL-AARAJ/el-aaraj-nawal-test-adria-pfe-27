package ma.enset.transactionservice.service.dto;

import ma.enset.transactionservice.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponseDTO(
        UUID id,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country,
        TransactionType type,
        Instant createdAt
) {
}
