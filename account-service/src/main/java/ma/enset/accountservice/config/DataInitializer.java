package ma.enset.accountservice.config;

import lombok.extern.slf4j.Slf4j;
import ma.enset.accountservice.service.AccountService;
import ma.enset.accountservice.service.dto.AccountRequestDTO;
import ma.enset.accountservice.service.dto.AccountResponseDTO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initAccounts(AccountService accountService) {
        return args -> {
            AccountResponseDTO eur = accountService.create(new AccountRequestDTO("CUST-001", "EUR", 5000));
            AccountResponseDTO mad = accountService.create(new AccountRequestDTO("CUST-002", "MAD", 3000));
            AccountResponseDTO blocked = accountService.block(
                    accountService.create(new AccountRequestDTO("CUST-003", "EUR", 1000)).id());

            log.info("Compte EUR actif  (5000 EUR) : {}", eur.id());
            log.info("Compte MAD actif  (3000 MAD) : {}", mad.id());
            log.info("Compte EUR BLOCKED (1000 EUR) : {}", blocked.id());
        };
    }
}
