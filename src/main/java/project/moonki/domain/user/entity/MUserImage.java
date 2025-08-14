package project.moonki.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "m_user_image")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MUserImage {

    @Id @GeneratedValue
    private Long id;

    // 원본 파일명
    @Column(nullable = false, length = 200)
    private String originalFilename;

    // MIME 타입: image/jpeg, image/png, image/webp 등
    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(length = 64)
    private String sha256;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
