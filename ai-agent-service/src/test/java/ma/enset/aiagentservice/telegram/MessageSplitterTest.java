package ma.enset.aiagentservice.telegram;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageSplitterTest {

    @Test
    void shortTextIsKeptInOneMessage() {
        assertEquals(List.of("Bonjour"), MessageSplitter.split("Bonjour"));
    }

    @Test
    void longTextIsSplitOnLineBreaks() {
        String line = "x".repeat(99) + "\n";
        String text = line.repeat(100); // 10 000 caractères

        List<String> parts = MessageSplitter.split(text);

        assertEquals(3, parts.size());
        parts.forEach(part -> assertTrue(part.length() <= MessageSplitter.MAX_LENGTH));
        assertEquals(text.replace("\n", ""), String.join("", parts).replace("\n", ""));
    }

    @Test
    void textWithoutSpacesIsCutAtMaxLength() {
        List<String> parts = MessageSplitter.split("a".repeat(8500));

        assertEquals(List.of(4000, 4000, 500), parts.stream().map(String::length).toList());
    }
}
