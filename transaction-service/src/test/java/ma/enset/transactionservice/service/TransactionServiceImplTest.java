package ma.enset.transactionservice.service;

import ma.enset.transactionservice.config.BankTransactionProperties;
import ma.enset.transactionservice.domain.Transaction;
import ma.enset.transactionservice.domain.TransactionType;
import ma.enset.transactionservice.repository.TransactionRepository;
import ma.enset.transactionservice.service.dto.*;
import ma.enset.transactionservice.service.exception.*;
import ma.enset.transactionservice.service.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ma.enset.transactionservice.service.event.TransactionCreatedEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountGateway accountGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BankTransactionProperties properties;
    private TransactionServiceImpl service;

    private final UUID eurId = UUID.randomUUID();
    private final UUID eur2Id = UUID.randomUUID();
    private final UUID madId = UUID.randomUUID();
    private final UUID blockedId = UUID.randomUUID();

    private final AccountDTO eur = new AccountDTO(eurId, "CUST-001", "ACTIVE", "EUR", 500);
    private final AccountDTO eur2 = new AccountDTO(eur2Id, "CUST-004", "ACTIVE", "EUR", 100);
    private final AccountDTO mad = new AccountDTO(madId, "CUST-002", "ACTIVE", "MAD", 3000);
    private final AccountDTO blocked = new AccountDTO(blockedId, "CUST-003", "BLOCKED", "EUR", 1000);

    @BeforeEach
    void setUp() {
        properties = new BankTransactionProperties();
        properties.setMaxAmount(new BigDecimal("1000"));
        service = new TransactionServiceImpl(transactionRepository, new TransactionMapper(), accountGateway, properties,
                eventPublisher);
    }

    private void saveReturnsArgument() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
    }

    private TransactionRequestDTO payment(UUID accountId, String amount, String currency) {
        return new TransactionRequestDTO(accountId, new BigDecimal(amount), currency, "GROCERY", "fr");
    }

    @Nested
    class Create {

        @Test
        void transactionValide_estEnregistreePuisLeCompteEstDebite() {
            when(accountGateway.getAccount(eurId)).thenReturn(eur);
            saveReturnsArgument();

            TransactionResponseDTO result = service.create(payment(eurId, "120.50", "eur"));

            assertThat(result.amount()).isEqualByComparingTo("120.50");
            assertThat(result.currency()).isEqualTo("EUR");
            assertThat(result.country()).isEqualTo("FR");
            assertThat(result.type()).isEqualTo(TransactionType.DEBIT);
            InOrder order = inOrder(transactionRepository, accountGateway);
            order.verify(transactionRepository).save(any(Transaction.class));
            order.verify(accountGateway).debit(eurId, new BigDecimal("120.50"));
        }

        @Test
        void transactionValide_publieUnTransactionCreatedEvent() {
            when(accountGateway.getAccount(eurId)).thenReturn(eur);
            saveReturnsArgument();

            TransactionResponseDTO result = service.create(payment(eurId, "42", "EUR"));

            ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            TransactionCreatedEvent event = captor.getValue();
            assertThat(event.eventId()).isNotNull();
            assertThat(event.eventType()).isEqualTo("TransactionCreated");
            assertThat(event.transactionId()).isEqualTo(result.id());
            assertThat(event.accountId()).isEqualTo(eurId);
            assertThat(event.amount()).isEqualByComparingTo("42");
            assertThat(event.currency()).isEqualTo("EUR");
        }

        @Test
        void transactionRefusee_nePublieAucunEvenement() {
            when(accountGateway.getAccount(eurId)).thenReturn(eur);

            assertThatThrownBy(() -> service.create(payment(eurId, "900", "EUR")))
                    .isInstanceOf(InsufficientBalanceException.class);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void montantEgalAuMaximum_estAccepte() {
            when(accountGateway.getAccount(madId)).thenReturn(mad);
            saveReturnsArgument();

            assertThat(service.create(payment(madId, "1000", "MAD")).amount()).isEqualByComparingTo("1000");
        }

        @Test
        void montantNul_estRefuse() {
            assertThatThrownBy(() -> service.create(payment(eurId, "0", "EUR")))
                    .isInstanceOf(InvalidAmountException.class)
                    .hasMessageContaining("strictement positif");
            verifyNoInteractions(accountGateway, transactionRepository);
        }

        @Test
        void montantNegatif_estRefuse() {
            assertThatThrownBy(() -> service.create(payment(eurId, "-10", "EUR")))
                    .isInstanceOf(InvalidAmountException.class);
            verifyNoInteractions(accountGateway, transactionRepository);
        }

        @Test
        void montantSuperieurAuMaximum_estRefuse() {
            assertThatThrownBy(() -> service.create(payment(madId, "1000.01", "MAD")))
                    .isInstanceOf(InvalidAmountException.class)
                    .hasMessageContaining("maximum");
            verifyNoInteractions(accountGateway, transactionRepository);
        }

        @Test
        void maximumRafraichi_estPrisEnCompte() {
            properties.setMaxAmount(new BigDecimal("50"));

            assertThatThrownBy(() -> service.create(payment(eurId, "60", "EUR")))
                    .isInstanceOf(InvalidAmountException.class);
        }

        @Test
        void compteInexistant_estRefuse() {
            when(accountGateway.getAccount(eurId)).thenThrow(new AccountNotFoundException(eurId));

            assertThatThrownBy(() -> service.create(payment(eurId, "10", "EUR")))
                    .isInstanceOf(AccountNotFoundException.class);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        void compteBloque_estRefuse() {
            when(accountGateway.getAccount(blockedId)).thenReturn(blocked);

            assertThatThrownBy(() -> service.create(payment(blockedId, "10", "EUR")))
                    .isInstanceOf(AccountNotActiveException.class);
            verifyNoInteractions(transactionRepository);
            verify(accountGateway, never()).debit(any(), any());
        }

        @Test
        void deviseDifferenteDuCompte_estRefusee() {
            when(accountGateway.getAccount(eurId)).thenReturn(eur);

            assertThatThrownBy(() -> service.create(payment(eurId, "10", "MAD")))
                    .isInstanceOf(CurrencyMismatchException.class);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        void soldeInsuffisant_estRefuse() {
            when(accountGateway.getAccount(eurId)).thenReturn(eur);

            assertThatThrownBy(() -> service.create(payment(eurId, "500.01", "EUR")))
                    .isInstanceOf(InsufficientBalanceException.class);
            verifyNoInteractions(transactionRepository);
            verify(accountGateway, never()).debit(any(), any());
        }

        @Test
        void accountServiceIndisponible_estPropage() {
            when(accountGateway.getAccount(eurId)).thenThrow(new AccountServiceUnavailableException());

            assertThatThrownBy(() -> service.create(payment(eurId, "10", "EUR")))
                    .isInstanceOf(AccountServiceUnavailableException.class);
            verifyNoInteractions(transactionRepository);
        }
    }

    @Nested
    class Transfer {

        private TransferRequestDTO transfer(UUID from, UUID to, String amount, String currency) {
            return new TransferRequestDTO(from, to, new BigDecimal(amount), currency);
        }

        @Test
        void virementValide_enregistreDeuxTransactionsDebiteEtCredite() {
            when(accountGateway.getAccount(eurId)).thenReturn(eur);
            when(accountGateway.getAccount(eur2Id)).thenReturn(eur2);
            saveReturnsArgument();

            TransferResponseDTO result = service.transfer(transfer(eurId, eur2Id, "200", "EUR"));

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues())
                    .extracting(Transaction::getAccountId, Transaction::getType)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(eurId, TransactionType.DEBIT),
                            org.assertj.core.groups.Tuple.tuple(eur2Id, TransactionType.CREDIT));
            verify(accountGateway).debit(eurId, new BigDecimal("200"));
            verify(accountGateway).credit(eur2Id, new BigDecimal("200"));
            assertThat(result.debit().merchantCategory()).isEqualTo("TRANSFER");
            assertThat(result.credit().accountId()).isEqualTo(eur2Id);
            verify(eventPublisher, times(2)).publishEvent(any(TransactionCreatedEvent.class));
        }

        @Test
        void memeCompte_estRefuse() {
            assertThatThrownBy(() -> service.transfer(transfer(eurId, eurId, "10", "EUR")))
                    .isInstanceOf(InvalidTransferException.class);
            verifyNoInteractions(accountGateway, transactionRepository);
        }

        @Test
        void montantSuperieurAuMaximum_estRefuse() {
            assertThatThrownBy(() -> service.transfer(transfer(eurId, eur2Id, "5000", "EUR")))
                    .isInstanceOf(InvalidAmountException.class);
            verifyNoInteractions(accountGateway, transactionRepository);
        }

        @Test
        void compteDestinationBloque_estRefuse() {
            when(accountGateway.getAccount(eurId)).thenReturn(eur);
            when(accountGateway.getAccount(blockedId)).thenReturn(blocked);

            assertThatThrownBy(() -> service.transfer(transfer(eurId, blockedId, "10", "EUR")))
                    .isInstanceOf(AccountNotActiveException.class);
            verifyNoInteractions(transactionRepository);
            verify(accountGateway, never()).debit(any(), any());
        }

        @Test
        void devisesDifferentes_sontRefusees() {
            when(accountGateway.getAccount(eurId)).thenReturn(eur);
            when(accountGateway.getAccount(madId)).thenReturn(mad);

            assertThatThrownBy(() -> service.transfer(transfer(eurId, madId, "10", "EUR")))
                    .isInstanceOf(CurrencyMismatchException.class);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        void soldeSourceInsuffisant_estRefuse() {
            when(accountGateway.getAccount(eur2Id)).thenReturn(eur2);
            when(accountGateway.getAccount(eurId)).thenReturn(eur);

            assertThatThrownBy(() -> service.transfer(transfer(eur2Id, eurId, "150", "EUR")))
                    .isInstanceOf(InsufficientBalanceException.class);
            verifyNoInteractions(transactionRepository);
            verify(accountGateway, never()).debit(any(), any());
        }

        @Test
        void echecDuCredit_rembourseLeCompteSource() {
            when(accountGateway.getAccount(eurId)).thenReturn(eur);
            when(accountGateway.getAccount(eur2Id)).thenReturn(eur2);
            saveReturnsArgument();
            when(accountGateway.credit(eur2Id, new BigDecimal("200")))
                    .thenThrow(new AccountServiceUnavailableException());

            assertThatThrownBy(() -> service.transfer(transfer(eurId, eur2Id, "200", "EUR")))
                    .isInstanceOf(AccountServiceUnavailableException.class);
            verify(accountGateway).debit(eurId, new BigDecimal("200"));
            verify(accountGateway).credit(eurId, new BigDecimal("200"));
        }
    }

    @Nested
    class Consultation {

        @Test
        void findById_inexistant_leveTransactionNotFoundException() {
            UUID id = UUID.randomUUID();
            when(transactionRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(id)).isInstanceOf(TransactionNotFoundException.class);
        }

        @Test
        void findByAccountId_retourneLesTransactionsDuCompte() {
            Transaction t = Transaction.builder().id(UUID.randomUUID()).accountId(eurId)
                    .amount(BigDecimal.TEN).currency("EUR").type(TransactionType.DEBIT).build();
            when(transactionRepository.findByAccountId(eurId)).thenReturn(List.of(t));

            assertThat(service.findByAccountId(eurId)).singleElement()
                    .extracting(TransactionResponseDTO::accountId).isEqualTo(eurId);
        }
    }
}
