package ma.enset.aiagentservice.service;

public interface AgentService {

    /**
     * Envoie un message à l'agent ; la mémoire de conversation est isolée par conversationId.
     */
    String chat(String conversationId, String message);
}
