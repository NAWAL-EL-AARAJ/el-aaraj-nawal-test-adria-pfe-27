package ma.enset.accountservice.service.dto;

import jakarta.validation.constraints.Positive;

public record AmountRequestDTO(@Positive double amount) {
}
