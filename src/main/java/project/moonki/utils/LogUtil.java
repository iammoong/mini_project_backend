package project.moonki.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.time.LocalDateTime;

@Slf4j
@UtilityClass
public class LogUtil {


    public void error(Logger log, Class<?> clazz, Throwable t) {
        // 포맷: [class:..., message:..., time:...]
        log.error("[class:{}, message:{}, time:{}]",
                clazz.getSimpleName(),
                t != null ? String.valueOf(t.getMessage()) : "null",
                LocalDateTime.now(),
                t);
    }

    public void error(Logger log, Class<?> clazz, String message, Throwable t) {
        log.error("[class:{}, message:{}, time:{}]",
                clazz.getSimpleName(),
                message,
                LocalDateTime.now(),
                t);
    }
}
