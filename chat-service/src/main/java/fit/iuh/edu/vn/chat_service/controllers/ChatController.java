package fit.iuh.edu.vn.chat_service.controllers;

import fit.iuh.edu.vn.chat_service.dto.ChatMessageDTO;
import fit.iuh.edu.vn.chat_service.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDTO messageDTO) {
        ChatMessageDTO savedMessage = chatService.saveMessage(messageDTO);
        messagingTemplate.convertAndSend("/topic/chat/" + messageDTO.getUserId(), savedMessage);
    }

    @GetMapping("/history")
    public List<ChatMessageDTO> getChatHistory(@RequestParam String userId) {
        return chatService.getChatHistory(userId);
    }
}