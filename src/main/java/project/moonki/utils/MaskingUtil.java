package project.moonki.utils;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class MaskingUtil {

    private MaskingUtil() {}

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        String[] parts = email.split("@");
        String id = parts[0].toLowerCase();
        String domainFull = parts[1].toLowerCase();

        String[] domainParts = domainFull.split("\\.", 2);
        String domain = domainParts[0];
        String tld = domainParts.length > 1 ? "." + domainParts[1] : "";

        Random random = new Random();
        String maskedId = maskWithRandomStars(id, random, 2);
        String maskedDomain = maskWithRandomStars(domain, random, 2);

        return (maskedId + "@" + maskedDomain + tld).toLowerCase();
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "";
        return phone.replaceAll("(\\d{3})-?(\\d{2,4})-?(\\d{2,4})", "$1-**$2-**$3");
    }

    private static String maskWithRandomStars(String src, Random random, int starCount) {
        if (src.length() <= 2) return src.charAt(0) + "*";
        char[] arr = src.toCharArray();
        Set<Integer> maskedIdx = new HashSet<>();
        int tries = 0;
        while (maskedIdx.size() < Math.min(starCount, src.length()-1) && tries < 10) {
            int idx = 1 + random.nextInt(src.length()-2);
            if (idx > 0 && idx < src.length()-1) maskedIdx.add(idx);
            tries++;
        }
        for (int idx : maskedIdx) arr[idx] = '*';
        return new String(arr);
    }
}
