package ma.enset.transactionservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Propriétés métier chargées depuis le config-service (config-repo/application.properties).
 * Les setters permettent à Spring Cloud de recharger les valeurs via POST /actuator/refresh.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bank.transaction")
public class BankTransactionProperties {

    private BigDecimal maxAmount = new BigDecimal("1000");
}
