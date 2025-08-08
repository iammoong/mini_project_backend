package project.moonki.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import project.moonki.domain.user.entity.MUser;

import java.util.Optional;

public interface MuserRepository extends JpaRepository<MUser, Long> {
    Optional <MUser> findByUserId(String userId);
    Optional <MUser> findByKakaoId(String kakaoId);
    boolean existsByUserId(String userId);
    boolean existsByNickname(String nickname);

    // 이메일(아이디)로 MUser 찾기
    Optional<MUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
