package project.moonki.config.ws;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * A custom WebSocket handshake handler that assigns a {@link Principal} to the WebSocket session
 * based on attributes provided during the handshake process.
 *
 * This class extends {@link DefaultHandshakeHandler} and overrides the {@code determineUser} method
 * to extract user-specific information from the WebSocket session attributes and create a {@link WsUserPrincipal}.
 *
 * Responsibilities:
 * - Reads user-specific attributes from the handshake attributes map.
 * - Constructs a {@link WsUserPrincipal} using the extracted information.
 * - Sets the {@link Principal} to the WebSocket session for further use in message handling.
 *
 * Integration:
 * - Typically used in conjunction with a {@link WsJwtHandshakeInterceptor} or similar component
 *   to enforce authentication and authorization during the handshake process.
 * - The {@link WebSocketConfig} includes this custom handshake handler in the endpoint registration.
 *
 * Key Methods:
 * - {@code determineUser}: Extracts the userId and userPk from handshake attributes and creates a user-specific {@link Principal}.
 *
 * Expected Attributes:
 * - "userId": A unique identifier representing the user (String).
 * - "userPk": The internal primary key representing the user (Long).
 *
 * Returns:
 * - A {@link WsUserPrincipal} containing the userId and userPk extracted from the handshake attributes.
 *
 * Extends:
 * - {@link DefaultHandshakeHandler}: Provides the base implementation of the WebSocket handshake process.
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
