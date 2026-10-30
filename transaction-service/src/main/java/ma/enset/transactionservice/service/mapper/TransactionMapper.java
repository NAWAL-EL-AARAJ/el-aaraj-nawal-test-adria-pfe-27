package ma.enset.transactionservice.service.mapper;

import ma.enset.transactionservice.domain.Transaction;
import ma.enset.transactionservice.service.dto.TransactionResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponseDTO toDto(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getMerchantCategory(),
                transaction.getCountry(),
                transaction.getType(),
                transaction.getCreatedAt()
        );
    }
}
