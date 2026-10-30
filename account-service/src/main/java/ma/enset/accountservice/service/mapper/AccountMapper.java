package ma.enset.accountservice.service.mapper;

import ma.enset.accountservice.domain.Account;
import ma.enset.accountservice.domain.AccountStatus;
import ma.enset.accountservice.service.dto.AccountRequestDTO;
import ma.enset.accountservice.service.dto.AccountResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponseDTO toDto(Account account) {
        return new AccountResponseDTO(
                account.getId(),
                account.getCustomerReference(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance()
        );
    }

    public Account toEntity(AccountRequestDTO request) {
        return Account.builder()
                .customerReference(request.customerReference())
                .currency(request.currency().toUpperCase())
                .balance(request.balance())
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
