# ADR: 알림함 발생 순서와 push 순서 보장 경계

## 상태

채택 — 2026-08-05

## 맥락

main outbox의 `SKIP LOCKED` 병렬 claim은 잠긴 오래된 이벤트를 기다리지 않고 뒤 이벤트를 발행할 수 있다. SQS standard queue도 순서를 보장하지 않는다. notification이 메시지를 받은 시각에 생성되는 `createdAt`으로 알림함을 정렬하면 실제 이벤트 발생 순서와 화면 순서가 달라진다.

notification은 payload의 `occurredAt`을 저장하므로 알림함 조회 순서는 보정할 수 있다. 그러나 push는 각 메시지를 소비한 직후 발송하며 선행 이벤트 sequence나 보류 상태를 저장하지 않는다.

## 결정

### 알림함

사용자 알림 목록은 다음 키로 정렬한다.

```text
COALESCE(occurredAt, createdAt) DESC
sourceEventId DESC
notificationId DESC
```

- `occurredAt`: 실제 도메인 이벤트 발생 시각이며 기본 정렬 기준이다.
- `createdAt`: 발생 시각이 누락되거나 파싱 불가능한 레거시·비정상 메시지의 fallback이다.
- `sourceEventId`: 같은 발생 시각에서 main outbox 생성 순서를 결정한다.
- `notificationId`: 완전한 deterministic ordering을 위한 마지막 키다.

기존 `(source_event_id, recipient_user_id)` unique key와 재전달 선조회는 유지한다.

### SQS correlation

JSON 파싱 전에는 `sqs:<messageId>`, 파싱 후에는 `outbox:<eventId>`를 MDC `requestId`로 사용한다. event type만 추가 context로 사용하며 payload, receipt handle, push token과 recipient 목록은 넣지 않는다. 메시지 처리가 끝나면 이전 MDC를 복원한다.

### push

현재 notification 구현과 SQS standard queue 조합은 push 순서를 보장하지 않는다. push 순서가 필요한 aggregate에는 다음 조건이 모두 필요하다.

1. main이 같은 aggregate의 outbox 이벤트를 발생 순서대로 enqueue한다.
2. infra가 FIFO queue를 사용한다.
3. producer가 동일 aggregate에 일관된 `MessageGroupId`를 설정한다.
4. deduplication ID는 event ID처럼 재전달에도 안정적인 값을 사용한다.

FIFO는 enqueue된 순서만 유지하므로 main이 역순으로 enqueue한 메시지를 복원하지 못한다. 현재 한 메시지에 여러 recipient가 들어 있으므로 aggregate 단위 group을 사용한다. `recipient + aggregate` 단위가 필요하면 producer가 메시지를 수신자별로 분리해야 한다.

## 결과

- 역순 소비돼도 알림함은 실제 발생 순서로 보인다.
- 동일 시각에도 pagination과 반복 조회 순서가 안정적이다.
- 중복 재전달은 새 inbox row나 push dispatch를 만들지 않는다.
- push 보장 책임은 main claim/enqueue와 infra FIFO 구성까지 이어지는 교차 저장소 계약으로 남는다.

## 한계와 운영 고려

- `occurredAt`이 없는 이벤트의 실제 순서는 복원할 수 없다. fallback `createdAt`은 소비 순서이므로 정상 이벤트보다 최근으로 보일 수 있다.
- source event ID의 시간 순서는 main outbox ID가 증가한다는 현재 계약에 의존한다.
- FIFO group의 오래된 메시지가 반복 실패하면 후속 메시지가 지연되는 head-of-line blocking이 발생한다.
- notification 단독 buffering은 선행 이벤트의 존재·최종 실패 여부를 알 수 없어 채택하지 않았다.
