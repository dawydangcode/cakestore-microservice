package fit.iuh.edu.vn.chat_service.controllers;

import fit.iuh.edu.vn.chat_service.models.ChatMessage;
import fit.iuh.edu.vn.chat_service.models.ErrorResponse;
import fit.iuh.edu.vn.chat_service.repositories.ChatMessageRepository;
import fit.iuh.edu.vn.chat_service.services.RateLimiterService;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RateLimiterService rateLimiterService;

    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public ChatMessage sendMessage(ChatMessage message) {
        // Kiểm tra rate limit
        boolean isAllowed = rateLimiterService.isAllowed(message.getUserName(), 5, 5);
        if (!isAllowed) {
            messagingTemplate.convertAndSendToUser(
                    message.getUserName(),
                    "/queue/errors",
                    new ErrorResponse("RATE_LIMIT_EXCEEDED", "Bạn đã gửi quá 5 tin nhắn trong 5 giây. Vui lòng thử lại sau.")
            );
            throw new RuntimeException("Bạn đã gửi quá 5 tin nhắn trong 5 giây. Vui lòng thử lại sau.");
        }

        try {
            message.setCreatedAt(LocalDateTime.now());
            chatMessageRepository.save(message);
            return message;
        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(
                    message.getUserName(),
                    "/queue/errors",
                    new ErrorResponse("MESSAGE_SEND_FAILED", "Lỗi khi gửi tin nhắn: " + e.getMessage())
            );
            throw new RuntimeException("Lỗi khi gửi tin nhắn: " + e.getMessage());
        }
    }

    @GetMapping("/chat/history")
    @Retry(name = "chatHistoryApi", fallbackMethod = "retryFallback")
    public List<ChatMessage> getChatHistory(@RequestParam String userName) {
        return chatMessageRepository.findByUserNameOrderByCreatedAtAsc(userName);
    }

    public ResponseEntity<?> retryFallback(String userName, Throwable t) {
        String errorMessage = String.format("Không thể tải lịch sử chat cho người dùng %s. Lỗi: %s. Vui lòng thử lại sau.",
                userName, t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("SERVICE_UNAVAILABLE", errorMessage));
    }

    @GetMapping("/chat/users")
    public List<String> getChatUsers() {
        return chatMessageRepository.findDistinctUserNames();
    }
}