# Suh Somansa Auth

소만사(SOMANSA) 직원 인증 확인 Java 라이브러리

## 최신버전 : 1.0.1

## 기능

- 소만사 Mattermost 로그인을 통한 직원 인증 확인
- OkHttp를 사용한 안정적인 HTTP 통신
- 간단한 boolean 반환으로 직원 여부 확인

## 설치 방법

### Gradle

```gradle
repositories {
    mavenCentral()
    // Suh-Nexus 추가
    maven {
        url "http://suh-project.synology.me:9999/repository/maven-releases/"
        allowInsecureProtocol = true
    }
}

dependencies {
    implementation 'me.suhsaechan:suh-somansa-auth: X.X.X' // 최신 버전으로 변경
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>suh-nexus</id>
        <url>http://suh-project.synology.me:9999/repository/maven-releases/</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>me.suhsaechan</groupId>
        <artifactId>suh-somansa-auth</artifactId>
        <version>X.X.X</version> <!-- 최신 버전으로 변경 -->
    </dependency>
</dependencies>
```

## 기본 사용법

### Spring Boot 프로젝트에서 사용

```java
import me.suhsaechan.suhsomansaauth.service.SomansaAuthService;
import me.suhsaechan.suhsomansaauth.dto.SomansaAuthResult;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    private final SomansaAuthService somansaAuthService;
    
    public UserService(SomansaAuthService somansaAuthService) {
        this.somansaAuthService = somansaAuthService;
    }
    
    public boolean checkEmployee(String loginId, String password) {
        SomansaAuthResult result = somansaAuthService.isSomansaEmployee(loginId, password);
        return result.isSomansaEmployee();
    }
    
    public String getDetailedResult(String loginId, String password) {
        SomansaAuthResult result = somansaAuthService.isSomansaEmployee(loginId, password);
        
        if (result.isSomansaEmployee()) {
            return "소만사 직원입니다: " + result.getMessage();
        } else {
            return "소만사 직원이 아닙니다: " + result.getMessage();
        }
    }
}
```

### 일반 Java 프로젝트에서 사용

```java
import me.suhsaechan.suhsomansaauth.service.SomansaAuthService;
import me.suhsaechan.suhsomansaauth.dto.SomansaAuthResult;

public class Example {
    public static void main(String[] args) {
        SomansaAuthService authService = new SomansaAuthService();
        
        // 간단한 직원 확인
        boolean isEmployee = authService.isSomansaEmployee("user@somansa.com", "password").isSomansaEmployee();
        System.out.println("소만사 직원 여부: " + isEmployee);
        
        // 상세한 결과 확인
        SomansaAuthResult result = authService.isSomansaEmployee("user@somansa.com", "password");
        
        System.out.println("직원 여부: " + result.isSomansaEmployee());
        System.out.println("인증 성공: " + result.isAuthSuccess());
        System.out.println("메시지: " + result.getMessage());
        System.out.println("요청 ID: " + result.getRequestId());
        
        if (!result.isSomansaEmployee()) {
            System.out.println("에러 ID: " + result.getErrorId());
        }
    }
}
```

## 인증 로직

### 성공 사례
1. **정상 로그인**: HTTP 200 응답 시 소만사 직원으로 판정
2. **MFA 단계**: HTTP 401이지만 `mfa.validate_token.authenticate.app_error` 에러 ID가 포함된 경우 소만사 직원으로 판정

### 실패 사례
1. **잘못된 인증 정보**: `api.user.login.invalid_credentials_email_username` 에러 ID가 포함된 경우 소만사 직원이 아님으로 판정
2. **시스템 오류**: 네트워크 오류 또는 예상치 못한 응답

## 응답 구조

```java
public class SomansaAuthResult {
    private boolean isSomansaEmployee;  // 소만사 직원 여부
    private boolean isAuthSuccess;      // 인증 성공 여부
    private String message;             // 응답 메시지
    private String errorId;             // 에러 ID (실패 시)
    private String requestId;           // 요청 ID
}
```

## 로그 설정

내부 Logger 사용으로 SLF4J 의존성 충돌을 방지합니다.

### 디버그 모드 활성화
```bash
# JVM 옵션으로 디버그 모드 활성화
-Dsomansa.debug=true
```

### 로그 출력 예시
```
[2024-01-15 10:30:45.123] INFO [SomansaAuthService] 소만사 인증 서비스 초기화 완료
[2024-01-15 10:30:45.456] INFO [SomansaAuthService] 소만사 직원 인증 시작 - loginId: us**@somansa.com
[2024-01-15 10:30:46.789] INFO [SomansaAuthService] MFA 인증 단계 도달 - 소만사 직원 확인됨
```

## 보안 고려사항

- 로그인 ID는 마스킹되어 로그에 기록됩니다
- 비밀번호는 로그에 기록되지 않습니다
- HTTPS 통신을 통한 안전한 인증 요청
- 타임아웃 설정으로 무한 대기 방지

## 라이선스

MIT License

## 변경 이력

### 1.0.0
- 초기 버전 출시
- 소만사 직원 인증 기능
- 내부 Logger 구현
- OkHttp 기반 HTTP 통신
