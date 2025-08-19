# Mini Project – Full‑Stack Chat & Auth

본 저장소는 **Spring Boot 3.5.4 + JPA/QueryDSL + WebSocket(STOMP)** 기반 백엔드(`moonki`)와 **Vue 3 + Vite ^7.0.4 + Vuetify ^3.9.3 + Pinia ^3.0.3** 기반 프런트엔드(`frontend`)로 구성된 **DM 채팅/알림 + 이메일·카카오 인증 + JWT 보안** 예제 애플리케이션입니다.

---

## 주요 기능

- **회원 관리/인증**
  - 자체 회원가입/로그인(JWT 발급, 액세스/리프레시 토큰)
  - 이메일 인증(아이디 찾기/임시 비밀번호 발송 – SMTP 필요)
  - 카카오 OAuth 로그인
- **채팅**
  - STOMP over WebSocket + SockJS
  - 1:1 DM 채팅, **미읽음 수 집계/알림**(전체/발신자별)
  - 대화방 개설/재사용, 메시지 페이지네이션
- **보안/CORS**
  - Spring Security + 커스텀 `JwtAuthenticationFilter`
  - Handshake 인터셉터를 통한 **WebSocket JWT 검증**
  - 프런트 개발 서버(`http://localhost:5173`) CORS 허용
- **개발 편의**
  - QueryDSL, Lombok, JPA DDL 자동 반영(`ddl-auto: update`)
  - Vuetify UI, Pinia 상태관리, Axios API 모듈화

---

## 폴더 구조

```
moonki/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       └── java/
├── build.gradle
├── gradlew
├── gradlew.bat
├── HELP.md
└── settings.gradle
```

```
frontend/
├── public/
├── src/
│   ├── api/
│   │   ├── auth/
│   │   ├── chat/
│   │   ├── kakao/
│   │   ├── token/
│   │   └── user/
│   ├── assets/
│   │   └── kakao_logo_wide.png
│   ├── components/
│   │   ├── alert/
│   │   ├── chat/
│   │   ├── common/
│   │   ├── form/
│   │   └── modal/
│   ├── hooks/
│   │   ├── session/
│   │   └── user/
│   ├── layout/
│   │   ├── AppLayout.vue
│   │   ├── TheFooter.vue
│   │   └── TheHeader.vue
│   ├── plugins/
│   │   └── i18n.js
│   ├── properties/
│   │   └── locales/
│   ├── router/
│   │   └── index.js
│   ├── store/
│   │   ├── chat/
│   │   └── alert.js
│   ├── views/
│   │   ├── chat/
│   │   ├── error/
│   │   ├── kakao/
│   │   ├── main/
│   │   └── user/
│   ├── App.vue
│   └── main.js
├── babel.config.js
├── eslint.config.js
├── frontend.iml
├── index.html
├── jsconfig.json
├── package-lock.json
├── package.json
├── README.md
└── vite.config.js
```

> 루트에는 `moonki`(백엔드, Gradle)와 `frontend`(프런트, Vite) 두 개의 독립 프로젝트가 존재합니다.

---

## 빠른 시작 (로컬 개발)

### 1) 필수 요구사항
- **JDK 17**
- **Node.js 18+** (Vite ^7.0.4 사용 권장: LTS 18/20)
- **MySQL 8.x** (기본 스키마명: `project`)

### 2) 데이터베이스 준비
```sql
CREATE DATABASE project CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER 'root'@'%' IDENTIFIED BY '1234'; -- 필요 시 변경
GRANT ALL PRIVILEGES ON project.* TO 'root'@'%';
FLUSH PRIVILEGES;
```
> 실제 환경에선 사용자/비밀번호를 반드시 변경하고, 계정/권한을 최소화하세요.

### 3) 백엔드 실행
```bash
cd moonki
./gradlew bootRun
# 또는
./gradlew clean build
java -jar build/libs/moonki-0.0.1-SNAPSHOT.jar
```
- 기본 포트: **`8081`**
- JPA: `ddl-auto: update` → 첫 실행 시 스키마 자동 생성
- `src/main/resources/application.yml`에서 **DB/메일/JWT** 값을 프로젝트 환경에 맞춰 수정하세요.
  - (권장) `application-local.yml`을 추가하고 `--spring.profiles.active=local`로 민감정보를 분리 관리

