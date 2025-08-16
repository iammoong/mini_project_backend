package project.moonki.controller.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import project.moonki.config.ws.WsUserPrincipal;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.chat.ChatMessageDto;
import project.moonki.dto.chat.ChatSendRequestDto;
import project.moonki.dto.chat.UnreadEventDto;
import project.moonki.repository.chat.ChatRoomRepository;
import project.moonki.repository.user.MuserRepository;
import project.moonki.service.chat.ChatService;

import java.security.Principal;

/**
 * Controller to handle WebSocket communication for the chat feature.
 * This controller listens for messages sent to the defined message mappings
 * and processes these messages accordingly.
 *
 * It is responsible for handling incoming chat messages, saving them,
 * and broadcasting them to the corresponding chat room topics.
 *
 * Dependencies:
 * - ChatService: Provides business logic for managing chat messages.
 * - MuserRepository: Repository for accessing user information from the database.
 * - SimpMessagingTemplate: Facilitates message broadcasting to WebSocket destinations.
 */
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;
    private final MuserRepository users;
    private final SimpMessagingTemplate broker;
    private final ChatRoomRepository rooms;

    @MessageMapping("/chat.send.{roomId}")
    public void send(@DestinationVariable Long roomId, ChatSendRequestDto req, Principal principal) {
        WsUserPrincipal p = (WsUserPrincipal) principal;

        var saved = chatService.saveMessage(roomId, p.getUserPk(), req.content());
        String nickname = users.findById(p.getUserPk()).map(MUser::getNickname).orElse("unknown");

        ChatMessageDto payload = new ChatMessageDto(
                saved.getId(), saved.getRoomId(), saved.getSenderId(),
                nickname, saved.getContent(), saved.getCreatedAt()
        );

        broker.convertAndSend("/topic/chat." + roomId, payload);

        var room = rooms.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Long receiver = room.otherOf(p.getUserPk());

        long total = chatService.countUnreadForUser(receiver);
        long bySender = chatService.countUnreadFrom(roomId, receiver, p.getUserPk());

        UnreadEventDto evt = new UnreadEventDto("UNREAD", total, roomId, p.getUserPk(), bySender);
        broker.convertAndSend("/topic/notify." + receiver, evt);
    }
}
