package ma.enset.transactionservice.web.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.enset.transactionservice.service.TransactionService;
import ma.enset.transactionservice.service.dto.TransactionRequestDTO;
import ma.enset.transactionservice.service.dto.TransactionResponseDTO;
import ma.enset.transactionservice.service.dto.TransferRequestDTO;
import ma.enset.transactionservice.service.dto.TransferResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionRestController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDTO create(@Valid @RequestBody TransactionRequestDTO request) {
        return transactionService.create(request);
    }

    @GetMapping
    public List<TransactionResponseDTO> findAll() {
        return transactionService.findAll();
    }

    @GetMapping("/{id}")
    public TransactionResponseDTO findById(@PathVariable UUID id) {
        return transactionService.findById(id);
    }

    @GetMapping("/account/{accountId}")
    public List<TransactionResponseDTO> findByAccountId(@PathVariable UUID accountId) {
        return transactionService.findByAccountId(accountId);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponseDTO transfer(@Valid @RequestBody TransferRequestDTO request) {
        return transactionService.transfer(request);
    }
}
