package project.moonki.service.auth;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Autowired
    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendAuthCodeMail(String email, String code, String username, int expireMin) throws MessagingException {
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
    }

    public void sendTempPasswordMail(String email, String tempPassword, String username) throws MessagingException {
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
    }

}
