package ma.enset.aiagentservice.telegram;

import java.util.ArrayList;
import java.util.List;

/**
 * Découpe un texte trop long pour un seul message Telegram (limite : 4096 caractères),
 * de préférence sur un retour à la ligne, sinon sur un espace.
 */
public final class MessageSplitter {

    public static final int MAX_LENGTH = 4000;

    private MessageSplitter() {
    }

    public static List<String> split(String text) {
        return split(text, MAX_LENGTH);
    }

    public static List<String> split(String text, int maxLength) {
        List<String> parts = new ArrayList<>();
        String rest = text == null ? "" : text.strip();
        while (rest.length() > maxLength) {
            int cut = rest.lastIndexOf('\n', maxLength);
            if (cut < maxLength / 2) {
                cut = rest.lastIndexOf(' ', maxLength);
            }
            if (cut < maxLength / 2) {
                cut = maxLength;
                // ne pas couper un emoji (paire de substitution UTF-16) en deux
                if (Character.isHighSurrogate(rest.charAt(cut - 1))) {
                    cut--;
                }
            }
            parts.add(rest.substring(0, cut).strip());
            rest = rest.substring(cut).strip();
        }
        if (!rest.isEmpty()) {
            parts.add(rest);
        }
        return parts;
    }
}
