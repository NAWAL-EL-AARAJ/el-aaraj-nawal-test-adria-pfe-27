package ma.enset.aiagentservice.service.exception;

public class AgentUnavailableException extends RuntimeException {

    public AgentUnavailableException(Throwable cause) {
        super("L'assistant est momentanément indisponible (modèle IA ou service bancaire injoignable), "
                + "réessayez dans quelques instants", cause);
    }
}
