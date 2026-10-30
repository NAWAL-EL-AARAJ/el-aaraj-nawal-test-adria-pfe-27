package ma.enset.accountservice.mcp;

import lombok.RequiredArgsConstructor;
import ma.enset.accountservice.service.AccountService;
import ma.enset.accountservice.service.dto.AccountRequestDTO;
import ma.enset.accountservice.service.dto.AccountResponseDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountMcpTools {

    private final AccountService accountService;

    @Tool(description = "Liste tous les comptes bancaires")
    public List<AccountResponseDTO> listAccounts() {
        return accountService.findAll();
    }

    @Tool(description = "Retourne le détail d'un compte bancaire à partir de son identifiant")
    public AccountResponseDTO getAccount(@ToolParam(description = "Identifiant UUID du compte") String accountId) {
        return accountService.findById(UUID.fromString(accountId));
    }

    @Tool(description = "Retourne le solde et la devise d'un compte bancaire")
    public String getBalance(@ToolParam(description = "Identifiant UUID du compte") String accountId) {
        AccountResponseDTO account = accountService.findById(UUID.fromString(accountId));
        return String.format(Locale.ROOT, "%.2f %s", account.balance(), account.currency());
    }

    @Tool(description = "Crée un nouveau compte bancaire actif")
    public AccountResponseDTO createAccount(
            @ToolParam(description = "Référence du client") String customerReference,
            @ToolParam(description = "Code devise sur 3 lettres, ex: EUR, MAD") String currency,
            @ToolParam(description = "Solde initial") double balance) {
        return accountService.create(new AccountRequestDTO(customerReference, currency, balance));
    }

    @Tool(description = "Bloque un compte bancaire")
    public AccountResponseDTO blockAccount(@ToolParam(description = "Identifiant UUID du compte") String accountId) {
        return accountService.block(UUID.fromString(accountId));
    }
}
