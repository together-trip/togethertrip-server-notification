# Work Plan

## 작업

이슈 #6 `feat: 알림 서버 공통 로깅 AOP 구현`을 진행한다.

- GitHub Issue: https://github.com/together-trip/togethertrip-server-notification/issues/6

알림 서버 특성에 맞춰 HTTP 요청 로그, 알림/provider 확장용 로그 컨텍스트, Service/Client 실행 시간 AOP, 민감정보 마스킹을 구현한다.

## 배경

`notification` 서버는 알림함 조회/읽음 처리와 푸시 알림 발송을 담당할 서버다.

향후 `main -> notification`, `chat -> notification`, `app -> gateway -> notification` 흐름이 붙으면 알림 생성, 대상 검증, provider 호출 실패를 구분해서 추적할 수 있어야 한다.

운영 중에는 다음 정보를 일관되게 확인할 수 있어야 한다.

- 어떤 알림 이벤트가 처리되었는지
- 어떤 대상 사용자에게 알림이 생성 또는 제외되었는지
- 푸시 provider 호출이 성공했는지 실패했는지
- 푸시 토큰, provider credential, payload 개인정보가 로그에 노출되지 않았는지

## 범위

- HTTP 요청 단위 `X-Request-Id` 생성/유지 로직을 추가한다.
- 요청별 requestId를 응답 헤더에 포함한다.
- MDC 기반 로그 컨텍스트를 추가한다.
- 알림/provider 확장을 고려해 `notificationId`, `eventType`, `targetUserId`, `provider` 로그 키를 둔다.
- Service/Client 계층 실행 시간 AOP를 추가한다.
- 정상 처리, 예외 처리 로그를 구분한다.
- 푸시 토큰, provider credential, payload, 전화번호, 이메일을 마스킹한다.
- 관련 단위 테스트를 추가한다.

## 제외 범위

- 실제 알림 도메인 기능 구현.
- FCM/APNs provider 연동 구현.
- 알림 대상 검증 정책 구현.
- 자기 자신 알림 제외 정책 구현.
- push retry/backoff 정책 구현.
- 외부 관측 도구 연동.

## 설계

- `global/logging` 패키지에 로깅 공통 컴포넌트를 둔다.
- `RequestLoggingFilter`가 requestId 생성/유지, 응답 헤더 설정, 요청 완료 로그를 담당한다.
- `NotificationLoggingContext`가 `requestId`, `userId`, `notificationId`, `eventType`, `targetUserId`, `provider` 키를 관리한다.
- `ServiceLoggingAspect`는 `com.togethertrip.notification..service..*`, `com.togethertrip.notification..client..*`를 대상으로 한다.
- AOP는 실행 시간과 예외 요약을 남기고, argument 상세는 `DEBUG`에서만 제한적으로 요약한다.
- `SensitiveDataMasker`는 푸시 토큰, credential, payload 상세값을 원문 그대로 남기지 않는다.

## 테스트 계획

- requestId가 없는 요청은 새 requestId를 생성하는지 검증한다.
- requestId가 있는 요청은 기존 값을 유지하는지 검증한다.
- 요청 종료 후 MDC가 정리되는지 검증한다.
- Service/Client AOP가 정상 결과를 그대로 반환하는지 검증한다.
- Service/Client AOP가 예외를 삼키지 않고 다시 던지는지 검증한다.
- 푸시 토큰, provider credential, payload, 이메일이 마스킹되는지 검증한다.
- 최종 검증 명령은 `./gradlew test`다.

## 위험과 확인 사항

- 알림 payload에는 개인정보가 포함될 수 있으므로 전문 로그를 금지한다.
- Provider client가 실제로 붙으면 provider error code, target count 중심으로 로그를 보강해야 한다.
- 알림 대상 검증과 자기 자신 제외 로그는 실제 정책 구현 시 별도 분기로 보강한다.
