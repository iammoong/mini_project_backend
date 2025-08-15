package project.moonki.controller.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.chat.ChatMessageDto;
import project.moonki.dto.chat.ChatRoomDto;
import project.moonki.dto.chat.ChatUserItemDto;
import project.moonki.repository.user.MuserRepository;
import project.moonki.service.chat.ChatService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;
    private final MuserRepository users;

    private Long me(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No principal");
        }
        String userId = principal.getName();
        return users.findByUserId(userId)
                .map(MUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found: " + userId));
    }

    @GetMapping("/users")
    public List<ChatUserItemDto> users(
            Principal principal,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit
    ) {
        Long myId = me(principal);
        var list = users.searchUsers(myId, q, limit);
        return list.stream()
                .map(u -> new ChatUserItemDto(
                        u.getId(),
                        u.getNickname(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getProfileImage() != null ? u.getProfileImage().getId() : null))
                .toList();
    }

    @PostMapping("/rooms/dm")
    public ChatRoomDto open(@RequestParam Long targetUserId, Principal principal) {
        Long myId = me(principal);
        var room = chatService.getOrCreateDmRoom(myId, targetUserId);
        return new ChatRoomDto(room.getId(), myId, room.otherOf(myId));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public PageImpl<ChatMessageDto> page(@PathVariable Long roomId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        var p = chatService.getMessages(roomId, page, size);
        var mapped = p.getContent().stream().map(m ->
                new ChatMessageDto(
                        m.getId(), m.getRoomId(), m.getSenderId(),
                        users.findById(m.getSenderId()).map(MUser::getNickname).orElse("unknown"),
                        m.getContent(), m.getCreatedAt()
                )).toList();
        return new PageImpl<>(mapped, p.getPageable(), p.getTotalElements());
    }

    @PostMapping("/rooms/{roomId}/read")
    public void read(@PathVariable Long roomId, Principal principal) {
        chatService.markRead(roomId, me(principal));
    }

    @GetMapping("/unread-count")
    public long unread(Principal principal) {
        return chatService.countUnreadForUser(me(principal));
    }
}
