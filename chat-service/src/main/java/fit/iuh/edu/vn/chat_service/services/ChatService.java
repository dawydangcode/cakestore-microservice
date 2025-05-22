package fit.iuh.edu.vn.chat_service.services;

import fit.iuh.edu.vn.chat_service.dto.ChatMessageDTO;
import fit.iuh.edu.vn.chat_service.models.ChatMessage;
import fit.iuh.edu.vn.chat_service.repositories.ChatMessageRepository;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String MESSAGE_CACHE_KEY_PREFIX = "chat_message:";
    private static final String USER_MESSAGES_KEY_PREFIX = "user_messages:";

    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, ChatMessageDTO> redisTemplate;

    @Autowired
    public ChatService(ChatMessageRepository chatMessageRepository, RedisTemplate<String, ChatMessageDTO> redisTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.redisTemplate = redisTemplate;
    }

    // Create: Lưu tin nhắn vào DB và Redis
    public ChatMessageDTO saveMessage(ChatMessageDTO messageDTO) {
        // Lưu vào DB
        ChatMessage message = new ChatMessage();
        message.setUserName(messageDTO.getUserName());
        message.setMessage(messageDTO.getMessage());
        message.setSenderType(ChatMessage.SenderType.valueOf(messageDTO.getSenderType()));
        message.setCreatedAt(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDTO savedDTO = convertToDTO(savedMessage);

        // Lưu vào Redis
        String messageKey = MESSAGE_CACHE_KEY_PREFIX + savedDTO.getId();
        redisTemplate.opsForValue().set(messageKey, savedDTO, 1, TimeUnit.HOURS); // Cache 1 giờ

        // Thêm vào danh sách tin nhắn của user trong Redis
        String userMessagesKey = USER_MESSAGES_KEY_PREFIX + savedDTO.getUserName();
        redisTemplate.opsForList().rightPush(userMessagesKey, savedDTO);
        redisTemplate.expire(userMessagesKey, 1, TimeUnit.HOURS); // Expire sau 1 giờ

        return savedDTO;
    }

    // Read: Lấy lịch sử tin nhắn từ Redis hoặc DB
    @Retry(name = "chatHistoryApi", fallbackMethod = "getChatHistoryFallback")
    public List<ChatMessageDTO> getChatHistory(String userName) {
        String userMessagesKey = USER_MESSAGES_KEY_PREFIX + userName;

        // Thử lấy từ Redis
        List<ChatMessageDTO> cachedMessages = redisTemplate.opsForList().range(userMessagesKey, 0, -1);
        if (cachedMessages != null && !cachedMessages.isEmpty()) {
            return cachedMessages;
        }

        // Nếu không có trong Redis, lấy từ DB và lưu vào Redis
        List<ChatMessageDTO> messages = chatMessageRepository.findByUserNameOrderByCreatedAtAsc(userName)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Lưu vào Redis
        if (!messages.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(userMessagesKey, messages);
            redisTemplate.expire(userMessagesKey, 1, TimeUnit.HOURS);
        }

        return messages;
    }

    // Update: Cập nhật tin nhắn
    public ChatMessageDTO updateMessage(Long messageId, ChatMessageDTO updatedDTO) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Tin nhắn không tồn tại"));

        message.setMessage(updatedDTO.getMessage());
        message.setSenderType(ChatMessage.SenderType.valueOf(updatedDTO.getSenderType()));
        message.setCreatedAt(LocalDateTime.now());

        ChatMessage updatedMessage = chatMessageRepository.save(message);
        ChatMessageDTO updatedMessageDTO = convertToDTO(updatedMessage);

        // Cập nhật Redis
        String messageKey = MESSAGE_CACHE_KEY_PREFIX + messageId;
        redisTemplate.opsForValue().set(messageKey, updatedMessageDTO, 1, TimeUnit.HOURS);

        // Cập nhật danh sách tin nhắn của user
        String userMessagesKey = USER_MESSAGES_KEY_PREFIX + updatedDTO.getUserName();
        List<ChatMessageDTO> userMessages = redisTemplate.opsForList().range(userMessagesKey, 0, -1);
        if (userMessages != null) {
            for (int i = 0; i < userMessages.size(); i++) {
                if (userMessages.get(i).getId().equals(messageId)) {
                    redisTemplate.opsForList().set(userMessagesKey, i, updatedMessageDTO);
                    break;
                }
            }
        }

        return updatedMessageDTO;
    }

    // Delete: Xóa tin nhắn
    public void deleteMessage(Long messageId, String userName) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Tin nhắn không tồn tại"));

        chatMessageRepository.delete(message);

        // Xóa khỏi Redis
        String messageKey = MESSAGE_CACHE_KEY_PREFIX + messageId;
        redisTemplate.delete(messageKey);

        // Xóa khỏi danh sách tin nhắn của user
        String userMessagesKey = USER_MESSAGES_KEY_PREFIX + userName;
        List<ChatMessageDTO> userMessages = redisTemplate.opsForList().range(userMessagesKey, 0, -1);
        if (userMessages != null) {
            for (int i = 0; i < userMessages.size(); i++) {
                if (userMessages.get(i).getId().equals(messageId)) {
                    redisTemplate.opsForList().remove(userMessagesKey, 1, userMessages.get(i));
                    break;
                }
            }
        }
    }

    public List<ChatMessageDTO> getChatHistoryFallback(String userName, Throwable t) {
        return List.of();
    }

    private ChatMessageDTO convertToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setUserName(message.getUserName());
        dto.setMessage(message.getMessage());
        dto.setSenderType(message.getSenderType().name());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}