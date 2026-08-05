# 이슈 #6 공통 로깅 복구 계획

## 작업

과거 PR #7에서 구현됐지만 현재 브랜치에서 유실된 HTTP 요청 로깅과 서비스 실행 시간 AOP를 최신 notification 코드에 복구한다.

- GitHub Issue: https://github.com/together-trip/togethertrip-server-notification/issues/6
- 참고 커밋: `c807d33`
- 기준 커밋: 마지막 `develop` 커밋 `59f2010`

원격 `develop` 브랜치는 2026-08-05 확인 시 삭제되어 있었다. 최신 `main`의 트리가 마지막 `develop`과 동일함을 확인하고 마지막 `develop` 커밋에서 작업 브랜치를 생성했다.

## 배경

HTTP 요청과 알림 처리 실패를 동일한 `requestId`로 추적하고, 서비스 및 외부 provider 호출의 지연과 실패 유형을 확인할 공통 관측성이 필요하다. 알림 본문, FCM 토큰, provider credential과 요청 본문은 개인정보 또는 인증정보를 포함할 수 있으므로 로그에 기록하지 않는다.

## 범위

- 안전한 `X-Request-Id`를 유지하거나 새로 생성하고 응답 헤더로 반환한다.
- 요청 처리 중 `requestId`, 선택적 사용자·알림·이벤트·provider 식별자를 MDC에 둔다.
- HTTP method, URI path, status, 실행 시간, 예외 유형만 구조화해 기록한다.
- notification 서비스와 push/provider 경계의 메서드명, 실행 시간, 예외 유형을 AOP로 기록한다.
- 토큰·credential·본문 등 민감 문자열을 방어적으로 마스킹하는 유틸리티를 제공한다.
- 단위 테스트와 실제 필터/AOP 프록시를 거치는 통합 성격 테스트를 추가한다.

## 제외 범위

- 로그 수집기·APM·분산 추적 도구 연동
- 요청 또는 응답 본문 로깅
- query string 로깅
- 알림 payload, 메서드 인자, 반환값, 예외 메시지 로깅
- SQS 메시지에 requestId를 전파하는 별도 계약 변경

## 설계

- `RequestLoggingFilter`는 외부 request ID를 허용 문자와 최대 길이로 검증한다. 유효하지 않은 값은 로그에 사용하지 않고 UUID로 교체한다.
- 필터는 기존 MDC를 보존하고 요청 종료 시 원래 상태로 복구해 다른 필터나 테스트 실행 컨텍스트를 훼손하지 않는다.
- 요청 URI는 query string을 제외한 `requestURI`만 기록한다.
- `ServiceLoggingAspect`는 인자·결과·예외 메시지와 stack trace를 기록하지 않고 메서드명, 실행 시간, 예외 클래스만 기록한다.
- `SensitiveDataMasker`는 다른 로그 지점에서 불가피하게 문자열을 요약할 때 알려진 secret key, Bearer token, 이메일, 전화번호를 제거하는 방어선으로 둔다.

## 테스트 계획

- request ID 생성, 유효한 ID 유지, CR/LF·공백·과다 길이 값 교체
- 응답 헤더 설정, 처리 중 MDC 노출, 종료 후 기존 MDC 복원
- 요청 query/body와 예외 메시지가 로그에 포함되지 않음
- 정상·실패 HTTP 요청의 method/path/status/elapsed/exception type 기록
- AOP 정상 결과 보존과 예외 재던짐
- AOP 로그에 메서드 인자, 반환값, 예외 메시지가 포함되지 않음
- Spring AOP 프록시에서 advice 적용
- 최종 `./gradlew test`

## 위험과 확인 사항

- 비동기 실행과 SQS 소비에는 HTTP MDC가 자동 전파되지 않는다. 비동기 메시지 추적 ID 계약은 별도 작업이 필요하다.
- URI path 자체에 개인정보를 넣는 API가 추가되면 route template 기반 로깅으로 강화해야 한다.
- 예외 stack trace를 공통 AOP에서 남기지 않으므로 세부 원인 분석은 requestId와 도메인별 안전한 로그·지표를 함께 사용한다.
