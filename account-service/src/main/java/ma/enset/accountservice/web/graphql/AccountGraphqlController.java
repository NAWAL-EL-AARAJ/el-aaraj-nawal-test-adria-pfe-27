package ma.enset.accountservice.web.graphql;

import lombok.RequiredArgsConstructor;
import ma.enset.accountservice.service.AccountService;
import ma.enset.accountservice.service.dto.AccountRequestDTO;
import ma.enset.accountservice.service.dto.AccountResponseDTO;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AccountGraphqlController {

    private final AccountService accountService;

    @QueryMapping
    public List<AccountResponseDTO> accounts() {
        return accountService.findAll();
    }

    @QueryMapping
    public AccountResponseDTO accountById(@Argument UUID id) {
        return accountService.findById(id);
    }

    @MutationMapping
    public AccountResponseDTO createAccount(@Argument AccountRequestDTO input) {
        return accountService.create(input);
    }

    @MutationMapping
    public AccountResponseDTO blockAccount(@Argument UUID id) {
        return accountService.block(id);
    }

    @MutationMapping
    public AccountResponseDTO activateAccount(@Argument UUID id) {
        return accountService.activate(id);
    }
}
