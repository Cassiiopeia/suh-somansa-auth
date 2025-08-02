package me.suhsaechan.suhsomansaauth.dto;

/**
 * 소만사 인증 결과 DTO
 */
public class SomansaAuthResult {
    
    // 소만사 직원 여부
    private boolean isSomansaEmployee;
    
    // 인증 성공 여부
    private boolean isAuthSuccess;
    
    // 응답 메시지
    private String message;
    
    // 에러 ID (실패 시)
    private String errorId;
    
    // 요청 ID
    private String requestId;
    
    // 사용자 이름
    private String userName;
    
    public SomansaAuthResult() {}
    
    public SomansaAuthResult(boolean isSomansaEmployee, boolean isAuthSuccess, String message, String errorId, String requestId) {
        this.isSomansaEmployee = isSomansaEmployee;
        this.isAuthSuccess = isAuthSuccess;
        this.message = message;
        this.errorId = errorId;
        this.requestId = requestId;
        this.userName = null;
    }
    
    public SomansaAuthResult(boolean isSomansaEmployee, boolean isAuthSuccess, String message, String errorId, String requestId, String userName) {
        this.isSomansaEmployee = isSomansaEmployee;
        this.isAuthSuccess = isAuthSuccess;
        this.message = message;
        this.errorId = errorId;
        this.requestId = requestId;
        this.userName = userName;
    }
    
    // Getter 메소드들
    public boolean isSomansaEmployee() {
        return isSomansaEmployee;
    }
    
    public boolean isAuthSuccess() {
        return isAuthSuccess;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getErrorId() {
        return errorId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    // Setter 메소드들
    public void setSomansaEmployee(boolean somansaEmployee) {
        isSomansaEmployee = somansaEmployee;
    }
    
    public void setAuthSuccess(boolean authSuccess) {
        isAuthSuccess = authSuccess;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public void setErrorId(String errorId) {
        this.errorId = errorId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    /**
     * 성공 결과 생성
     */
    public static SomansaAuthResult success(String message, String requestId) {
        return new SomansaAuthResult(true, true, message, null, requestId);
    }
    
    /**
     * 성공 결과 생성 (사용자 이름 포함)
     */
    public static SomansaAuthResult success(String message, String requestId, String userName) {
        return new SomansaAuthResult(true, true, message, null, requestId, userName);
    }
    
    /**
     * 실패 결과 생성 (소만사 직원 아님)
     */
    public static SomansaAuthResult failure(String message, String errorId, String requestId) {
        return new SomansaAuthResult(false, false, message, errorId, requestId);
    }
    
    /**
     * 예외 결과 생성
     */
    public static SomansaAuthResult exception(String message) {
        return new SomansaAuthResult(false, false, message, "SYSTEM_ERROR", null);
    }
}