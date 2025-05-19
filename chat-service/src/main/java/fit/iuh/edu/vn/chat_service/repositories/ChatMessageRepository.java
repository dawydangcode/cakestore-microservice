package fit.iuh.edu.vn.chat_service.repositories;

import fit.iuh.edu.vn.chat_service.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserNameOrderByCreatedAtAsc(String userName);

    @Query("SELECT DISTINCT cm.userName FROM ChatMessage cm")
    List<String> findDistinctUserNames();
}