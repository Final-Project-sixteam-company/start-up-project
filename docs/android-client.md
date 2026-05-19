# Android 앱 연동 메모

<!-- 백엔드를 Android 네이티브 앱에서 호출할 때 헷갈리는 로컬 주소와 CORS 차이를 정리한다. -->

Android 앱은 Spring Boot 서버를 그대로 REST API 서버로 사용하면 됩니다. 웹처럼 화면을 서버에서 렌더링하는 방식이 아니라, Android 앱이 HTTP 요청을 보내고 JSON 응답을 받는 구조입니다.

## 로컬 서버 주소

Android Studio Emulator에서 PC의 `localhost:8080`에 접근할 때는 `localhost`가 아니라 아래 주소를 씁니다.

```text
http://10.0.2.2:8080
```

예시:

```text
http://10.0.2.2:8080/actuator/health
```

실제 기기에서 테스트할 때는 PC와 휴대폰이 같은 Wi-Fi에 있어야 하고, PC의 사설 IP를 씁니다.

```text
http://192.168.x.x:8080
```

Windows 방화벽이 8080 포트를 막으면 실제 기기에서 접속이 안 됩니다.

## Docker로 서버를 띄운 경우

`docker compose` 실행 기준으로 앱 서버는 호스트의 8080 포트에 열립니다.

- PC 브라우저: `http://localhost:8080`
- Android Emulator: `http://10.0.2.2:8080`
- 실제 기기: `http://PC_IP:8080`

## Android에서 HTTP 허용

로컬 개발은 보통 `http://`를 쓰므로 Android 9 이상에서 cleartext 설정이 필요할 수 있습니다.

개발용으로만 `AndroidManifest.xml`에 아래 설정을 둘 수 있습니다.

```xml
<application
    android:usesCleartextTraffic="true">
</application>
```

운영 배포에서는 HTTPS를 쓰는 방향이 맞습니다.

## Retrofit 기준 예시

Retrofit baseUrl은 마지막 `/`가 필요합니다.
현재 API 문서의 엔드포인트는 `/api/...` 경로를 기준으로 하므로, Retrofit 인터페이스에서 `@GET("api/scenarios")`처럼 `api/`를 포함해 호출합니다.

```kotlin
Retrofit.Builder()
    .baseUrl("http://10.0.2.2:8080/")
    .build()
```

실제 기기에서는:

```kotlin
Retrofit.Builder()
    .baseUrl("http://192.168.x.x:8080/")
    .build()
```

## CORS에 대해

Native Android 앱의 Retrofit, OkHttp 같은 HTTP 클라이언트는 브라우저가 아니므로 CORS 제한을 받지 않습니다.

다만 Android WebView, 웹 프론트, API 테스트 페이지까지 같이 쓸 수 있도록 서버에는 로컬 개발용 CORS 설정을 넣어두었습니다.

현재 기본 허용 패턴:

- `http://localhost:[*]`
- `http://127.0.0.1:[*]`
- `http://10.0.2.2:[*]`
- `http://192.168.*.*:[*]`

필요하면 `.env`에서 수정합니다.

```properties
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:[*],http://10.0.2.2:[*],http://192.168.*.*:[*]
```

## 웹 개발과 다른 점

백엔드 입장에서는 컨트롤러, DTO, 서비스, JPA 코드는 거의 같습니다.

달라지는 부분:

- 화면은 Spring이 아니라 Android 앱에서 만듭니다.
- 서버는 JSON API를 안정적으로 제공하는 역할에 집중합니다.
- 로그인 후 토큰은 Android 앱의 안전한 저장소에 저장합니다.
- 파일 업로드는 Android에서 `multipart/form-data`로 보냅니다.
- 로컬 접속 주소가 `localhost`가 아니라 Emulator 기준 `10.0.2.2`입니다.

## Spring AI 기능을 붙일 때

Spring AI는 서버 쪽 기능입니다. Android 앱은 일반 API처럼 서버 엔드포인트를 호출하고, 서버가 OpenAI 등 AI Provider와 통신한 뒤 JSON 응답을 내려주면 됩니다.

즉 Android에서 OpenAI 키를 직접 들고 있지 않는 구조가 기본입니다. API 키는 서버의 `.env` 또는 배포 secret에만 둡니다.
