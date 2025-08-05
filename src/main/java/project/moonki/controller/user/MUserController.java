package project.moonki.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.moonki.domain.user.entity.MUser;
import project.moonki.dto.login.MUserDetails;
import project.moonki.dto.muser.UserResponseDto;
import project.moonki.mapper.MUserMapper;
import project.moonki.repository.MuserRepository;

@RestController
@RequestMapping("/auth")
public class MUserController {

    @Autowired
    private MuserRepository muserRepository;

    @GetMapping("/{id}")
    public ResponseEntity<MUser> getUser(@PathVariable Long id) {
        return muserRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

//    @GetMapping("/me")
//    public ResponseEntity<UserResponseDto> getMe(Authentication authentication) {
//        if (authentication == null || !authentication.isAuthenticated()) {
//            return ResponseEntity.status(401).build();
//        }
//        // 예시: principal이 MUserDetails 타입인 경우
//        MUserDetails principal = (MUserDetails) authentication.getPrincipal();
//        return ResponseEntity.ok(MUserMapper.toResponse(principal.getUser()));
//    }
}
