package fit.iuh.edu.vn.category_service.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotEnvConfig {

    @PostConstruct
    public void loadDotEnv() {
        // Tải file .env từ thư mục gốc của dự án
        Dotenv dotenv = Dotenv.configure()
                .directory("./.env") // Đường dẫn đến thư mục chứa file .env
                .ignoreIfMissing() // Bỏ qua nếu không tìm thấy file .env
                .load();

        // Đặt các biến từ file .env vào môi trường hệ thống
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
    }
}
