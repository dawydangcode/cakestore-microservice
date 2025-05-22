package fit.iuh.edu.vn.chat_service.config;

import fit.iuh.edu.vn.chat_service.dto.ChatMessageDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ChatMessageDTO> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ChatMessageDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Sử dụng Jackson2JsonRedisSerializer để serialize/deserialize ChatMessageDTO
        Jackson2JsonRedisSerializer<ChatMessageDTO> serializer = new Jackson2JsonRedisSerializer<>(ChatMessageDTO.class);
        template.setValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}