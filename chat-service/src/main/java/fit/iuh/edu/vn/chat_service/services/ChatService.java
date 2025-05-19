package fit.iuh.edu.vn.chat_service.services;

import fit.iuh.edu.vn.chat_service.dto.ChatMessageDTO;
import fit.iuh.edu.vn.chat_service.models.ChatMessage;
import fit.iuh.edu.vn.chat_service.repositories.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatMessageDTO saveMessage(ChatMessageDTO messageDTO) {
        ChatMessage message = new ChatMessage();
        message.setUserName(messageDTO.getUserName());
        message.setMessage(messageDTO.getMessage());
        message.setSenderType(ChatMessage.SenderType.valueOf(messageDTO.getSenderType()));
        message.setCreatedAt(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(message);

        return convertToDTO(savedMessage);
    }

    public List<ChatMessageDTO> getChatHistory(String userName) {
        return chatMessageRepository.findByUserNameOrderByCreatedAtAsc(userName)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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