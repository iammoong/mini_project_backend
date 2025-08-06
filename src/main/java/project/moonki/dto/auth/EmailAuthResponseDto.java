package project.moonki.dto.auth;

import lombok.Data;

@Data
public class EmailAuthResponseDto {
    private boolean success;
    private String message;
}
