package ma.enset.aiagentservice.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Sous-ensemble des objets de la Bot API Telegram utilisés par le bot.
 */
public final class TelegramDtos {

    private TelegramDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiResponse<T>(boolean ok, T result, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Update(@JsonProperty("update_id") long updateId, Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(@JsonProperty("message_id") long messageId, Chat chat, User from, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(long id, String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(long id, @JsonProperty("first_name") String firstName, String username) {
    }
}