### 4) 프런트엔드 실행
```bash
cd frontend
npm ci   # 또는 npm install
npm run dev
```
- 기본 포트: **`5173`**
- 환경변수: `frontend/.env`
```
VITE_API_URL=http://localhost:8081
VITE_WS_URL=http://localhost:8081/ws
VITE_KAKAO_CLIENT_ID=df3c513849129cd04414d8506466d718
VITE_KAKAO_REDIRECT_URI=http://localhost:5173/oauth/kakao/callback
VITE_KAKAO_AUTHORIZE=https://kauth.kakao.com/oauth/authorize
```
> 필요 시 API/WS URL을 변경하세요. 백엔드 CORS 허용 도메인(`SecurityConfig`)도 함께 조정합니다.

---

## 백엔드 구성 개요

- **주요 기술**: Spring Boot 3.5.4, Spring Data JPA, QueryDSL, Spring Security, WebSocket(STOMP), Mail, Validation, Thymeleaf(메일 템플릿), Lombok
- **패키지 개요**
  - `config` : 보안/웹/QueryDSL/WebSocket 설정 (`SecurityConfig`, `WebSocketConfig`, `WsJwtHandshakeInterceptor`, `WsUserHandshakeHandler` 등)
  - `security` : `JwtTokenProvider`, `JwtAuthenticationFilter`
  - `domain` : 엔티티(`user`, `chat`, `post`, `auth` 등)
  - `repository` : JPA/QueryDSL 리포지토리
  - `service` : 도메인 서비스(`ChatService`, `ChatQueryService`, `LoginService` 등)
  - `controller` : REST (`/auth/**`, `/api/chat/**`, `/api/user/**`) 및 WS 컨트롤러
  - `resources` : `application.yml`, `application-oauth.yml`, 메일 템플릿
- **DB 설정 (예시)** – `application.yml`
  - `spring.datasource.url=jdbc:mysql://localhost:3306/project?...`
  - `spring.jpa.hibernate.ddl-auto=update`
  - `server.port=8081`
  - `app.jwt.*` 토큰 관련 설정(시크릿/만료/리프레시쿠키)

### 보안 & CORS
- `SecurityFilterChain`에서 CORS 허용 및 `JwtAuthenticationFilter` 등록
- 기본으로 `/api/auth/**`, `/auth/**`, `/api/chat/**`, `/ws/**`가 `permitAll`로 설정되어 있습니다.
  - **실서비스**에서는 `/api/chat/**`도 토큰 기반 인증이 필요하므로, 필요 시 `authenticated()`로 변경하세요.

---

## 프런트엔드 구성 개요

- **주요 기술**: Vue ^3.5.17, Vite ^7.0.4, Vuetify ^3.9.3, Pinia ^3.0.3, Axios, Vue Router, `@stomp/stompjs` ^7.1.1, `sockjs-client` ^1.6.1
- **핵심 디렉터리**
  - `src/api/**` : Axios 기반 API 클라이언트(토큰 자동첨부, 채팅/사용자/카카오)
  - `src/store/chat/chat.js` : 채팅 상태/소켓 연결/미읽음 관리
  - `src/views/**` : `Main`, `Account`, `ChatHome` 등 라우트 뷰
  - `src/components/**` : 폼/모달/채팅 버블/알림 컴포넌트
  - `src/router/index.js` : 인증 가드 + 토큰 만료 전 자동 갱신 로직
- **환경변수 (`.env`)**
  - `VITE_API_URL` : REST API 베이스 URL (예: `http://localhost:8081`)
  - `VITE_WS_URL`  : WebSocket 엔드포인트 (예: `http://localhost:8081/ws`)
  - `VITE_KAKAO_*` : 카카오 OAuth 설정

---

## API 요약 (주요 엔드포인트)

### 인증(`/auth/**`)
- `POST /auth/signup` : 회원가입
- `POST /auth/login` : 로그인(JWT 발급)
- `GET  /auth/exists/userId?userId=` : 아이디 중복체크
- `GET  /auth/exists/nickname?nickname=` : 닉네임 중복체크
- `GET  /auth/me` : 내 프로필

