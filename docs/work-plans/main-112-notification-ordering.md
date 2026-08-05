# Main #112 notification 알림 순서 보정 계획

## 작업

Main #112 중 notification 저장소 책임을 구현한다.

- Main Issue: https://github.com/together-trip/togethertrip-server-main/issues/112
- 기준 브랜치: notification `origin/develop`
- 기준 커밋: `4fad184` — 공통 로깅 PR #35 포함

## 배경

main outbox worker의 `SKIP LOCKED` 병렬 claim과 표준 SQS는 이벤트 발행·전달 순서를 보장하지 않는다. notification은 payload의 `occurredAt`을 저장하지만 현재 알림함은 소비 시 생성되는 `createdAt DESC`로 조회해, 역순 소비가 사용자 화면 순서에 그대로 노출된다.

알림함 표시 순서는 notification이 보정할 수 있다. 반면 push 발송은 메시지를 소비한 즉시 수행되므로, 선행 이벤트 정보나 aggregate sequence 없이 notification 단독으로 안전하게 재정렬할 수 없다.

## 범위

- 알림함을 실제 발생 시각 중심으로 정렬한다.
- `occurredAt` 누락 fallback과 동일 시각 tie-breaker를 정의한다.
- 역순 소비, 동일 시각, 누락 발생 시각, 중복 재전달을 통합 테스트한다.
- SQS 소비 중 main outbox event ID를 공통 로깅 `requestId`로 연결한다.
- push 순서의 main claim·FIFO queue 의존성과 표준 queue 한계를 문서화한다.

## 제외 범위

- main outbox claim·sender 수정
- infra SQS standard/FIFO 설정 수정
- notification 내부 push 지연 버퍼·aggregate sequence 저장
- 기존 `(source_event_id, recipient_user_id)` 멱등성 키 변경

## 설계

### 알림함 정렬

정렬 키는 다음 순서다.

1. `COALESCE(occurredAt, createdAt) DESC`
2. `sourceEventId DESC`
3. notification `id DESC`

정상 이벤트는 실제 `occurredAt` 최신순으로 표시한다. `occurredAt`이 없거나 파싱할 수 없으면 소비 시각인 `createdAt`을 fallback으로 사용한다. 동일 발생 시각에는 main outbox ID가 큰 이벤트를 먼저 표시하고, 마지막으로 notification ID를 사용해 항상 같은 순서를 반환한다.

### 멱등성

기존 `(source_event_id, recipient_user_id)` unique key와 선조회 방식을 유지한다. 같은 메시지가 재전달되면 새 inbox row와 push dispatch를 만들지 않는다.

### SQS correlation

메시지 JSON 파싱 전에는 `sqs:<messageId>`, 파싱 후에는 `outbox:<eventId>`를 안전하게 정규화해 MDC `requestId`로 사용한다. event type도 MDC에 두며 처리 종료 후 이전 MDC를 복원한다. payload, receipt handle, recipient 목록은 로그 context에 넣지 않는다.

### push 순서 경계

notification은 소비 완료 직후 push를 발송하므로 현재 표준 queue에서는 push 순서를 보장하지 않는다. 보장이 필요하면 main이 같은 aggregate의 outbox를 순서대로 enqueue하고 infra가 FIFO queue와 일관된 `MessageGroupId`를 제공해야 한다. 하나의 메시지에 여러 recipient가 있으므로 현재 메시지 구조에서는 aggregate 단위 group이 자연스럽고, recipient+aggregate 단위가 필요하면 producer가 메시지를 수신자별로 분리해야 한다.

## 테스트 계획

- 최신 이벤트가 먼저 소비되고 오래된 이벤트가 나중에 소비돼도 `occurredAt DESC` 조회
- 동일 `occurredAt`은 `sourceEventId DESC`, 필요 시 notification ID DESC
- `occurredAt` 누락은 `createdAt DESC` fallback
- 같은 메시지 재전달 시 사용자별 알림 중복 없음
- SQS 처리 중 `requestId=outbox:<eventId>`와 event type 노출, 종료 후 MDC 복원
- `./gradlew test`

## 위험과 확인 사항

- 누락된 `occurredAt`은 실제 발생 순서를 복원할 정보가 없어 소비 시각 fallback만 가능하다.
- source event ID는 main outbox에서 증가한다는 현재 계약을 tie-breaker로 사용한다.
- FIFO도 producer가 역순 enqueue하면 순서를 복원하지 못한다. main claim 순서와 queue 설정을 함께 충족해야 한다.
- FIFO message group의 오래된 메시지가 반복 실패하면 같은 group의 후속 push가 지연될 수 있다.
