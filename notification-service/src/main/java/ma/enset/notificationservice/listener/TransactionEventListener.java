package ma.enset.notificationservice.listener;

import lombok.extern.slf4j.Slf4j;
import ma.enset.notificationservice.event.TransactionCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme les événements de transaction. Lecture seule : aucune donnée bancaire n'est modifiée.
 */
@Slf4j
@Component
public class TransactionEventListener {

    @KafkaListener(topics = "${bank.kafka.topic.transactions:bank.transactions.created}",
            groupId = "notification-group")
    public void onTransactionCreated(ConsumerRecord<String, TransactionCreatedEvent> record) {
        TransactionCreatedEvent event = record.value();
        log.info("Événement reçu : {} | clé={} | partition={} | offset={}",
                event, record.key(), record.partition(), record.offset());
        log.info("Notification : transaction {} de {} {} sur le compte {}",
                event.transactionId(), event.amount(), event.currency(), event.accountId());
    }
}
