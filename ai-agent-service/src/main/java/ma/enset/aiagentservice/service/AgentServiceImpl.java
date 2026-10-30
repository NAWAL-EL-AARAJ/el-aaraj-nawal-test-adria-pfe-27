package ma.enset.aiagentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.enset.aiagentservice.service.exception.AgentUnavailableException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;

    @Override
    public String chat(String conversationId, String message) {
        try {
            String answer = chatClient.prompt()
                    .user(message)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            return answer == null || answer.isBlank() ? "Je n'ai pas de réponse à proposer." : answer;
        } catch (RuntimeException ex) {
            log.error("Échec de l'agent (conversation {}) : {}", conversationId, ex.getMessage());
            throw new AgentUnavailableException(ex);
        }
    }
}
