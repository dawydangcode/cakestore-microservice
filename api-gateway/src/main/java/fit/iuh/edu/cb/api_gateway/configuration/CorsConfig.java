package fit.iuh.edu.cb.api_gateway.configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class CorsConfig {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    @Bean
    public GlobalFilter corsFilter() {
        return new CorsGlobalFilter();
    }

    static class CorsGlobalFilter implements GlobalFilter, Ordered {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            logger.debug("Processing request: {} {}", request.getMethod(), request.getURI());

            // Xóa các header CORS cũ từ backend
            response.getHeaders().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            response.getHeaders().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
            response.getHeaders().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
            response.getHeaders().remove(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
            response.getHeaders().remove(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);

            // Xử lý yêu cầu preflight (OPTIONS)
            if (request.getMethod() == HttpMethod.OPTIONS) {
                logger.debug("Handling OPTIONS preflight request for {}", request.getURI());
                response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
                response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
                response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
                response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
                response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                response.setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }

            // Thêm header CORS cho các yêu cầu khác
            logger.debug("Adding CORS headers to response for {}", request.getURI());
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

            return chain.filter(exchange);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}