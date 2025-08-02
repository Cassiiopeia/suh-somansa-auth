# Suh Somansa Auth

소만사(SOMANSA) 직원 인증 확인 Java 라이브러리

## 📋 최신 버전 : 1.0.5

## ✨ 주요 기능

- 🔐 **소만사 Mattermost 로그인을 통한 직원 인증 확인**
- 👤 **실제 사용자 이름 조회 기능** (예: "서새찬")
- 🌐 **OkHttp를 사용한 안정적인 HTTP 통신**
- 🎯 **간단한 boolean 반환으로 직원 여부 확인**
- 📝 **깔끔한 로그 출력 및 디버그 지원**

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

## 🚀 사용법

### 1️⃣ 기본 직원 인증 (기존 방식)

```java
import me.suhsaechan.suhsomansaauth.service.SomansaAuthEngine;
import me.suhsaechan.suhsomansaauth.dto.SomansaAuthResult;

public class Example {
    public static void main(String[] args) {
        SomansaAuthEngine authEngine = new SomansaAuthEngine();
        
        // 간단한 직원 확인
        SomansaAuthResult result = authEngine.isSomansaEmployee("chan4760", "password");
        
        if (result.isSomansaEmployee()) {
            System.out.println("✅ 소만사 직원입니다: " + result.getMessage());
        } else {
            System.out.println("❌ " + result.getMessage());
        }
    }
}
```

### 2️⃣ 📝 **NEW!** 사용자 이름까지 조회 (v1.0.5+)

```java
import me.suhsaechan.suhsomansaauth.service.SomansaAuthEngine;
import me.suhsaechan.suhsomansaauth.dto.SomansaAuthResult;

public class AdvancedExample {
    public static void main(String[] args) {
        SomansaAuthEngine authEngine = new SomansaAuthEngine();
        
        // 🆕 직원 인증 + 실제 이름 조회
        SomansaAuthResult result = authEngine.getSomansaEmployeeInfo("chan4760", "password");
        
        if (result.isSomansaEmployee()) {
            System.out.println("✅ 소만사 직원입니다!");
            System.out.println("👤 이름: " + result.getUserName()); // 예: "서새찬"
            System.out.println("📝 메시지: " + result.getMessage());
        } else {
            System.out.println("❌ " + result.getMessage());
        }
    }
}
```

### 3️⃣ Spring Boot에서 사용

```java
import me.suhsaechan.suhsomansaauth.service.SomansaAuthEngine;
import me.suhsaechan.suhsomansaauth.dto.SomansaAuthResult;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    private final SomansaAuthEngine somansaAuthEngine;
    
    public UserService(SomansaAuthEngine somansaAuthEngine) {
        this.somansaAuthEngine = somansaAuthEngine;
    }
    
    public boolean checkEmployee(String loginId, String password) {
        SomansaAuthResult result = somansaAuthEngine.isSomansaEmployee(loginId, password);
        return result.isSomansaEmployee();
    }
    
    // 🆕 이름까지 가져오는 메서드
    public String getEmployeeNameIfValid(String loginId, String password) {
        SomansaAuthResult result = somansaAuthEngine.getSomansaEmployeeInfo(loginId, password);
        
        if (result.isSomansaEmployee()) {
            return result.getUserName(); // 실제 이름 반환
        }
        return null;
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

## 📊 응답 구조

```java
public class SomansaAuthResult {
    private boolean isSomansaEmployee;  // 소만사 직원 여부
    private boolean isAuthSuccess;      // 인증 성공 여부
    private String message;             // 응답 메시지
    private String errorId;             // 에러 ID (실패 시)
    private String requestId;           // 요청 ID
    private String userName;            // 🆕 사용자 이름 (v1.0.5+)
}
```

### 🆕 새로운 메서드 (v1.0.5+)

- `getSomansaEmployeeInfo(loginId, password)` - 직원 인증 + 실제 이름 조회
- `getUserName()` - 조회된 사용자 실제 이름 반환

## 로그 설정

내부 Logger 사용으로 SLF4J 의존성 충돌을 방지합니다.

### 디버그 모드 활성화
```bash
# JVM 옵션으로 디버그 모드 활성화
-Dsomansa.debug=true
```

### 📋 로그 출력 예시

#### ✅ 성공시 (이름 조회 포함)
```
[2025-08-02 16:43:37.163] INFO [SomansaAuthEngine] 소만사 인증 엔진 초기화 완료
[2025-08-02 16:43:37.164] INFO [SomansaAuthEngine] 소만사 직원 정보 조회 시작 - loginId: ch******
[2025-08-02 16:43:37.514] INFO [SomansaAuthEngine] MFA 인증 단계 도달 - 소만사 직원 확인됨
[2025-08-02 16:43:38.677] INFO [SomansaAuthEngine] Mattermost 인증 성공 - 사용자 이름: 서새찬
```

#### ❌ 실패시
```
[2025-08-02 16:43:37.163] INFO [SomansaAuthEngine] 소만사 인증 엔진 초기화 완료
[2025-08-02 16:43:37.164] INFO [SomansaAuthEngine] 소만사 직원 정보 조회 시작 - loginId: us******
[2025-08-02 16:43:37.411] INFO [SomansaAuthEngine] Mattermost 로그인 정보 확인 필요
```

## 보안 고려사항

- 로그인 ID는 마스킹되어 로그에 기록됩니다
- 비밀번호는 로그에 기록되지 않습니다
- HTTPS 통신을 통한 안전한 인증 요청
- 타임아웃 설정으로 무한 대기 방지

## 라이선스

MIT License

## 📚 변경 이력

전체 변경 이력은 [CHANGELOG.md](./CHANGELOG.md)를 참고하세요.

### 🆕 v1.0.5 (최신)
- 🎯 **새로운 기능**: `getSomansaEmployeeInfo()` - 직원 인증 + 실제 이름 조회
- 🔄 **클래스명 변경**: `SomansaAuthService` → `SomansaAuthEngine`
- 📝 **로그 개선**: 더 명확한 Mattermost 로그인 오류 메시지
- 🛠️ **버그 수정**: 자동 도메인 처리 및 쿠키 관리 개선

### v1.0.4
- 버그 수정 및 안정성 개선

### v1.0.0
- 초기 버전 출시
- 소만사 직원 인증 기능
- 내부 Logger 구현
- OkHttp 기반 HTTP 통신
