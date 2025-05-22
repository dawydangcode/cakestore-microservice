package fit.iuh.edu.vn.chat_service.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public boolean isAllowed(String userName, int maxMessages, long windowSeconds) {
        String key = RATE_LIMIT_KEY_PREFIX + userName;
        Long count = redisTemplate.opsForValue().increment(key, 1);

        if (count == 1) {
            // Lần đầu tiên, đặt thời gian hết hạn
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        return count <= maxMessages;
    }
}