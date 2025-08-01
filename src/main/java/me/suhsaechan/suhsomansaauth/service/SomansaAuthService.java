package me.suhsaechan.suhsomansaauth.service;

import me.suhsaechan.suhsomansaauth.dto.SomansaAuthResult;
import me.suhsaechan.suhsomansaauth.util.SomansaLogger;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 소만사 인증 서비스
 * 소만사 Mattermost 로그인을 통한 직원 인증 확인
 */
@Service
public class SomansaAuthService {
    
    private static final SomansaLogger logger = SomansaLogger.getLogger(SomansaAuthService.class);
    
    private static final String LOGIN_URL = "https://mattermost.somansa.com/api/v4/users/login";
    private static final String MFA_ERROR_ID = "mfa.validate_token.authenticate.app_error";
    private static final String INVALID_CREDENTIALS_ID = "api.user.login.invalid_credentials_email_username";
    
    private final OkHttpClient httpClient;
    
    public SomansaAuthService() {
        // HTTP 클라이언트 설정
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        logger.info("소만사 인증 서비스 초기화 완료");
    }
    
    /**
     * 소만사 직원 여부 확인
     * 
     * @param loginId 로그인 ID (이메일 또는 사용자명)
     * @param password 비밀번호
     * @return 인증 결과
     */
    public SomansaAuthResult isSomansaEmployee(String loginId, String password) {
        logger.info("소만사 직원 인증 시작 - loginId: " + maskLoginId(loginId));
        
        try {
            // 입력값 검증
            if (loginId == null || loginId.trim().isEmpty()) {
                logger.warn("로그인 ID가 비어있음");
                return SomansaAuthResult.exception("로그인 ID가 필요합니다");
            }
            
            if (password == null || password.trim().isEmpty()) {
                logger.warn("비밀번호가 비어있음");
                return SomansaAuthResult.exception("비밀번호가 필요합니다");
            }
            
            // 로그인 요청 생성 및 전송
            String jsonPayload = createLoginPayload(loginId.trim(), password);
            Request request = createHttpRequest(jsonPayload);
            
            logger.debug("소만사 서버에 인증 요청 전송");
            
            try (Response response = httpClient.newCall(request).execute()) {
                return processResponse(response);
            }
            
        } catch (Exception e) {
            logger.error("소만사 인증 중 예외 발생", e);
            return SomansaAuthResult.exception("인증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 로그인 페이로드 생성
     */
    private String createLoginPayload(String loginId, String password) {
        return String.format(
            "{\"login_id\":\"%s\",\"password\":\"%s\",\"token\":\"\",\"deviceId\":\"\"}",
            escapeJson(loginId),
            escapeJson(password)
        );
    }
    
    /**
     * HTTP 요청 생성
     */
    private Request createHttpRequest(String jsonPayload) {
        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));
        
        return new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Language", "ko")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Content-Type", "application/json")
                .addHeader("Origin", "https://mattermost.somansa.com")
                .addHeader("Pragma", "no-cache")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
    }
    
    /**
     * 응답 처리
     */
    private SomansaAuthResult processResponse(Response response) throws IOException {
        String responseBody = response.body() != null ? response.body().string() : "";
        int statusCode = response.code();
        
        logger.debug("응답 상태 코드: " + statusCode);
        logger.debug("응답 본문 길이: " + responseBody.length());
        
        if (statusCode == 401) {
            return handleUnauthorizedResponse(responseBody);
        } else if (statusCode == 200) {
            logger.info("정상 로그인 성공 - 소만사 직원 확인됨");
            return SomansaAuthResult.success("소만사 직원입니다", extractRequestId(responseBody));
        } else {
            logger.warn("예상치 못한 응답 코드: " + statusCode);
            return SomansaAuthResult.exception("예상치 못한 응답입니다: " + statusCode);
        }
    }
    
    /**
     * 401 Unauthorized 응답 처리
     */
    private SomansaAuthResult handleUnauthorizedResponse(String responseBody) {
        try {
            // JSON 파싱을 통한 에러 확인
            if (responseBody.contains(MFA_ERROR_ID)) {
                logger.info("MFA 인증 단계 도달 - 소만사 직원 확인됨");
                String requestId = extractRequestId(responseBody);
                return SomansaAuthResult.success("소만사 직원입니다 (MFA 단계)", requestId);
            } else if (responseBody.contains(INVALID_CREDENTIALS_ID)) {
                logger.info("잘못된 인증 정보 - 소만사 직원 아님");
                String requestId = extractRequestId(responseBody);
                return SomansaAuthResult.failure("소만사 직원이 아닙니다", INVALID_CREDENTIALS_ID, requestId);
            } else {
                logger.warn("알 수 없는 401 응답: " + responseBody);
                return SomansaAuthResult.exception("알 수 없는 인증 오류입니다");
            }
        } catch (Exception e) {
            logger.error("응답 파싱 중 오류", e);
            return SomansaAuthResult.exception("응답 처리 중 오류가 발생했습니다");
        }
    }
    
    /**
     * 응답에서 request_id 추출
     */
    private String extractRequestId(String responseBody) {
        try {
            int startIndex = responseBody.indexOf("\"request_id\":\"");
            if (startIndex != -1) {
                startIndex += 14; // "request_id":" 길이
                int endIndex = responseBody.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    return responseBody.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {
            logger.debug("request_id 추출 실패", e);
        }
        return null;
    }
    
    /**
     * JSON 문자열 이스케이프 처리
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * 로그인 ID 마스킹 처리 (로그용)
     */
    private String maskLoginId(String loginId) {
        if (loginId == null || loginId.length() <= 3) {
            return "***";
        }
        
        if (loginId.contains("@")) {
            // 이메일인 경우
            String[] parts = loginId.split("@");
            String localPart = parts[0];
            String domain = parts.length > 1 ? parts[1] : "";
            
            String maskedLocal = localPart.length() > 2 
                ? localPart.substring(0, 2) + "*".repeat(Math.max(1, localPart.length() - 2))
                : localPart;
            
            return maskedLocal + "@" + domain;
        } else {
            // 사용자명인 경우
            return loginId.substring(0, 2) + "*".repeat(Math.max(1, loginId.length() - 2));
        }
    }
}