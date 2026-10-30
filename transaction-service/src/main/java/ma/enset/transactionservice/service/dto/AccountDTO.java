package ma.enset.transactionservice.service.dto;

import java.util.UUID;

public record AccountDTO(
        UUID id,
        String customerReference,
        String status,
        String currency,
        double balance
) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
