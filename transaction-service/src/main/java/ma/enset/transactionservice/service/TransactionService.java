package ma.enset.transactionservice.service;

import ma.enset.transactionservice.service.dto.TransactionRequestDTO;
import ma.enset.transactionservice.service.dto.TransactionResponseDTO;
import ma.enset.transactionservice.service.dto.TransferRequestDTO;
import ma.enset.transactionservice.service.dto.TransferResponseDTO;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransactionResponseDTO create(TransactionRequestDTO request);

    List<TransactionResponseDTO> findAll();

    TransactionResponseDTO findById(UUID id);

    List<TransactionResponseDTO> findByAccountId(UUID accountId);

    TransferResponseDTO transfer(TransferRequestDTO request);
}
