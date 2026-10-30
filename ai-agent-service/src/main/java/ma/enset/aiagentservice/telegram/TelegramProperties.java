package ma.enset.aiagentservice.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du bot : le token et le nom viennent des variables d'environnement
 * TELEGRAM_BOT_TOKEN et TELEGRAM_BOT_USERNAME (jamais écrits dans un fichier).
 */
@ConfigurationProperties(prefix = "telegram.bot")
public record TelegramProperties(String token, String username, int pollingTimeoutSeconds) {

    public TelegramProperties {
        if (pollingTimeoutSeconds <= 0) {
            pollingTimeoutSeconds = 30;
        }
    }
}
