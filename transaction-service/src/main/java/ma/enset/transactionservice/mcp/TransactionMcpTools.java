package ma.enset.transactionservice.mcp;

import lombok.RequiredArgsConstructor;
import ma.enset.transactionservice.service.TransactionService;
import ma.enset.transactionservice.service.dto.TransactionRequestDTO;
import ma.enset.transactionservice.service.dto.TransactionResponseDTO;
import ma.enset.transactionservice.service.dto.TransferRequestDTO;
import ma.enset.transactionservice.service.dto.TransferResponseDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionMcpTools {

    private final TransactionService transactionService;

    @Tool(description = "Enregistre un paiement (débit) sur un compte bancaire et met à jour son solde")
    public TransactionResponseDTO createTransaction(
            @ToolParam(description = "Identifiant UUID du compte") String accountId,
            @ToolParam(description = "Montant strictement positif") double amount,
            @ToolParam(description = "Code devise sur 3 lettres, identique à celle du compte (ex: EUR, MAD)") String currency,
            @ToolParam(description = "Catégorie du marchand, ex: GROCERY, TRAVEL, RESTAURANT") String merchantCategory,
            @ToolParam(description = "Code pays sur 2 lettres, ex: FR, MA") String country) {
        return transactionService.create(new TransactionRequestDTO(UUID.fromString(accountId),
                BigDecimal.valueOf(amount), currency, merchantCategory, country));
    }

    @Tool(description = "Liste les transactions d'un compte bancaire, de la plus récente à la plus ancienne")
    public List<TransactionResponseDTO> listTransactionsByAccount(
            @ToolParam(description = "Identifiant UUID du compte") String accountId) {
        return transactionService.findByAccountId(UUID.fromString(accountId));
    }

    @Tool(description = "Effectue un virement entre deux comptes de même devise")
    public TransferResponseDTO transfer(
            @ToolParam(description = "Identifiant UUID du compte à débiter") String fromAccountId,
            @ToolParam(description = "Identifiant UUID du compte à créditer") String toAccountId,
            @ToolParam(description = "Montant strictement positif") double amount,
            @ToolParam(description = "Code devise sur 3 lettres, identique à celle des deux comptes") String currency) {
        return transactionService.transfer(new TransferRequestDTO(UUID.fromString(fromAccountId),
                UUID.fromString(toAccountId), BigDecimal.valueOf(amount), currency));
    }
}
