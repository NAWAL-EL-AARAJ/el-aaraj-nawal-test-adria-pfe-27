package ma.enset.transactionservice.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publie les TransactionCreatedEvent dans Kafka uniquement après le commit de la transaction JPA :
 * une transaction annulée (rollback) ne produit jamais d'événement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @Value("${bank.kafka.topic.transactions:bank.transactions.created}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        String key = event.accountId().toString();
        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Échec de publication de l'événement {} : {}", event.eventId(), ex.getMessage());
                } else {
                    log.info("Événement {} publié dans {} (clé={}, partition={}, offset={})", event.eventId(),
                            topic, key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (RuntimeException ex) {
            // Kafka injoignable : la transaction est déjà validée, on journalise sans faire échouer la requête
            log.error("Kafka indisponible, événement {} non publié : {}", event.eventId(), ex.getMessage());
        }
    }
}
