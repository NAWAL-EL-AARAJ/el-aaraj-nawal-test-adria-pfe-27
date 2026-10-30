package ma.enset.accountservice.service;

import ma.enset.accountservice.service.dto.AccountRequestDTO;
import ma.enset.accountservice.service.dto.AccountResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    List<AccountResponseDTO> findAll();

    AccountResponseDTO findById(UUID id);

    AccountResponseDTO create(AccountRequestDTO request);

    AccountResponseDTO block(UUID id);

    AccountResponseDTO activate(UUID id);

    AccountResponseDTO debit(UUID id, double amount);

    AccountResponseDTO credit(UUID id, double amount);
}
