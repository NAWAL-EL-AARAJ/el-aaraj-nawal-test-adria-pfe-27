package ma.enset.accountservice.service;

import ma.enset.accountservice.domain.Account;
import ma.enset.accountservice.domain.AccountStatus;
import ma.enset.accountservice.repository.AccountRepository;
import ma.enset.accountservice.service.dto.AccountRequestDTO;
import ma.enset.accountservice.service.dto.AccountResponseDTO;
import ma.enset.accountservice.service.exception.AccountBlockedException;
import ma.enset.accountservice.service.exception.AccountNotFoundException;
import ma.enset.accountservice.service.exception.InsufficientBalanceException;
import ma.enset.accountservice.service.mapper.AccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Spy
    private AccountMapper accountMapper = new AccountMapper();

    @InjectMocks
    private AccountServiceImpl accountService;

    private UUID id;
    private Account account;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        account = Account.builder()
                .id(id)
                .customerReference("CUST-001")
                .status(AccountStatus.ACTIVE)
                .currency("EUR")
                .balance(1000)
                .build();
    }

    private void saveReturnsArgument() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void findAll_retourneTousLesComptes() {
        when(accountRepository.findAll()).thenReturn(List.of(account));

        List<AccountResponseDTO> result = accountService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(id);
    }

    @Test
    void findById_retourneLeCompte() {
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        AccountResponseDTO result = accountService.findById(id);

        assertThat(result.customerReference()).isEqualTo("CUST-001");
        assertThat(result.balance()).isEqualTo(1000);
    }

    @Test
    void findById_compteInexistant_leveAccountNotFoundException() {
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findById(id))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void create_creeUnCompteActifAvecDeviseEnMajuscules() {
        saveReturnsArgument();

        AccountResponseDTO result = accountService.create(new AccountRequestDTO("CUST-009", "mad", 250));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.currency()).isEqualTo("MAD");
        assertThat(result.balance()).isEqualTo(250);
    }

    @Test
    void block_passeLeStatutABlocked() {
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        saveReturnsArgument();

        assertThat(accountService.block(id).status()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void activate_passeLeStatutAActive() {
        account.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        saveReturnsArgument();

        assertThat(accountService.activate(id).status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void debit_diminueLeSolde() {
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        saveReturnsArgument();

        assertThat(accountService.debit(id, 300).balance()).isEqualTo(700);
    }

    @Test
    void debit_soldeInsuffisant_leveInsufficientBalanceException() {
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit(id, 1500))
                .isInstanceOf(InsufficientBalanceException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void debit_compteBloque_leveAccountBlockedException() {
        account.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit(id, 100))
                .isInstanceOf(AccountBlockedException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void debit_montantNegatif_leveIllegalArgumentException() {
        assertThatThrownBy(() -> accountService.debit(id, -5))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void credit_augmenteLeSolde() {
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        saveReturnsArgument();

        assertThat(accountService.credit(id, 500).balance()).isEqualTo(1500);
    }

    @Test
    void credit_compteInexistant_leveAccountNotFoundException() {
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.credit(id, 100))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
