package project.moonki.config.ws;

import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import project.moonki.domain.user.entity.MUser;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A WebSocket handshake interceptor that validates JWT tokens before allowing
 * the handshake to proceed. This class is used to authorize WebSocket connections
 * based on a token-based authentication mechanism.
 *
 * Responsibilities:
 * - Extracts the JWT token from the Authorization header or query parameters of the request.
 * - Validates the extracted token using the JwtTokenProvider.
 * - Retrieves user information from the database based on the token payload.
 * - Adds user-specific attributes to the WebSocket session attributes for further use.
 *
 * Integration:
 * - Typically, this interceptor is added to the WebSocket endpoint configuration
 *   to enforce authentication and authorization during the handshake process.
 *
 * Methods:
 * - `beforeHandshake`: Validates the JWT token, retrieves associated user information,
 *   and sets session attributes.
 * - `afterHandshake`: No-op method executed after the handshake process.
 *
 * Dependencies:
 * - `JwtTokenProvider`: Provides methods for validating tokens and extracting user identifiers.
 * - `MuserRepository`: Used to fetch user details from the database.
 *
 * Returns:
 * - `true`: If the token is valid and the associated user exists in the database.
 * - `false`: If the token is invalid or the user does not exist.
 */
@Component
@RequiredArgsConstructor
public class WsJwtHandshakeInterceptor implements HandshakeInterceptor {


    private final JwtTokenProvider jwtTokenProvider;
    private final MuserRepository muserRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        List<String> authHeaders = request.getHeaders().get("Authorization");
        String token = null;

        if (authHeaders != null && !authHeaders.isEmpty() && authHeaders.get(0).startsWith("Bearer ")) {
            token = authHeaders.get(0).substring(7);
        }
        if (token == null) {
            URI uri = request.getURI();
            String q = uri.getQuery(); // token=xxx 허용
            if (q != null && q.contains("token=")) {
                token = q.replaceFirst(".*token=", "").replaceFirst("&.*", "");
            }
        }

        if (token == null || !jwtTokenProvider.validateToken(token)) return false;

        String userId = jwtTokenProvider.getUserId(token);
        Optional<MUser> opt = muserRepository.findByUserId(userId);
        if (opt.isEmpty()) return false;

        MUser user = opt.get();
        attributes.put("userId", user.getUserId());
        attributes.put("userPk", user.getId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}

}
