package project.moonki.mapper;

import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.muser.UserResponseDto;

public class MUserMapper {

    /***
     * 엔티티 -> DTO 변환 (공통화)
     *
     * @param user
     * @return
     */
    public static UserResponseDto toResponse(MUser user) {
        if (user == null) return null;
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUserId(user.getUserId());
        dto.setKakaoId(user.getKakaoId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
