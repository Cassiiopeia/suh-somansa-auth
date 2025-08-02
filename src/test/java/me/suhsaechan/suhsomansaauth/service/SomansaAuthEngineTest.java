package me.suhsaechan.suhsomansaauth.service;

import me.suhsaechan.suhsomansaauth.dto.SomansaAuthResult;
import me.suhsaechan.suhsomansaauth.util.LogUtil;
import org.junit.jupiter.api.Test;
import lombok.extern.slf4j.Slf4j;

/**
 * 소만사 인증 엔진 테스트
 */
@Slf4j
class SomansaAuthEngineTest {

    private SomansaAuthEngine somansaAuthEngine;

    @Test
    void mainTest(){
        LogUtil.timeLog(this::testValidation);
        LogUtil.timeLog(this::testInvalidCredentials);
//        LogUtil.timeLog(this::testValidCredentials);
//        LogUtil.timeLog(this::testGetEmployeeInfo);
    }

    void testValidation() {
        somansaAuthEngine = new SomansaAuthEngine();

        // 입력값 검증 테스트
        
        // 빈 로그인 ID 테스트
        SomansaAuthResult result1 = somansaAuthEngine.isSomansaEmployee("", "password");
        assert !result1.isSomansaEmployee();
        assert "로그인 ID가 필요합니다".equals(result1.getMessage());
        log.info("빈 로그인 ID 테스트 통과");
        
        // 빈 비밀번호 테스트
        SomansaAuthResult result2 = somansaAuthEngine.isSomansaEmployee("testuser", "");
        assert !result2.isSomansaEmployee();
        assert "비밀번호가 필요합니다".equals(result2.getMessage());
        log.info("빈 비밀번호 테스트 통과");
        
        // null 값 테스트
        SomansaAuthResult result3 = somansaAuthEngine.isSomansaEmployee(null, "password");
        assert !result3.isSomansaEmployee();
        log.info("null 로그인 ID 테스트 통과");
        
        SomansaAuthResult result4 = somansaAuthEngine.isSomansaEmployee("testuser", null);
        assert !result4.isSomansaEmployee();
        log.info("null 비밀번호 테스트 통과");
    }
    
    void testInvalidCredentials() {

        // 잘못된 인증 정보로 테스트 (실제 요청)
        SomansaAuthResult result = somansaAuthEngine.isSomansaEmployee("invalid_user", "invalid_password");
        
        // 소만사 직원이 아님을 확인
        LogUtil.superLog(result.isSomansaEmployee());
    }
    
     // 주의: 실제 계정 정보로 테스트하지 마세요
     void testValidCredentials() {

         // 실제 소만사 계정으로 테스트
         SomansaAuthResult result = somansaAuthEngine.isSomansaEmployee("실제아이디", "실제비밀번호");

         // 소만사 직원임을 확인 (MFA 또는 정상 로그인)
         LogUtil.superLog(result.isSomansaEmployee());
     }
     
     // 주의: 실제 계정 정보로 테스트하지 마세요
     void testGetEmployeeInfo() {

         // 실제 소만사 계정으로 이름 포함 정보 조회 테스트
         SomansaAuthResult result = somansaAuthEngine.getSomansaEmployeeInfo("실제아이디", "실제비밀번호");

         // 소만사 직원임을 확인 (MFA 또는 정상 로그인)
         LogUtil.superLog(result.isSomansaEmployee());
     }
}