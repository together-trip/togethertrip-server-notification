# 이슈 #6 공통 로깅 복구 검증

## 검증 대상

- HTTP `X-Request-Id` 생성·유지·응답 반환
- MDC request/user 및 notification 확장 context
- HTTP method/path/status/실행 시간/예외 유형 로그
- 서비스와 push provider 경계 실행 시간 AOP
- query/body/token/credential/인자/반환값/예외 메시지 비노출
- Spring 애플리케이션 컨텍스트와 기존 notification 기능 회귀

## 실행한 명령

```bash
./gradlew compileKotlin
./gradlew test --tests 'com.togethertrip.notification.global.logging.*' \
  --tests 'com.togethertrip.notification.notification.service.ServiceLoggingAspectIntegrationTest'
./gradlew test
git diff --check 59f2010..HEAD
```

## 결과

- Kotlin main/test compilation 성공
- 신규 로깅 targeted 테스트 10개 성공
- 전체 테스트 53개 성공, 실패 0개, 오류 0개
- 실제 Spring 애플리케이션 컨텍스트 시작 성공
- JPA·알림·push token·FCM·SQS·계정 삭제 기존 테스트 회귀 없음
- whitespace 오류 없음
- 로그 캡처 테스트에서 query secret, 요청 body, 메서드 인자·반환값, 예외 메시지가 출력되지 않음을 확인
- 안전하지 않은 request ID가 새 UUID로 교체되고 기존 MDC가 요청 종료 후 복원됨을 확인

## 실패 또는 미검증 항목

- 초기 전체 테스트에서 FCM 설정 객체를 과도하게 프록시해 19개 context 기반 테스트가 실패했다. pointcut 축소와 interface proxy 설정 후 전체 53개가 성공했다.
- 외부 로그 수집기/APM 연동은 범위에서 제외했다.
- 실제 운영 로그 포맷·수집·검색 대시보드는 배포 후 확인이 필요하다.
- SQS 메시지 correlation ID 전파는 구현하지 않았다.

## 다음 조치

1. PR/배포 후 HTTP 요청의 응답 `X-Request-Id`와 수집 로그의 동일 requestId 검색을 확인한다.
2. 운영 로그 수집기의 MDC 필드 수집 여부와 무관하게 명시적 `requestId` 필드가 검색되는지 확인한다.
3. SQS 메시지 계약에 correlation ID를 추가할 필요가 있으면 별도 이슈로 진행한다.
