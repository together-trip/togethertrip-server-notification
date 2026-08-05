# 이슈 #6 공통 로깅 복구 리뷰

## 요약

과거 PR #7의 기능을 현재 notification 구조에 복구하되, 공통 로그가 요청·응답 본문, query string, 메서드 인자·반환값, 예외 메시지·stack trace를 기록하지 않도록 범위를 축소했다. 외부 `X-Request-Id`는 허용 문자와 길이를 검증하고 HTTP/AOP 로그 필드에 명시적으로 포함한다.

최종 diff 기준 차단할 버그나 보안 발견 사항은 없다.

## 발견 사항

| 심각도 | 파일 | 내용 | 조치 |
| --- | --- | --- | --- |
| 높음(수정됨) | `ServiceLoggingAspect.kt` | 초기 package pointcut이 `FcmPushProperties` 같은 final 설정 객체까지 프록시해 애플리케이션 컨텍스트 시작을 실패시켰다. | pointcut을 `@Service`와 `PushNotificationSender.send`로 제한하고 interface 기반 proxy 설정을 추가했다. 전체 context 테스트로 회귀를 확인했다. |
| 보통(수정됨) | `RequestLoggingFilter.kt`, `ServiceLoggingAspect.kt` | requestId를 MDC에만 두면 기본 로그 포맷이나 수집기 설정에 따라 출력되지 않을 수 있었다. | 모든 공통 HTTP/AOP 로그에 `requestId={}` 필드를 명시적으로 추가했다. |
| 보통(수정됨) | `RequestLoggingFilter.kt` | 과거 구현은 외부 request ID를 길이만 제한해 CR/LF 또는 공백 기반 로그 인젝션 여지가 있었다. | `[A-Za-z0-9._:-]{1,100}`만 수용하고 나머지는 새 UUID로 교체했다. |
| 보통(수정됨) | `RequestLoggingFilter.kt`, `ServiceLoggingAspect.kt` | 과거 구현은 query, argument 요약, 예외 메시지와 throwable을 기록해 토큰·알림 본문이 노출될 수 있었다. | path와 실행 메타데이터·예외 클래스만 기록하고 데이터 내용은 전부 제외했다. |

## 보안 확인

- HTTP request/response body와 query string을 읽거나 기록하지 않는다.
- 서비스 인자·반환값과 예외 메시지를 기록하지 않는다.
- FCM token, credential, 알림 title/body, SQS payload를 AOP 로그에 전달하지 않는다.
- requestId와 MDC 확장 값은 줄바꿈·구분자와 과도한 길이를 제거한다.
- 기존 MDC는 요청 종료 후 복원해 다른 필터의 context를 파괴하지 않는다.
- `SensitiveDataMasker`는 명시적으로 문자열을 기록해야 하는 다른 안전한 로그 지점의 보조 방어선이며, 본문 로깅 허용 수단이 아니다.

## 확인한 명령

```bash
./gradlew test --tests 'com.togethertrip.notification.global.logging.*' \
  --tests 'com.togethertrip.notification.notification.service.ServiceLoggingAspectIntegrationTest'
./gradlew test
git diff --check 59f2010..HEAD
```

## 남은 위험

- SQS 소비와 scheduler 실행에는 HTTP requestId가 없으므로 AOP 로그에는 `requestId=none`이 기록된다. 메시지 단위 correlation ID 계약은 별도 이슈가 필요하다.
- 향후 개인정보가 URI path segment에 들어가는 API가 생기면 raw `requestURI` 대신 route template 또는 정규화된 path를 기록해야 한다.
- 공통 AOP는 의도적으로 stack trace를 기록하지 않는다. 도메인별 예외 원인 관측은 민감정보가 제거된 전용 로그와 지표를 사용해야 한다.
