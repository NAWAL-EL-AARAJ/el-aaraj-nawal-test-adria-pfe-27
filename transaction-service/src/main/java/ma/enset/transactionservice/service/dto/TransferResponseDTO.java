package ma.enset.transactionservice.service.dto;

public record TransferResponseDTO(
        TransactionResponseDTO debit,
        TransactionResponseDTO credit
) {
}
