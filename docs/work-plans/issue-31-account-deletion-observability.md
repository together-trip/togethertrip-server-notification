# Work Plan: 이슈 #31 계정 삭제 소비 관측성 보강

## 범위

- `USER_ACCOUNT_DELETED` 메시지 처리 결과를 삭제 완료, 중복 no-op, 실패로 구분해 metric으로 기록한다.
- 성공 로그에 `sourceEventId`, `userId`, 처리 결과를 남겨 운영 추적성을 높인다.
- 실패 메시지는 기존처럼 acknowledge하지 않아 SQS visibility timeout과 DLQ 정책을 따른다.
- 기존 트랜잭션 기반 알림·토큰·발송 이력 hard delete와 tombstone 멱등성은 유지한다.

## 제외 범위

- SQS queue와 DLQ 인프라 프로비저닝
- main의 계정 삭제 이벤트 발행 방식 변경
- 계정 삭제 데이터 정책 변경

## 아키텍처 판단

- 메시지 처리 결과를 가장 정확히 아는 `NotificationMessageConsumer`에서 outcome metric을 기록한다.
- metric 구현은 interface로 분리하고 Micrometer adapter를 둬 consumer가 관측 도구에 직접 결합되지 않게 한다.
- DB 삭제는 기존 `AccountDeletionService`의 단일 트랜잭션을 유지한다.

## TDD 계획

1. 삭제 완료 시 `deleted` metric과 acknowledge를 검증한다.
2. tombstone 중복 시 `duplicate` metric과 acknowledge를 검증한다.
3. 처리 예외 시 `failed` metric을 기록하고 acknowledge하지 않는지 검증한다.
4. 기존 H2 PostgreSQL mode 통합 테스트로 알림·토큰·발송 이력 삭제와 tombstone을 검증한다.

## 검증

- `./gradlew test`
- `git diff --check`

## 남은 운영 검증

- 운영 SQS redrive policy와 CloudWatch alarm은 infra 환경에서 확인한다.
- metric exporter와 dashboard 연결은 배포 환경 설정에서 확인한다.
