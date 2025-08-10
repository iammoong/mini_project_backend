package project.moonki.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /***
     * 아이디 찾기 - 이메일로 인증코드 발송 로직
     *
     * @param email
     * @param code
     * @param username
     * @param expireMin
     * @throws MessagingException
     */
    public void sendAuthCodeMail(String email, String code, String username, int expireMin) throws MessagingException {
        try {
            Context context = new Context();
            context.setVariable("title", "[아이디찾기] 인증번호 안내");
            context.setVariable("code", code);
            context.setVariable("username", username);
            context.setVariable("expireMin", expireMin);

            String html = templateEngine.process("mail/authCode", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(email);
            helper.setSubject("[아이디찾기] 인증번호 안내");
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("인증번호 메일 발송 실패 - email={}, username={}", email, username, e);
            throw new RuntimeException("인증번호 메일 발송 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("인증번호 메일 발송 중 예기치 못한 예외 - email={}, username={}", email, username, e);
            throw new RuntimeException("메일 발송 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 비밀번호 찾기 - 이메일로 임시 비밀번호 발송
     *
     * @param email
     * @param tempPassword
     * @param username
     * @throws MessagingException
     */
    public void sendTempPasswordMail(String email, String tempPassword, String username) throws MessagingException {
        try {
            Context context = new Context();
            context.setVariable("title", "[비밀번호 찾기] 임시 비밀번호 발급");
            context.setVariable("password", tempPassword);
            context.setVariable("username", username);

            String html = templateEngine.process("mail/tempPassword", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(email);
            helper.setSubject("[비밀번호 찾기] 임시 비밀번호 발급");
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("임시 비밀번호 메일 발송 실패 - email={}, username={}", email, username, e);
            throw new RuntimeException("임시 비밀번호 메일 발송 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("임시 비밀번호 메일 발송 중 예기치 못한 예외 - email={}, username={}", email, username, e);
            throw new RuntimeException("메일 발송 처리 중 오류가 발생했습니다.");
        }
    }

}
