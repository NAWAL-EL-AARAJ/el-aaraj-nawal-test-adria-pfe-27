package ma.enset.accountservice.service.dto;

import ma.enset.accountservice.domain.AccountStatus;

import java.util.UUID;

public record AccountResponseDTO(
        UUID id,
        String customerReference,
        AccountStatus status,
        String currency,
        double balance
) {
}
