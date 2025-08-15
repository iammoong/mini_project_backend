package project.moonki.config.ws;

import java.security.Principal;

/**
 * Represents a WebSocket user principal with details of the authenticated user.
 *
 * This class implements the {@link Principal} interface to associate a specific user
 * with a WebSocket session. It stores the user's unique identifier (userId) and internal
 * primary key (userPk) for authentication and authorization purposes.
 *
 * Features:
 * - Provides the userId as the name via the {@code getName} method.
 * - Includes a method to retrieve the internal primary key (userPk) of the user.
 *
 * Primary Use:
 * - This class is typically used in WebSocket implementations to represent the
 *   authenticated user in the context of a WebSocket session.
 *
 * Constructor:
 * - Accepts a userId (String) and userPk (Long) to initialize the principal.
 *
 * Implements:
 * - {@link Principal}: Ensures compatibility with Java security APIs and WebSocket session handling.
 */
public class WsUserPrincipal implements Principal {
    private final String name;  // userId (문자열)
    private final Long userPk;  // 내부 PK

    public WsUserPrincipal(String userId, Long userPk) {
        this.name = userId;
        this.userPk = userPk;
    }
    @Override public String getName() { return name; }
    public Long getUserPk() { return userPk; }
}
