package project.moonki.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import project.moonki.dto.auth.EmailAuthResponseDto;
import project.moonki.service.login.LoginService;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 공통 바디 생성 */
    private Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("timestamp", LocalDateTime.now().toString());
        res.put("status", status.value());
        res.put("code", status.name());
        res.put("error", true);
        res.put("message", message);
        return res;
    }

    // 400: 잘못된 요청(비즈니스/상태)
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class,
            HttpMessageNotReadableException.class })
    public ResponseEntity<?> handleBadRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    // 400: Bean Validation(@Valid) 바인딩 오류
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> res = body(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다.");
        res.put("fieldErrors", e.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                .collect(Collectors.toList()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    // 400: 경로/쿼리 검증 오류
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException e) {
        Map<String, Object> res = body(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다.");
        res.put("violations", e.getConstraintViolations().stream()
                .map(v -> Map.of("property", v.getPropertyPath().toString(), "message", v.getMessage()))
                .collect(Collectors.toList()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    // 401: 인증 필요(도메인 예외)
    @ExceptionHandler(LoginService.UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(LoginService.UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(HttpStatus.UNAUTHORIZED, e.getMessage()));
    }

    // 403: 권한 부족
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."));
    }

    // 404: 리소스 없음
    @ExceptionHandler({ NoSuchElementException.class, EntityNotFoundException.class })
    public ResponseEntity<?> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."));
    }

    // 405: 메서드 미지원
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(body(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."));
    }

    // 409: 무결성 제약 위반
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleConflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, "데이터 무결성 위반입니다."));
    }

    // 500: 그 외 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."));
    }
}