### 이메일 인증(`/auth/mail/**`)
- `POST /auth/mail/sendCode` : 인증코드 발송
- `POST /auth/mail/checkCode` : 코드 검증
- `GET  /auth/mail/findUser?userId=` : 비밀번호 재설정 사용자 확인
- `POST /auth/mail/sendTempPw` : 임시 비밀번호 발송

### 채팅 REST(`/api/chat/**`)
- `GET  /api/chat/users?q=&limit=` : **미읽음 있는 사용자 우선**으로 사용자 목록
- `POST /api/chat/rooms/dm?userId=` : DM 방 개설(존재 시 재사용)
- `GET  /api/chat/rooms/{roomId}/messages?page=&size=` : 메시지 페이징 조회
- `POST /api/chat/rooms/{roomId}/read` : 방의 메시지 **읽음 처리**
- `GET  /api/chat/unread/count` : 내 전체 미읽음 수
- `GET  /api/chat/unread/by-sender` : **발신자별** 미읽음 수
- `GET  /api/chat/rooms/my?limit=` : 나의 채팅방 목록

---

## WebSocket/STOMP 사용 방법

- **엔드포인트**: `http://localhost:8081/ws`
- **Handshake 토큰 전달**: `Authorization: Bearer <JWT>` 헤더 **또는** `ws?token=<JWT>` 쿼리스트링
- **보내기(SEND)**: `destination=/app/chat.send.{roomId}`
  - 페이로드 예시:
    ```json
    {
      "content": "안녕하세요"
    }
    ```
- **구독(SUBSCRIBE)**:
  - 방 메시지: `/topic/chat.room.{roomId}`
  - 미읽음 이벤트: `/topic/notify.{myUserPk}`

> 서버는 메시지 저장 후, 보낸 사람의 닉네임/타임스탬프를 포함한 `ChatMessageDto`를 브로드캐스트하고, 수신자에 대해 전체/발신자별 미읽음 카운트를 이벤트로 발행합니다.

---

## 개발 팁 & 트러블슈팅

- **JWT 401/403**: 로컬 라우팅 진입 시 토큰이 없거나 만료되면 `/login`으로 이동합니다. `.env`의 API URL, 백엔드 CORS 설정을 재확인하세요.
- **DM 개설 500**: `POST /api/chat/rooms/dm` 호출 시 **`userId` 파라미터명**이 일치해야 합니다. (프런트 `openDmRoom(userId)` 확인)
- **WebSocket 403/Handshake 실패**: 토큰 미첨부/만료 또는 CORS 미허용. `WsJwtHandshakeInterceptor`와 `SecurityConfig`의 허용 도메인을 확인하세요.
- **MySQL 연결 실패**: `spring.datasource.*` 값, 포트/권한/방화벽 확인. 최초 실행은 `ddl-auto: update`로 테이블 자동생성.
- **카카오 OAuth 401**: 카카오 개발자 콘솔의 **Redirect URI**가 `.env`의 `VITE_KAKAO_REDIRECT_URI`와 정확히 일치해야 합니다.
- **QueryDSL 생성소스 인식 문제(IDE)**: Gradle 빌드 후 `build/generated` 경로를 **Generated Sources**로 마킹하세요.

---

## 배포(예시)

1) 백엔드
```bash
cd moonki
./gradlew clean bootJar
java -jar build/libs/moonki-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```
2) 프런트엔드
```bash
cd frontend
npm run build
# 정적 파일은 Nginx/Apache 등으로 서빙하거나 `npm run preview`로 검수
```

> 실서비스에서는 HTTPS, 보안 헤더, JWT 시크릿/메일자격증명/DB 크리덴셜의 **비밀 관리**(환경변수/Secret Manager 등)를 필수로 적용하세요.

---

## 라이선스
사내/과제용 예제 코드로 별도 라이선스를 명시하지 않았습니다. 외부 공개 전 라이선스 표기를 검토하세요.

---

## 작성 이력
- 최초 작성: 자동 생성 (본 README는 저장소 구조를 기반으로 생성되었습니다)