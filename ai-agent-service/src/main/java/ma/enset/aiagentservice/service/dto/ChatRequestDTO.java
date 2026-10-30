package ma.enset.aiagentservice.service.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank String conversationId,
        @NotBlank String message
) {
}
