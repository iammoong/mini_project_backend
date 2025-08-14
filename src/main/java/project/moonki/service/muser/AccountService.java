package project.moonki.service.muser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import project.moonki.domain.user.entity.MUser;
import project.moonki.domain.user.entity.MUserImage;
import project.moonki.dto.login.LoginResponseDto;
import project.moonki.dto.login.MUserDetailsDto;
import project.moonki.dto.muser.ChangePasswordRequestDto;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.dto.muser.UserUpdateRequestDto;
import project.moonki.mapper.MUserMapper;
import project.moonki.repository.user.MUserImageRepository;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;
import project.moonki.service.login.LoginService;
import project.moonki.utils.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional( readOnly = true)
public class AccountService {

    private final MuserRepository muserRepository;
    private final MUserImageRepository imageRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto getMe(Authentication authentication) {
        try {
            MUser user = resolveUser(authentication);
            return MUserMapper.toResponse(user);
        } catch (Exception e) {
            LogUtil.error(log, AccountService.class, e);
            throw e;
        }
    }

    /***
     * 사용자 정보 변경
     *
     * @param authentication
     * @param req
     * @return
     */
    @Transactional
    public LoginResponseDto updateMe(Authentication authentication, UserUpdateRequestDto req) {
        try {
            MUser user = resolveUser(authentication);

            if (user.getKakaoId() != null) {
                throw new IllegalArgumentException("카카오 로그인 계정은 정보를 수정할 수 없습니다.");
            }

            // 아이디 변경


            boolean needReauth = false;

            if (req.getUserId() != null && !req.getUserId().isBlank()
                    && !req.getUserId().equals(user.getUserId())) {
                if (muserRepository.existsByUserId(req.getUserId())) {
                    throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
                }
                user.setUserId(req.getUserId());
                needReauth = true;
            }

            // 닉네임 변경
            if (req.getNickname() != null && !req.getNickname().equals(user.getNickname())) {
                if (muserRepository.existsByNickname(req.getNickname())) {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                }
                user.setNickname(req.getNickname());
            }

            if (req.getUsername() != null) user.setUsername(req.getUsername());
            if (req.getEmail() != null)    user.setEmail(req.getEmail());
            if (req.getPhone() != null)    user.setPhone(req.getPhone());

            MUser saved = muserRepository.save(user);

            // 아이디 변경 시 새 토큰 발급
            String token = null;
            if (needReauth) {
                token = jwtTokenProvider.generateToken(saved.getUserId());
            }

            return new LoginResponseDto(MUserMapper.toResponse(saved), token);

        } catch (Exception e) {
            LogUtil.error(log, AccountService.class, e);
            throw e;
        }
    }

