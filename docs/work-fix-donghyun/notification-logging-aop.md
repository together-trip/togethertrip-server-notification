# Work: 알림 서버 공통 로깅 AOP 구현

작성일: 2026-06-08
브랜치: `feature/issue-6-notification-logging-aop`
이슈: https://github.com/together-trip/togethertrip-server-notification/issues/6
PR: https://github.com/together-trip/togethertrip-server-notification/pull/7

## 작업

알림 서버에 공통 로깅 기반을 추가했다.

현재 `notification` 서버는 skeleton 상태지만, 이후 알림 생성, 대상 검증, 푸시 provider 호출 로직이 붙을 때 로그 기준이 흔들리지 않도록 요청 식별자, 알림 컨텍스트, Service/Client 실행 시간, 민감정보 마스킹을 먼저 구성했다.

## 배경

알림 서버는 `main`, `chat`, `gateway`에서 들어오는 알림 요청과 푸시 provider 호출을 담당하게 된다.

운영 중에는 다음 정보를 빠르게 확인해야 한다.

- 어떤 이벤트에서 알림 생성 흐름이 시작되었는지
- 어떤 사용자에게 알림이 생성 또는 제외되었는지
- provider 호출이 성공했는지 실패했는지
- 푸시 토큰, provider credential, payload 개인정보가 로그에 남지 않았는지

## 수정

- `spring-aop`, `aspectjweaver` 의존성을 추가했다.
- HTTP 요청 단위 `X-Request-Id` 생성/유지 필터를 추가했다.
- 요청 완료 시 method, path, status, elapsedMs를 로그로 남기도록 했다.
- 알림 특화 로그 컨텍스트를 추가했다.
  - `requestId`
  - `userId`
  - `notificationId`
  - `eventType`
  - `targetUserId`
  - `provider`
- Service/Client 계층 실행 시간 AOP를 추가했다.
- 예외 발생 시 실행 시간, exception type, 마스킹된 message를 남기도록 했다.
- 푸시 토큰, provider credential, payload, 전화번호, 이메일 마스킹 유틸을 추가했다.
- 요청 필터, 마스킹, AOP 단위 테스트를 추가했다.

## 변경 파일

- `build.gradle.kts`
  - AOP 의존성 추가
- `src/main/kotlin/com/togethertrip/notification/global/logging/NotificationLoggingContext.kt`
  - 알림 로그 컨텍스트 키 관리
- `src/main/kotlin/com/togethertrip/notification/global/logging/RequestLoggingFilter.kt`
  - 요청 단위 requestId와 완료 로그 처리
- `src/main/kotlin/com/togethertrip/notification/global/logging/ServiceLoggingAspect.kt`
  - Service/Client 실행 시간 로그 처리
- `src/main/kotlin/com/togethertrip/notification/global/logging/SensitiveDataMasker.kt`
  - 푸시 토큰, payload, credential 마스킹
- `src/test/kotlin/com/togethertrip/notification/global/logging/*`
  - 로깅 단위 테스트

## 테스트

```bash
./gradlew test
```

검증 결과:

- 전체 테스트 통과

## 위험과 확인 사항

- 알림 payload에는 개인정보가 포함될 수 있으므로 전문 로그를 금지한다.
- 실제 provider client가 생기면 provider error code, target count 중심으로 로그를 보강해야 한다.
- 알림 대상 검증과 자기 자신 제외 로그는 실제 정책 구현 시 별도 분기로 보강한다.

## 관련 이슈

- https://github.com/together-trip/togethertrip-server-notification/issues/6
