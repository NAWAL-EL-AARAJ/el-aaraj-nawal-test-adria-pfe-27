package ma.enset.aiagentservice.telegram;

import ma.enset.aiagentservice.telegram.dto.TelegramDtos.ApiResponse;
import ma.enset.aiagentservice.telegram.dto.TelegramDtos.Update;
import ma.enset.aiagentservice.telegram.dto.TelegramDtos.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client minimal de la Bot API Telegram (https://core.telegram.org/bots/api).
 */
public class TelegramApiClient {

    private final RestClient restClient;
    private final String token;

    public TelegramApiClient(RestClient.Builder builder, TelegramProperties properties) {
        this.token = properties.token();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // le long polling garde la requête ouverte pollingTimeoutSeconds : le read timeout doit être plus long
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.pollingTimeoutSeconds() + 15L));
        this.restClient = builder.clone()
                .baseUrl("https://api.telegram.org/bot" + token)
                .requestFactory(requestFactory)
                .build();
    }

    public User getMe() {
        return call("getMe", Map.of(), new ParameterizedTypeReference<ApiResponse<User>>() {
        });
    }

    /**
     * Supprime un éventuel webhook : sinon Telegram refuse getUpdates (409 Conflict).
     */
    public void deleteWebhook() {
        call("deleteWebhook", Map.of("drop_pending_updates", false),
                new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                });
    }

    public List<Update> getUpdates(long offset, int timeoutSeconds) {
        return call("getUpdates",
                Map.of("offset", offset, "timeout", timeoutSeconds, "allowed_updates", List.of("message")),
                new ParameterizedTypeReference<ApiResponse<List<Update>>>() {
                });
    }

    public void sendMessage(long chatId, String text) {
        call("sendMessage", Map.of("chat_id", chatId, "text", text),
                new ParameterizedTypeReference<ApiResponse<Object>>() {
                });
    }

    public void sendTyping(long chatId) {
        call("sendChatAction", Map.of("chat_id", chatId, "action", "typing"),
                new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                });
    }

    /**
     * Masque le token dans un message d'erreur (l'URL appelée le contient).
     */
    public String hideToken(String message) {
        return message == null ? null : message.replace(token, "***");
    }

    private <T> T call(String method, Map<String, Object> body, ParameterizedTypeReference<ApiResponse<T>> type) {
        ApiResponse<T> response = restClient.post()
                .uri("/{method}", method)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(type);
        if (response == null || !response.ok()) {
            throw new IllegalStateException("Appel Telegram " + method + " refusé : "
                    + (response == null ? "réponse vide" : response.description()));
        }
        return response.result();
    }
}
