package fit.iuh.edu.cb.api_gateway.configuration;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class WebSocketGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            if (exchange.getRequest().getPath().toString().startsWith("/ws/")) {
                exchange.getResponse().getHeaders().add("X-WebSocket", "true");
            }
            return chain.filter(exchange);
        };
    }
}
