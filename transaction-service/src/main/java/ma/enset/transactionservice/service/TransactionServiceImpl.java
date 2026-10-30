package ma.enset.transactionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.enset.transactionservice.config.BankTransactionProperties;
import ma.enset.transactionservice.domain.Transaction;
import ma.enset.transactionservice.domain.TransactionType;
import ma.enset.transactionservice.repository.TransactionRepository;
import ma.enset.transactionservice.service.dto.*;
import ma.enset.transactionservice.service.event.TransactionCreatedEvent;
import ma.enset.transactionservice.service.exception.*;
import ma.enset.transactionservice.service.mapper.TransactionMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    static final String TRANSFER_CATEGORY = "TRANSFER";

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountGateway accountGateway;
    private final BankTransactionProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public TransactionResponseDTO create(TransactionRequestDTO request) {
        String currency = request.currency().toUpperCase();
        checkAmount(request.amount());
        AccountDTO account = getUsableAccount(request.accountId(), currency);
        checkBalance(account, request.amount());

        Transaction saved = transactionRepository.save(Transaction.builder()
                .accountId(account.id())
                .amount(request.amount())
                .currency(currency)
                .merchantCategory(request.merchantCategory())
                .country(request.country().toUpperCase())
                .type(TransactionType.DEBIT)
                .build());

        accountGateway.debit(account.id(), request.amount());
        // envoyé dans Kafka par TransactionEventPublisher, seulement après le commit
        eventPublisher.publishEvent(TransactionCreatedEvent.from(saved));
        return transactionMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> findAll() {
        return transactionRepository.findAll().stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDTO findById(UUID id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> findByAccountId(UUID accountId) {
        return transactionRepository.findByAccountId(accountId).stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    @Override
    public TransferResponseDTO transfer(TransferRequestDTO request) {
        String currency = request.currency().toUpperCase();
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new InvalidTransferException("Le compte source et le compte destination doivent être différents");
        }
        checkAmount(request.amount());
        AccountDTO source = getUsableAccount(request.fromAccountId(), currency);
        AccountDTO target = getUsableAccount(request.toAccountId(), currency);
        checkBalance(source, request.amount());

        Transaction debit = transactionRepository.save(transferTransaction(source.id(), request.amount(), currency,
                TransactionType.DEBIT));
        Transaction credit = transactionRepository.save(transferTransaction(target.id(), request.amount(), currency,
                TransactionType.CREDIT));

        accountGateway.debit(source.id(), request.amount());
        try {
            accountGateway.credit(target.id(), request.amount());
        } catch (RuntimeException ex) {
            // compensation : on rembourse le compte source avant d'annuler la transaction locale
            log.warn("Échec du crédit sur {}, remboursement du compte {}", target.id(), source.id());
            accountGateway.credit(source.id(), request.amount());
            throw ex;
        }
        eventPublisher.publishEvent(TransactionCreatedEvent.from(debit));
        eventPublisher.publishEvent(TransactionCreatedEvent.from(credit));
        return new TransferResponseDTO(transactionMapper.toDto(debit), transactionMapper.toDto(credit));
    }

    private Transaction transferTransaction(UUID accountId, BigDecimal amount, String currency, TransactionType type) {
        return Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .currency(currency)
                .merchantCategory(TRANSFER_CATEGORY)
                .type(type)
                .build();
    }

    private void checkAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Le montant doit être strictement positif");
        }
        BigDecimal max = properties.getMaxAmount();
        if (amount.compareTo(max) > 0) {
            throw new InvalidAmountException("Le montant " + amount.toPlainString()
                    + " dépasse le maximum autorisé (" + max.toPlainString() + ")");
        }
    }

    private AccountDTO getUsableAccount(UUID accountId, String currency) {
        AccountDTO account = accountGateway.getAccount(accountId);
        if (!account.isActive()) {
            throw new AccountNotActiveException(accountId, account.status());
        }
        if (!account.currency().equalsIgnoreCase(currency)) {
            throw new CurrencyMismatchException(accountId, account.currency(), currency);
        }
        return account;
    }

    private void checkBalance(AccountDTO account, BigDecimal amount) {
        BigDecimal balance = BigDecimal.valueOf(account.balance());
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(account.id(), balance, amount);
        }
    }
}
