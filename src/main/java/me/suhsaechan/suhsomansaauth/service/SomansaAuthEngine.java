package me.suhsaechan.suhsomansaauth.service;

import me.suhsaechan.suhsomansaauth.dto.SomansaAuthResult;
import me.suhsaechan.suhsomansaauth.util.SomansaLogger;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 소만사 인증 엔진
 * 소만사 Mattermost 로그인을 통한 직원 인증 확인
 */
@Service
public class SomansaAuthEngine {
    
    private static final SomansaLogger logger = SomansaLogger.getLogger(SomansaAuthEngine.class);
    
    private static final String LOGIN_URL = "https://mattermost.somansa.com/api/v4/users/login";
    private static final String MFA_ERROR_ID = "mfa.validate_token.authenticate.app_error";
    private static final String INVALID_CREDENTIALS_ID = "api.user.login.invalid_credentials_email_username";
    
    // 버스 예약 시스템 엔드포인트 
    private static final String BUS_LOGIN_PAGE_URL = "https://cs.android.busin.co.kr/Login.aspx?device=";
    private static final String BUS_LOGIN_API_URL = "https://cs.android.busin.co.kr/Login.aspx/LoginCheck";
    private static final String BUS_CREATE_SESSION_URL = "https://cs.android.busin.co.kr/Default.aspx/CreateSession";
    private static final String BUS_HOME_URL = "https://cs.android.busin.co.kr/Home.aspx#";
    
    private final OkHttpClient httpClient;
    
    public SomansaAuthEngine() {
        // HTTP 클라이언트 설정 (쿠키 지원 추가)
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .cookieJar(new SimpleCookieJar())
                .build();
        
        logger.info("소만사 인증 서비스 초기화 완료");
    }
    
    /**
     * 간단한 쿠키 저장소 구현
     */
    private static class SimpleCookieJar implements CookieJar {
        private final java.util.List<Cookie> cookieStore = new java.util.ArrayList<>();

        @Override
        public void saveFromResponse(HttpUrl url, java.util.List<Cookie> cookies) {
            cookieStore.addAll(cookies);
        }

        @Override
        public java.util.List<Cookie> loadForRequest(HttpUrl url) {
            java.util.List<Cookie> validCookies = new java.util.ArrayList<>();
            for (Cookie cookie : cookieStore) {
                if (cookie.matches(url)) {
                    validCookies.add(cookie);
                }
            }
            return validCookies;
        }
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
     * 소만사 직원 정보 확인 (이름 포함)
     * Mattermost 인증 후 버스 시스템에서 사용자 이름을 가져옵니다.
     * 
     * @param loginId 로그인 ID (이메일 또는 사용자명)
     * @param password 비밀번호
     * @return 인증 결과 (사용자 이름 포함)
     */
    public SomansaAuthResult getSomansaEmployeeInfo(String loginId, String password) {
        logger.info("소만사 직원 정보 조회 시작 - loginId: " + maskLoginId(loginId));
        
        // 1. 먼저 Mattermost 인증 수행
        SomansaAuthResult authResult = isSomansaEmployee(loginId, password);
        
        if (!authResult.isSomansaEmployee()) {
            logger.info("Mattermost 인증 실패");
            return authResult; // 인증 실패 시 그대로 반환
        }
        
        logger.debug("Mattermost 인증 성공 - 사용자 이름 조회 시작");
        
        // 2. 버스 시스템에서 사용자 이름 조회 (도메인 추가)
        String busLoginId = loginId.contains("@") ? loginId : loginId + "@somansa.com";
        String userName = getBusSystemUserName(busLoginId);
        
        if (userName != null) {
            logger.info("Mattermost 인증 성공 - 사용자 이름: " + userName);
            return SomansaAuthResult.success(
                authResult.getMessage() + " (이름: " + userName + ")", 
                authResult.getRequestId(), 
                userName
            );
        } else {
            logger.info("Mattermost 인증 성공");
            logger.debug("사용자 이름 조회 실패 - 기본 결과 반환");
            return authResult; // 이름 조회 실패해도 인증은 성공이므로 기본 결과 반환
        }
    }
    
    /**
     * 버스 시스템에서 사용자 이름을 가져옵니다.
     * 
     * @param loginId 로그인 ID
     * @return 사용자 이름, 실패 시 null
     */
    private String getBusSystemUserName(String loginId) {
        try {
            logger.debug("사용자 이름 조회를 위한 추가 인증 시작");
            
            // 1. 로그인 페이지 GET (세션 쿠키 획득)
            if (!getBusLoginPage()) {
                return null;
            }
            
            // 2. 버스 시스템 로그인
            int passengerId = performBusLogin(loginId);
            if (passengerId <= 0) {
                logger.debug("추가 인증 실패");
                return null;
            }
            
            // 3. CreateSession 두 번 호출
            createBusSession(loginId, passengerId);
            
            // 4. Home 페이지에서 사용자 이름 추출
            return extractUserNameFromBusHome();
            
        } catch (Exception e) {
            logger.debug("사용자 이름 조회 중 예외 발생", e);
            return null;
        }
    }
    
