package ma.enset.transactionservice.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestDTO(
        @NotNull UUID fromAccountId,
        @NotNull UUID toAccountId,
        @NotNull BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
