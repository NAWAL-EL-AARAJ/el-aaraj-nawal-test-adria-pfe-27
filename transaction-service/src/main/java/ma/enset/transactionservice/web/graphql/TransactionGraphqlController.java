package ma.enset.transactionservice.web.graphql;

import lombok.RequiredArgsConstructor;
import ma.enset.transactionservice.service.TransactionService;
import ma.enset.transactionservice.service.dto.TransactionRequestDTO;
import ma.enset.transactionservice.service.dto.TransactionResponseDTO;
import ma.enset.transactionservice.service.dto.TransferRequestDTO;
import ma.enset.transactionservice.service.dto.TransferResponseDTO;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransactionGraphqlController {

    private final TransactionService transactionService;

    @QueryMapping
    public List<TransactionResponseDTO> transactions() {
        return transactionService.findAll();
    }

    @QueryMapping
    public TransactionResponseDTO transactionById(@Argument UUID id) {
        return transactionService.findById(id);
    }

    @QueryMapping
    public List<TransactionResponseDTO> transactionsByAccount(@Argument UUID accountId) {
        return transactionService.findByAccountId(accountId);
    }

    @MutationMapping
    public TransactionResponseDTO createTransaction(@Argument TransactionRequestDTO input) {
        return transactionService.create(input);
    }

    @MutationMapping
    public TransferResponseDTO transfer(@Argument TransferRequestDTO input) {
        return transactionService.transfer(input);
    }
}
