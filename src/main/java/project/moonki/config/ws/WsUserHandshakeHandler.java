package project.moonki.config.ws;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * A WebSocket handshake handler that extracts user information from the handshake request.
 */
public class WsUserHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String userId = (String) attributes.get("userId");
        Long userPk   = (Long)   attributes.get("userPk");
        return new WsUserPrincipal(userId, userPk);
    }
}
