package fit.iuh.edu.vn.chat_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private Long id;
    private String userId;
    private String message;
    private String senderType;
    private LocalDateTime createdAt;
}