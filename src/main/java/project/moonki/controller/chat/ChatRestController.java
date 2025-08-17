package project.moonki.controller.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.moonki.dto.chat.*;
import project.moonki.dto.login.MUserDetailsDto;
import project.moonki.service.chat.ChatQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {
    private final ChatQueryService chatQuery;

    /** 사용자 목록: 미읽음 있는 상대 우선 정렬 */
    @GetMapping("/users")
    public List<ChatUserItemDto> users(
            @AuthenticationPrincipal MUserDetailsDto principal,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        Long myId = principal.getUser().getId();
        return chatQuery.listUsersWithUnreadFirst(myId, q, limit);
    }

    /** DM 방 개설(존재 시 재사용) */
    @PostMapping("/rooms/dm")
    public ChatRoomDto openRoom(
            @AuthenticationPrincipal MUserDetailsDto principal,
            @RequestParam("userId") Long otherUserId
    ) {
        Long myId = principal.getUser().getId();
        return chatQuery.openDm(myId, otherUserId);
    }

    /** 방 메시지 페이지 조회 */
    @GetMapping("/rooms/{roomId}/messages")
    public PageImpl<ChatMessageDto> getMessages(
            @AuthenticationPrincipal MUserDetailsDto principal,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Long myId = principal.getUser().getId();
        return chatQuery.getRoomMessages(myId, roomId, page, size);
    }

    /** 방 읽음 처리 */
    @PostMapping("/rooms/{roomId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal MUserDetailsDto principal,
            @PathVariable Long roomId
    ) {
        Long myId = principal.getUser().getId();
        chatQuery.markRead(myId, roomId);
    }

    /** 전체 미읽음 수 */
    @GetMapping("/unread/count")
    public long unread(@AuthenticationPrincipal MUserDetailsDto principal) {
        return chatQuery.countUnread(principal.getUser().getId());
    }

    /** 발신자별 미읽음 수 */
    @GetMapping("/unread/by-sender")
    public List<UnreadBySenderDto> unreadBySender(@AuthenticationPrincipal MUserDetailsDto principal) {
        return chatQuery.unreadBySender(principal.getUser().getId());
    }

    @GetMapping("/rooms/my")
    public List<ChatRoomListItemDto> myRooms(
            @AuthenticationPrincipal MUserDetailsDto principal,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return chatQuery.myRooms(principal.getUser().getId(), limit);
    }
}