    /***
     * 비밀번호 변경
     *
     * @param authentication
     * @param req
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Authentication authentication, ChangePasswordRequestDto req) {
        try {
            // 인증 검증
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                throw new LoginService.UnauthorizedException("인증이 필요합니다.");
            }

            Object p = authentication.getPrincipal();
            if (!(p instanceof MUserDetailsDto principal)) {
                log.warn("[changePassword] Unexpected principal type: {}", p.getClass());
                throw new LoginService.UnauthorizedException("인증이 필요합니다.");
            }

            //  principal에 든 user는 영속 객체가 아닐 수 있어 id로 재조회(영속화)
            Long userPk = principal.getUser().getId();
            MUser user = muserRepository.findById(userPk)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            // 현재 비밀번호 검증
            if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
                log.warn("[changePassword] Current password mismatch: userId={}", user.getUserId());
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }

            // 동일 비밀번호 방지
            if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
                log.warn("[changePassword] New password equals old: userId={}", user.getUserId());
                throw new IllegalArgumentException("새 비밀번호가 기존과 동일합니다.");
            }

            // 비밀번호 변경
            String encoded = passwordEncoder.encode(req.newPassword());
            user.changePassword(encoded);

            // 영속 상태이므로 별도 save() 없이 커밋 시 UPDATE 수행
            log.info("[changePassword] Password changed: userId={}", user.getUserId());


        } catch (LoginService.UnauthorizedException | IllegalArgumentException e) {
            log.warn("[changePassword] Client error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LogUtil.error(log, AccountService.class, e);
            throw new RuntimeException("비밀번호 변경 처리 중 오류가 발생했습니다.");
        }
    }

    /***
     * 회원탈퇴
     *
     * @param authentication
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMe(Authentication authentication) {
        MUser me = resolveUser(authentication);
        // 하드 삭제. 연관 데이터가 생기면 soft-delete로 전환 검토
        muserRepository.delete(me);
    }

    /***
     * 프로필 이미지 업로드/교체
     *
     * @param authentication
     * @param file
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public UserResponseDto uploadProfileImage(Authentication authentication, MultipartFile file) throws Exception{
        Path savedPath = null;          // 새로 저장한 파일 경로
        String oldPathToDelete = null;  // 기존 이미지 파일 경로

        try {
            // 인증 검증
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                throw new LoginService.UnauthorizedException("인증이 필요합니다.");
            }
            Object p = authentication.getPrincipal();
            if (!(p instanceof MUserDetailsDto principal)) {
                throw new LoginService.UnauthorizedException("인증이 필요합니다.");
            }

            // 파일 검증
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("업로드할 파일이 없습니다.");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
            }
            long max = 5L * 1024 * 1024; // 5MB
            if (file.getSize() > max) {
                throw new IllegalArgumentException("이미지는 5MB 이하만 업로드할 수 있습니다.");
            }

            // 사용자 영속 조회
            Long userPk = principal.getUser().getId();
            MUser user = muserRepository.findById(userPk)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            // 파일 저장 (디렉터리 생성 → 복사)
            Path baseDir = pickWritableBaseDir();
            String ext = Optional.ofNullable(file.getOriginalFilename())
                    .filter(fn -> fn.contains("."))
                    .map(fn -> fn.substring(fn.lastIndexOf('.') + 1))
                    .orElse("dat");
            String datePath = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(LocalDate.now());
            String objectKey = UUID.randomUUID() + "." + ext;

            // 사용자명을 디렉터리 세그먼트로 쓸 때는 안전화
            String userSeg = safeSegment(principal.getUsername());

            Path saveDir = baseDir.resolve(datePath).resolve(userSeg);
            Files.createDirectories(saveDir); // 날짜/사용자별 하위 폴더도 자동 생성
            Path savePath = saveDir.resolve(objectKey);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
            }
            savedPath = savePath; // 보상 삭제를 위해 보관

            // 5) 기존 이미지가 있으면 DB 연결 해제 및 레코드 삭제
            if (user.getProfileImage() != null) {
                MUserImage old = user.getProfileImage();
                oldPathToDelete = old.getPath(); // 성공 후 파일 삭제용
                user.setProfileImage(null);      // FK 해제
                imageRepository.deleteById(old.getId());
            }

            // 6) 새 이미지 메타데이터 저장 및 사용자 연결
            MUserImage image = MUserImage.builder()
                    .originalFilename(file.getOriginalFilename())
                    .contentType(contentType)
                    .size(file.getSize())
                    .path(savePath.toString())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            imageRepository.save(image);
            user.setProfileImage(image);
            muserRepository.save(user);

            // 기존 이미지 실제 파일 삭제(성공 후 정리)
            if (oldPathToDelete != null) {
                try {
                    Files.deleteIfExists(Paths.get(oldPathToDelete));
                } catch (IOException ex) {
                    log.warn("[uploadProfileImage] 기존 파일 삭제 실패: {}", oldPathToDelete, ex);
                }
            }

            return MUserMapper.toResponse(user);

        } catch (LoginService.UnauthorizedException | IllegalArgumentException e) {
            log.warn("[uploadProfileImage] {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            // 내부 오류: 신규로 쓴 파일이 있으면 보상 삭제
           LogUtil.error(log, AccountService.class, e);
            if (savedPath != null) {
                try {
                    Files.deleteIfExists(savedPath);
                } catch (IOException ex) {
                    log.warn("[uploadProfileImage] 신규 파일 보상 삭제 실패: {}", savedPath, ex);
                }
            }
            throw (e instanceof RuntimeException)
                    ? e
                    : new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * resolveUser 내부는 그대로 두고, 상위 메서드에서 예외 로깅/처리 일원화
     *
     * @param authentication
     * @return
     */
    private MUser resolveUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("인증이 필요합니다.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof MUserDetailsDto)) {
            throw new IllegalStateException("인증 주체 유형이 올바르지 않습니다.");
        }
        return ((MUserDetailsDto) principal).getUser();
    }

    /**
     *  사용자 홈, 시스템 임시 디렉터리
     *
     * @return
     * @throws IOException
     */
    private Path pickWritableBaseDir() throws IOException {
        Path[] candidates = new Path[] {
                Paths.get("/var/app/uploads/profile"),
                Paths.get(System.getProperty("user.home"), "moonki", "uploads", "profile"),
                Paths.get(System.getProperty("java.io.tmpdir"), "moonki", "uploads", "profile")
        };

        IOException last = null;
        for (Path dir : candidates) {
            try {
                Files.createDirectories(dir);   // 없으면 생성
                if (Files.isWritable(dir)) {    // 쓰기 가능 확인
                    return dir.toAbsolutePath().normalize();
                }
            } catch (IOException e) {
                last = e; // 다음 후보로
            }
        }
        throw new IOException("업로드 디렉터리를 생성/쓰기할 수 없습니다.", last);
    }

    /**
     * 경로 세그먼트 안전화
     *
     * @param s
     * @return
     */
    private static String safeSegment(String s) {

        if (s == null || s.isBlank()) return "unknown";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}


