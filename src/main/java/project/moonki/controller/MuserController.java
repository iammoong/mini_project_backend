package project.moonki.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.moonki.domain.user.entity.MUser;
import project.moonki.repository.MuserRepository;

@RestController
@RequestMapping("/api/user")
public class MuserController {

    @Autowired
    private MuserRepository muserRepository;

    @GetMapping("/{id}")
    public ResponseEntity<MUser> getUser(@PathVariable Long id) {
        return muserRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
