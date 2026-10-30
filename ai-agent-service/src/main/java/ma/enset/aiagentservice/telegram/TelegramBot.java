package ma.enset.aiagentservice.telegram;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.enset.aiagentservice.service.AgentService;
import ma.enset.aiagentservice.service.exception.AgentUnavailableException;
import ma.enset.aiagentservice.telegram.dto.TelegramDtos.Message;
import ma.enset.aiagentservice.telegram.dto.TelegramDtos.Update;
import ma.enset.aiagentservice.telegram.dto.TelegramDtos.User;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bot Telegram en long polling (aucun webhook : fonctionne en local, sans URL publique).
 * Chaque message texte passe par le même AgentService que /api/chat, avec le chatId comme conversationId.
 */
@Slf4j
@RequiredArgsConstructor
public class TelegramBot {

    static final String WELCOME_MESSAGE = """
            Bonjour%s ! Je suis l'assistant bancaire.

            Je peux, en langage naturel :
            - lister les comptes et afficher le détail d'un compte ;
            - consulter le solde d'un compte ;
            - créer un compte ou bloquer un compte ;
            - enregistrer un paiement sur un compte ;
            - afficher l'historique des transactions d'un compte ;
            - faire un virement entre deux comptes de même devise.

            Exemples :
            « Liste les comptes »
            « Quel est le solde du compte de CUST-001 ? »
            « Vire 100 EUR du compte <id1> vers le compte <id2> »

            Je me souviens de notre conversation : vous pouvez faire référence à vos messages précédents.""";

    static final String ERROR_MESSAGE = "Désolé, je ne peux pas répondre pour le moment : %s.";
    static final String TEXT_ONLY_MESSAGE = "Je ne comprends que les messages texte. Envoyez /start pour voir ce que je sais faire.";

    private final TelegramApiClient api;
    private final AgentService agentService;
    private final TelegramProperties properties;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    // un seul message traité à la fois par conversation, pour garder l'ordre dans la mémoire de l'agent
    private final Map<Long, ReentrantLock> chatLocks = new ConcurrentHashMap<>();
    private volatile boolean running;
    private Thread pollingThread;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        try {
            User me = api.getMe();
            api.deleteWebhook();
            log.info("Bot Telegram connecté : @{} (id {}), mode long polling", me.username(), me.id());
            if (properties.username() != null && !properties.username().isBlank()
                    && !properties.username().replace("@", "").equalsIgnoreCase(me.username())) {
                log.warn("TELEGRAM_BOT_USERNAME ne correspond pas au bot du token (@{})", me.username());
            }
        } catch (RuntimeException ex) {
            log.error("Connexion du bot Telegram impossible, bot désactivé : {}", api.hideToken(ex.getMessage()));
            return;
        }
        running = true;
        pollingThread = Thread.ofPlatform().name("telegram-polling").daemon().start(this::poll);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
        executor.shutdownNow();
    }

    private void poll() {
        long offset = 0;
        while (running) {
            try {
                List<Update> updates = api.getUpdates(offset, properties.pollingTimeoutSeconds());
                for (Update update : updates) {
                    offset = update.updateId() + 1;
                    if (update.message() != null) {
                        executor.submit(() -> handle(update.message()));
                    }
                }
            } catch (RuntimeException ex) {
                if (!running) {
                    break;
                }
                log.warn("Erreur de long polling Telegram, nouvel essai dans 5 s : {}", api.hideToken(ex.getMessage()));
                pause();
            }
        }
        log.info("Long polling Telegram arrêté");
    }

    private void handle(Message message) {
        long chatId = message.chat().id();
        ReentrantLock lock = chatLocks.computeIfAbsent(chatId, id -> new ReentrantLock());
        lock.lock();
        try {
            reply(chatId, answer(message));
        } catch (RuntimeException ex) {
            log.error("Échec du traitement du message Telegram (chat {}) : {}", chatId, api.hideToken(ex.getMessage()));
        } finally {
            lock.unlock();
        }
    }

    private String answer(Message message) {
        String text = message.text();
        if (text == null || text.isBlank()) {
            return TEXT_ONLY_MESSAGE;
        }
        String command = command(text);
        if ("/start".equals(command) || "/help".equals(command)) {
            String firstName = message.from() != null && message.from().firstName() != null
                    ? " " + message.from().firstName() : "";
            return WELCOME_MESSAGE.formatted(firstName);
        }
        long chatId = message.chat().id();
        log.info("Message Telegram reçu (chat {})", chatId);
        api.sendTyping(chatId);
        try {
            return agentService.chat(String.valueOf(chatId), text);
        } catch (AgentUnavailableException ex) {
            return ERROR_MESSAGE.formatted(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Erreur inattendue de l'agent (chat {}) : {}", chatId, ex.getMessage());
            return ERROR_MESSAGE.formatted("erreur inattendue, réessayez dans quelques instants");
        }
    }

    /**
     * Extrait la commande d'un message (« /start » ou « /start@NomDuBot »), ou null si ce n'en est pas une.
     */
    private String command(String text) {
        if (!text.startsWith("/")) {
            return null;
        }
        String command = text.strip().split("\\s+", 2)[0];
        int at = command.indexOf('@');
        return (at > 0 ? command.substring(0, at) : command).toLowerCase();
    }

    private void reply(long chatId, String text) {
        for (String part : MessageSplitter.split(text)) {
            api.sendMessage(chatId, part);
        }
    }

    private void pause() {
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
