package me.suhsaechan.suhsomansaauth.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 소만사 인증 모듈 전용 로거
 * SLF4J 의존성 충돌 방지를 위한 내부 구현
 */
public class SomansaLogger {
    private final String className;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private SomansaLogger(String className) {
        this.className = className;
    }
    
    public static SomansaLogger getLogger(Class<?> clazz) {
        return new SomansaLogger(clazz.getSimpleName());
    }
    
    public void info(String message) {
        log("INFO", message);
    }
    
    public void debug(String message) {
        log("DEBUG", message);
    }
    
    public void debug(String message, Throwable throwable) {
        log("DEBUG", message + " - " + throwable.getMessage());
        if (isDebugEnabled()) {
            throwable.printStackTrace();
        }
    }
    
    public void warn(String message) {
        log("WARN", message);
    }
    
    public void error(String message) {
        log("ERROR", message);
    }
    
    public void error(String message, Throwable throwable) {
        log("ERROR", message + " - " + throwable.getMessage());
        if (isDebugEnabled()) {
            throwable.printStackTrace();
        }
    }
    
    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logMessage = String.format("[%s] %s [%s] %s", 
            timestamp, level, className, message);
        System.out.println(logMessage);
    }
    
    private boolean isDebugEnabled() {
        return "true".equalsIgnoreCase(System.getProperty("somansa.debug", "false"));
    }
}