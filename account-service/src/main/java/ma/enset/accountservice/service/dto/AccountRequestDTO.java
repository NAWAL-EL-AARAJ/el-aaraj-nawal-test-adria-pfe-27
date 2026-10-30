package ma.enset.accountservice.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AccountRequestDTO(
        @NotBlank String customerReference,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @PositiveOrZero double balance
) {
}
