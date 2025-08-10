package project.moonki.utils;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Utility class for password-related operations.
 * Provides functionality to generate a random password with a specified length.
 * The generated password is a combination of uppercase letters, lowercase letters,
 * digits, and special characters for enhanced security.
 */
public class PasswordUtil {

    private PasswordUtil() {}

    /***
     * 랜덤 비밀번호 생성 유틸 (대문자, 소문자, 숫자, 특수문자 조합)
     *
     * @param length
     * @return
     */
    public static String generateRandomPassword(int length) {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String all = upper + lower + digits + special;
        Random rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        sb.append(upper.charAt(rnd.nextInt(upper.length())));
        sb.append(lower.charAt(rnd.nextInt(lower.length())));
        sb.append(digits.charAt(rnd.nextInt(digits.length())));
        sb.append(special.charAt(rnd.nextInt(special.length())));

        for (int i = 4; i < length; i++) {
            sb.append(all.charAt(rnd.nextInt(all.length())));
        }
        List<Character> pwdChars = sb.chars().mapToObj(e -> (char) e).collect(Collectors.toList());
        Collections.shuffle(pwdChars, rnd);
        StringBuilder result = new StringBuilder();
        pwdChars.forEach(result::append);
        return result.toString();
    }

}
