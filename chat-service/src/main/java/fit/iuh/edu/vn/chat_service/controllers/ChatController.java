package fit.iuh.edu.vn.chat_service.controllers;

import fit.iuh.edu.vn.chat_service.models.ChatMessage;
import fit.iuh.edu.vn.chat_service.repositories.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public ChatMessage sendMessage(ChatMessage message) {
        message.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(message);
        return message;
    }

    @GetMapping("/chat/history")
    public List<ChatMessage> getChatHistory(@RequestParam String userName) {
        return chatMessageRepository.findByUserNameOrderByCreatedAtAsc(userName);
    }

    @GetMapping("/chat/users")
    public List<String> getChatUsers() {
        return chatMessageRepository.findDistinctUserNames();
    }
}