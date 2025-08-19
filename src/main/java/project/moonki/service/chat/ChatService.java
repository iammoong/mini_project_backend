package project.moonki.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.moonki.domain.chat.ChatMessage;
import project.moonki.domain.chat.ChatRead;
import project.moonki.domain.chat.ChatRoom;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.chat.ChatMessageDto;
import project.moonki.dto.chat.ChatRoomListItemDto;
import project.moonki.dto.chat.ChatUserItemDto;
import project.moonki.repository.chat.ChatMessageRepository;
import project.moonki.repository.chat.ChatReadRepository;
import project.moonki.repository.chat.ChatRoomRepository;
import project.moonki.repository.user.MuserRepository;
import project.moonki.utils.LogUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReadRepository chatReadRepository;

    /**
     * Retrieves an existing direct message (DM) chat room between two users or creates a new one
     * if it does not already exist. Ensures that the associated read rows are pre-created for
     * both users.
     *
     * @param me   the user ID of the first user (typically the current user)
     * @param other the user ID of the second user
     * @return the existing or newly created ChatRoom instance representing the DM chat room
     *         between the two users
     */
    @Transactional
    public ChatRoom getOrCreateDmRoom(Long me, Long other) {
        try {
            long u1 = Math.min(me, other), u2 = Math.max(me, other);
            Optional<ChatRoom> found = chatRoomRepository.findByUser1IdAndUser2Id(u1, u2);
            if (found.isPresent()) return found.get();

            ChatRoom created = ChatRoom.dm(me, other);
            chatRoomRepository.save(created);

            // read row 미리 생성
            ensureReadRow(created.getId(), me);
            ensureReadRow(created.getId(), other);
            return created;
        } catch (Exception e) {
            LogUtil.error(log, ChatService.class, e);
            throw e;
        }
    }

    /**
     * Ensures that a ChatRead entry exists for the given room ID and user ID.
     * If no entry exists, a new one is created with the current timestamp as the last read time.
     *
     * @param roomId the ID of the chat room
     * @param userId the ID of the user
     * @return the existing or newly created ChatRead entry
     */
    @Transactional
    public ChatRead ensureReadRow(Long roomId, Long userId) {
        return chatReadRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseGet(() -> chatReadRepository.save(
                        ChatRead.builder()
                                .roomId(roomId)
                                .userId(userId)
                                .lastReadAt(LocalDateTime.now()) // ★ 핵심: 문자열 아님
                                .build()
                ));
    }

    /**
     * Saves a new chat message in the system based on the provided room ID, sender ID, and message content.
     *
     * @param roomId the ID of the chat room where the message is being sent
     * @param senderId the ID of the user sending the message
     * @param content the content of the message to be saved
     * @return the saved ChatMessage entity
     * @throws RuntimeException if an error occurs during the save operation
     */
    @Transactional
    public ChatMessage saveMessage(Long roomId, Long senderId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!room.hasParticipant(senderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        try {
            ChatMessage msg = ChatMessage.builder()
                    .roomId(roomId)
                    .senderId(senderId)
                    .content(content)
                    .createdAt(LocalDateTime.now())
                    .build();
            return chatMessageRepository.save(msg);
        } catch (Exception e) {
            LogUtil.error(log, ChatService.class, e);
            throw e;
        }
    }

    /**
     * Retrieves a paginated list of chat messages for a specific chat room,
     * ordered by their creation timestamp in descending order.
     *
     * @param roomId the ID of the chat room whose messages are to be retrieved
     * @param page the page number of the messages to retrieve (zero-based index)
     * @param size the number of messages to retrieve per page
     * @return a paginated list of chat messages for the specified chat room
     *         ordered by their creation time in descending order
     * @throws RuntimeException if an error occurs during message retrieval
     */
    @Transactional(readOnly = true)
    public Page<ChatMessage> getMessages(Long roomId, int page, int size) {
        try {
            return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(page, size));
        } catch (Exception e) {
            LogUtil.error(log, ChatService.class, e);
            throw e;
        }
    }

    /**
     * Updates the last read timestamp for a user in a specific chat room.
     * If no existing record is found for the given user and room, it creates a new entry with a default timestamp.
     *
     * @param roomId the ID of the chat room
     * @param userId the ID of the user
     */
    @Transactional
    public void markRead(Long roomId, Long userId) {
        try {
            ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!room.hasParticipant(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            ChatRead r = chatReadRepository.findByRoomIdAndUserId(roomId, userId)
                    .orElse(ChatRead.builder().roomId(roomId).userId(userId).lastReadAt(LocalDateTime.MIN).build());
            r.setLastReadAt(LocalDateTime.now());
            chatReadRepository.save(r);
        } catch (Exception e) {
            LogUtil.error(log, ChatService.class, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Page<ChatMessage> getMessagesForUser(Long roomId, Long userId, int page, int size) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!room.hasParticipant(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(page, size));
    }

    /**
     * Counts the total number of unread messages for a user across all chat rooms they participate in.
     * This method calculates the count by iterating through all chat rooms the user is a participant of and
     * comparing the timestamp of the user's last read message in the room to the timestamp of new messages in the room.
     *
     * @param userId the ID of the user for whom unread messages are being counted
     * @return the total count of unread messages for the specified user
     */
    @Transactional(readOnly = true)
    public long countUnreadForUser(Long userId) {
        try {
            // 간단 구현: 사용자가 참여한 방의 lastReadAt 이후 메시지수 합계
            // 실제 서비스에서는 쿼리 최적화/집계 테이블 사용 고려
            // (여기서는 실용적 단순화)
            // rooms 전체 순회 -> 각 room에 대해 reads와 메시지수 계산
            return chatRoomRepository.findAll().stream()
                    .filter(r -> r.hasParticipant(userId))
                    .mapToLong(r -> {
                        LocalDateTime lastRead = chatReadRepository.findByRoomIdAndUserId(r.getId(), userId)
                                .map(ChatRead::getLastReadAt).orElse(LocalDateTime.MIN);
                        long total = chatMessageRepository.countByRoomIdAndCreatedAtAfter(r.getId(), lastRead);
                        long mine  = chatMessageRepository.countByRoomIdAndSenderIdAndCreatedAtAfter(r.getId(), userId, lastRead);
                        return total - mine; // ★ 내 메시지는 제외
                    }).sum();
        } catch (Exception e) {
            LogUtil.error(log, ChatService.class, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public long countUnreadFrom(Long roomId, Long receiverId, Long senderId) {
        try {
            LocalDateTime lastRead = chatReadRepository.findByRoomIdAndUserId(roomId, receiverId)
                    .map(ChatRead::getLastReadAt).orElse(LocalDateTime.MIN);
            return chatMessageRepository.countByRoomIdAndSenderIdAndCreatedAtAfter(roomId, senderId, lastRead);
        } catch (Exception e) {
            LogUtil.error(log, ChatService.class, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countUnreadByOther(Long userId) {
        return chatRoomRepository.findAll().stream()
                .filter(r -> r.hasParticipant(userId))
                .collect(Collectors.toMap(
                        r -> r.otherOf(userId),
                        r -> {
                            LocalDateTime last = chatReadRepository.findByRoomIdAndUserId(r.getId(), userId)
                                    .map(ChatRead::getLastReadAt).orElse(LocalDateTime.MIN);
                            Long other = r.otherOf(userId);
                            return chatMessageRepository.countByRoomIdAndSenderIdAndCreatedAtAfter(r.getId(), other, last);
                        },
                        Long::sum // 동일 상대가 여러 방인 경우(현재는 DM만이라 사실상 1개)
                ));
    }

}
