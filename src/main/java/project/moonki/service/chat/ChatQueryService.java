package project.moonki.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.moonki.domain.chat.ChatMessage;
import project.moonki.domain.chat.ChatRoom;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.chat.ChatMessageDto;
import project.moonki.dto.chat.ChatRoomDto;
import project.moonki.dto.chat.ChatUserItemDto;
import project.moonki.dto.chat.UnreadBySenderDto;
import project.moonki.repository.chat.ChatRoomRepository;
import project.moonki.repository.user.MuserRepository;
import project.moonki.utils.LogUtil;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional( readOnly = true)
public class ChatQueryService {

    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;
    private final MuserRepository muserRepository;

    /** 사용자 검색(내 계정 제외) + 미읽음 보낸 사람 우선 정렬 */
    public List<ChatUserItemDto> listUsersWithUnreadFirst(Long myId, String q, int limit) {
        try {
            // 기본 검색
            List<MUser> base = muserRepository.searchUsers(myId, q, limit);

            // 미읽음 발신자 수 집계
            Map<Long, Long> unreadMap = chatService.countUnreadByOther(myId); // userId -> count

            // 미읽음이 있는 사용자 우선, 그 다음 이름(닉네임/이름 coalesce ASC) 정렬
            return base.stream()
                    .sorted(Comparator
                            .comparing((MUser u) -> unreadMap.getOrDefault(u.getId(), 0L) == 0L) // false(미읽음 有) 먼저
                            .thenComparing(u -> Optional.ofNullable(u.getNickname()).orElse(u.getUsername()), String.CASE_INSENSITIVE_ORDER))
                    .map(this::toUserItem)
                    .collect(Collectors.toList());
        }catch (ResponseStatusException e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw e;
        } catch (Exception e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /** DM 방 개설(존재 시 재사용) */
    @Transactional
    public ChatRoomDto openDm(Long meId, Long otherId) {
        try {
            if (Objects.equals(meId, otherId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인과의 대화방은 생성할 수 없습니다.");
            }
            ChatRoom room = chatService.getOrCreateDmRoom(meId, otherId);
            return new ChatRoomDto(room.getId(), meId, room.otherOf(meId));
        } catch (ResponseStatusException e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw e;
        } catch (Exception e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DM 방 생성 중 오류가 발생했습니다.", e);
        }
    }

    /** 방 메시지 조회(참여자 검증 + DTO 매핑) */
    public PageImpl<ChatMessageDto> getRoomMessages(Long myId, Long roomId, int page, int size) {
        try {
            assertParticipant(roomId, myId);

            Page<ChatMessage> slice = chatService.getMessages(roomId, page, size);

            // 발신자 일괄 조회 후 매핑
            Set<Long> senderIds = slice.getContent().stream().map(ChatMessage::getSenderId).collect(Collectors.toSet());
            Map<Long, MUser> senderMap = muserRepository.findAllById(senderIds).stream()
                    .collect(Collectors.toMap(MUser::getId, u -> u));

            List<ChatMessageDto> content = slice.getContent().stream()
                    .map(m -> new ChatMessageDto(
                            m.getId(),
                            m.getRoomId(),
                            m.getSenderId(),
                            Optional.ofNullable(senderMap.get(m.getSenderId()))
                                    .map(MUser::getNickname).orElse(null),
                            m.getContent(),
                            m.getCreatedAt()))
                    .toList();

            return new PageImpl<>(content, PageRequest.of(page, size), slice.getTotalElements());
        } catch (ResponseStatusException e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw e;
        } catch (Exception e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "메시지 조회 중 오류가 발생했습니다.", e);
        }

    }

    /** 읽음 처리 */
    @Transactional
    public void markRead(Long myId, Long roomId) {
        try {
            chatService.markRead(roomId, myId);
        } catch (ResponseStatusException e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw e;
        } catch (Exception e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "읽음 처리 중 오류가 발생했습니다.", e);
        }
    }

    /** 전체 미읽음 개수 */
    public long countUnread(Long myId) {
        try {
            return chatService.countUnreadForUser(myId);
        } catch (ResponseStatusException e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw e;
        } catch (Exception e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "미읽음 카운트 조회 중 오류가 발생했습니다.", e);
        }
    }

    /** 발신자별 미읽음 집계 → DTO 리스트 */
    public List<UnreadBySenderDto> unreadBySender(Long myId) {
        try {
            return chatService.countUnreadByOther(myId).entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue() > 0)
                    .map(e -> new UnreadBySenderDto(e.getKey(), e.getValue()))
                    .toList();
        } catch (ResponseStatusException e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw e;
        } catch (Exception e) {
            LogUtil.error(log, ChatQueryService.class, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "발신자별 미읽음 집계 중 오류가 발생했습니다.", e);
        }

    }

    private void assertParticipant(Long roomId, Long userId) {
        try {
            ChatRoom room = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!room.hasParticipant(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        } catch (Exception e) {
            LogUtil.error(log, ChatQueryService.class, e);
        }
    }

    private ChatUserItemDto toUserItem(MUser u) {
        return new ChatUserItemDto(u.getId(), u.getNickname(), u.getUsername(), u.getEmail(), u.getProfileImage() != null ? u.getProfileImage().getId() : null);
    }
}
