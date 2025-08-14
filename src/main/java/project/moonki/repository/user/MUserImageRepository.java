package project.moonki.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import project.moonki.domain.user.entity.MUserImage;

public interface MUserImageRepository extends JpaRepository <MUserImage, Long>{
}
