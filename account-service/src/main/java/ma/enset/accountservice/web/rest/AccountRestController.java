package ma.enset.accountservice.web.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.enset.accountservice.service.AccountService;
import ma.enset.accountservice.service.dto.AccountRequestDTO;
import ma.enset.accountservice.service.dto.AccountResponseDTO;
import ma.enset.accountservice.service.dto.AmountRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponseDTO> findAll() {
        return accountService.findAll();
    }

    @GetMapping("/{id}")
    public AccountResponseDTO findById(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponseDTO create(@Valid @RequestBody AccountRequestDTO request) {
        return accountService.create(request);
    }

    @PutMapping("/{id}/block")
    public AccountResponseDTO block(@PathVariable UUID id) {
        return accountService.block(id);
    }

    @PutMapping("/{id}/activate")
    public AccountResponseDTO activate(@PathVariable UUID id) {
        return accountService.activate(id);
    }

    @PostMapping("/{id}/debit")
    public AccountResponseDTO debit(@PathVariable UUID id, @Valid @RequestBody AmountRequestDTO request) {
        return accountService.debit(id, request.amount());
    }

    @PostMapping("/{id}/credit")
    public AccountResponseDTO credit(@PathVariable UUID id, @Valid @RequestBody AmountRequestDTO request) {
        return accountService.credit(id, request.amount());
    }
}
