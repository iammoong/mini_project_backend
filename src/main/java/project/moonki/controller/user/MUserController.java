package project.moonki.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.moonki.domain.user.entity.MUser;
import project.moonki.repository.user.MuserRepository;
import project.moonki.security.JwtTokenProvider;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class MUserController {

    @Autowired
    private MuserRepository muserRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/{id}")
    public ResponseEntity<MUser> getUser(@PathVariable Long id) {
        return muserRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
