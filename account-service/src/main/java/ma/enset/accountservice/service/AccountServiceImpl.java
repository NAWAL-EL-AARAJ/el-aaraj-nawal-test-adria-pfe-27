package ma.enset.accountservice.service;

import lombok.RequiredArgsConstructor;
import ma.enset.accountservice.domain.Account;
import ma.enset.accountservice.domain.AccountStatus;
import ma.enset.accountservice.repository.AccountRepository;
import ma.enset.accountservice.service.dto.AccountRequestDTO;
import ma.enset.accountservice.service.dto.AccountResponseDTO;
import ma.enset.accountservice.service.exception.AccountBlockedException;
import ma.enset.accountservice.service.exception.AccountNotFoundException;
import ma.enset.accountservice.service.exception.InsufficientBalanceException;
import ma.enset.accountservice.service.mapper.AccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponseDTO> findAll() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDTO findById(UUID id) {
        return accountMapper.toDto(getAccount(id));
    }

    @Override
    public AccountResponseDTO create(AccountRequestDTO request) {
        Account saved = accountRepository.save(accountMapper.toEntity(request));
        return accountMapper.toDto(saved);
    }

    @Override
    public AccountResponseDTO block(UUID id) {
        Account account = getAccount(id);
        account.setStatus(AccountStatus.BLOCKED);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Override
    public AccountResponseDTO activate(UUID id) {
        Account account = getAccount(id);
        account.setStatus(AccountStatus.ACTIVE);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Override
    public AccountResponseDTO debit(UUID id, double amount) {
        checkAmount(amount);
        Account account = getActiveAccount(id);
        if (account.getBalance() < amount) {
            throw new InsufficientBalanceException(id, account.getBalance(), amount);
        }
        account.setBalance(account.getBalance() - amount);
        return accountMapper.toDto(accountRepository.save(account));
    }

    @Override
    public AccountResponseDTO credit(UUID id, double amount) {
        checkAmount(amount);
        Account account = getActiveAccount(id);
        account.setBalance(account.getBalance() + amount);
        return accountMapper.toDto(accountRepository.save(account));
    }

    private Account getAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private Account getActiveAccount(UUID id) {
        Account account = getAccount(id);
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new AccountBlockedException(id);
        }
        return account;
    }

    private void checkAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif");
        }
    }
}
