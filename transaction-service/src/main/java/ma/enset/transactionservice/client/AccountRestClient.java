package ma.enset.transactionservice.client;

import ma.enset.transactionservice.service.dto.AccountDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "ACCOUNT-SERVICE")
public interface AccountRestClient {

    @GetMapping("/api/accounts/{id}")
    AccountDTO getAccount(@PathVariable("id") UUID id);

    @PostMapping("/api/accounts/{id}/debit")
    AccountDTO debit(@PathVariable("id") UUID id, @RequestBody AmountRequest request);

    @PostMapping("/api/accounts/{id}/credit")
    AccountDTO credit(@PathVariable("id") UUID id, @RequestBody AmountRequest request);

    record AmountRequest(double amount) {
    }
}
