package ma.enset.aiagentservice.web.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.enset.aiagentservice.service.AgentService;
import ma.enset.aiagentservice.service.dto.ChatRequestDTO;
import ma.enset.aiagentservice.service.dto.ChatResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final AgentService agentService;

    @PostMapping
    public ChatResponseDTO chat(@Valid @RequestBody ChatRequestDTO request) {
        String answer = agentService.chat(request.conversationId(), request.message());
        return new ChatResponseDTO(request.conversationId(), answer);
    }
}