    /**
     * 버스 시스템 로그인 페이지 GET
     */
    private boolean getBusLoginPage() {
        try {
            Request request = new Request.Builder()
                    .url(BUS_LOGIN_PAGE_URL)
                    .get()
                    .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.debug("버스 로그인 페이지 GET 성공");
                    return true;
                } else {
                    logger.warn("버스 로그인 페이지 GET 실패: " + response.code());
                    return false;
                }
            }
        } catch (IOException e) {
            logger.error("버스 로그인 페이지 GET 중 예외", e);
            return false;
        }
    }
    
    /**
     * 버스 시스템 로그인 수행
     */
    private int performBusLogin(String loginId) {
        try {
            String payload = String.format("{ \"data\": \"%s,pc\" }", escapeJson(loginId));
            RequestBody body = RequestBody.create(payload, MediaType.get("application/json"));
            
            Request request = new Request.Builder()
                    .url(BUS_LOGIN_API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json; charset=UTF-8")
                    .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15")
                    .addHeader("Referer", BUS_LOGIN_PAGE_URL)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    logger.debug("버스 로그인 응답: " + responseBody);
                    
                    // {"d":126491} 형태에서 승객 ID 추출
                    // 간단한 정규식 또는 더 안전한 방법 사용
                    try {
                        // "d": 뒤의 숫자 부분만 추출
                        String pattern = "\"d\":";
                        int startIndex = responseBody.indexOf(pattern);
                        if (startIndex != -1) {
                            startIndex += pattern.length(); // "d": 길이만큼 건너뛰기
                            int endIndex = responseBody.indexOf("}", startIndex);
                            if (endIndex == -1) {
                                endIndex = responseBody.length(); // } 가 없으면 끝까지
                            }
                            
                            String idStr = responseBody.substring(startIndex, endIndex)
                                    .trim()
                                    .replaceAll("[^0-9-]", ""); // 숫자와 마이너스만 남김
                            
                            if (!idStr.isEmpty()) {
                                int passengerId = Integer.parseInt(idStr);
                                logger.debug("추가 인증 성공, ID: " + passengerId);
                                return passengerId;
                            } else {
                                logger.debug("ID를 추출할 수 없음: " + responseBody);
                            }
                        } else {
                            logger.debug("\"d\": 패턴을 찾을 수 없음: " + responseBody);
                        }
                    } catch (Exception e) {
                        logger.debug("ID 파싱 실패 - 응답: " + responseBody + ", 오류: " + e.getMessage());
                    }
                }
                logger.debug("추가 인증 실패: " + response.code());
                return -1;
            }
        } catch (IOException e) {
            logger.error("버스 로그인 중 예외", e);
            return -1;
        }
    }
    
    /**
     * 버스 시스템 세션 생성
     */
    private void createBusSession(String loginId, int passengerId) {
        try {
            String data = String.format("%s,%d,,pc", loginId, passengerId);
            String payload = String.format("{ \"data\": \"%s\" }", data);
            RequestBody body = RequestBody.create(payload, MediaType.get("application/json"));
            
            Request request = new Request.Builder()
                    .url(BUS_CREATE_SESSION_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json; charset=UTF-8")
                    .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15")
                    .build();

            // 두 번 호출 (원래 로직과 동일)
            try (Response response1 = httpClient.newCall(request).execute()) {
                logger.debug("첫 번째 세션 생성: " + response1.code());
            }
            try (Response response2 = httpClient.newCall(request).execute()) {
                logger.debug("두 번째 세션 생성: " + response2.code());
            }
        } catch (IOException e) {
            logger.error("버스 세션 생성 중 예외", e);
        }
    }
    
    /**
     * 버스 시스템 Home 페이지에서 사용자 이름 추출
     */
    private String extractUserNameFromBusHome() {
        try {
            Request request = new Request.Builder()
                    .url(BUS_HOME_URL)
                    .get()
                    .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15")
                    .addHeader("Referer", BUS_LOGIN_PAGE_URL)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.warn("버스 Home 페이지 GET 실패: " + response.code());
                    return null;
                }

                String html = response.body().string();
                Document doc = Jsoup.parse(html);
                
                // <span id="noticeClear">서새찬</span> 요소 찾기
                Element nameElement = doc.getElementById("noticeClear");
                if (nameElement != null) {
                    String userName = nameElement.text().trim();
                    logger.debug("사용자 이름 추출 성공: " + userName);
                    return userName;
                }
                
                // 대안: "서새찬 님 안녕하세요" 형태에서 추출
                Element titleElement = doc.select(".contents-box .title").first();
                if (titleElement != null) {
                    String titleText = titleElement.text();
                    if (titleText.contains("님 안녕하세요")) {
                        String userName = titleText.replace("님 안녕하세요", "").trim();
                        logger.debug("대안 방법으로 사용자 이름 추출 성공: " + userName);
                        return userName;
                    }
                }
                
                logger.warn("사용자 이름 요소를 찾을 수 없음");
                return null;
            }
        } catch (IOException e) {
            logger.error("버스 Home 페이지 요청 중 예외", e);
            return null;
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