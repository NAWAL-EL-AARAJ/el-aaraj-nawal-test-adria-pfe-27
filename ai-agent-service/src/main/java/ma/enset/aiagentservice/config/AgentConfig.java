package ma.enset.aiagentservice.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    private static final String SYSTEM_PROMPT = """
            Tu es l'assistant bancaire de la banque. Tu aides l'utilisateur à gérer ses comptes et ses transactions
            grâce aux outils mis à ta disposition (account-service et transaction-service).
            Règles :
            - utilise toujours les outils pour obtenir les comptes, soldes et transactions : n'invente jamais
              d'identifiant, de solde ni de transaction ;
            - si un identifiant de compte manque ou est ambigu, liste les comptes ou demande des précisions ;
            - si un outil renvoie une erreur, explique-la simplement à l'utilisateur ;
            - réponds dans la langue de l'utilisateur, de façon concise, en texte brut sans Markdown
              (les réponses sont aussi affichées dans Telegram).
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, ToolCallbackProvider mcpTools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(mcpTools)
                .build();
    }
}
