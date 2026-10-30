package ma.enset.aiagentservice.telegram;

import ma.enset.aiagentservice.service.AgentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Le bot n'est créé que si TELEGRAM_BOT_TOKEN est défini (propriété telegram.bot.token non vide).
 */
@Configuration
@ConditionalOnExpression("!'${telegram.bot.token:}'.isBlank()")
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfig {

    @Bean
    public TelegramApiClient telegramApiClient(RestClient.Builder builder, TelegramProperties properties) {
        return new TelegramApiClient(builder, properties);
    }

    @Bean
    public TelegramBot telegramBot(TelegramApiClient api, AgentService agentService, TelegramProperties properties) {
        return new TelegramBot(api, agentService, properties);
    }
}
