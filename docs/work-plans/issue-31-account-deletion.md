# Work Plan: 이슈 #31 회원 탈퇴 알림 데이터 정리

## 작업

main의 `USER_ACCOUNT_DELETED` v1 이벤트를 소비해 탈퇴 사용자의 알림·푸시 데이터를 멱등 삭제한다.

## 배경

현재 push token 비활성화는 앱의 현재 기기 요청에 의존하며, 사용자별 알림 본문과 발송 이력은
main 계정 삭제 뒤에도 notification DB에 남는다.

## 범위

- 계정 삭제 이벤트 계약 검증과 전용 consumer 분기를 추가한다.
- 사용자의 모든 push token, notification, 관련 delivery attempt를 hard delete한다.
- 처리한 사용자와 source event를 tombstone으로 기록해 중복 이벤트를 멱등 처리한다.
- tombstone 사용자는 지연된 일반 알림 생성과 push token 재등록을 차단한다.
- 실패 메시지는 ack하지 않아 기존 queue 재시도·DLQ 정책을 따른다.
- 서비스·consumer·repository 통합 테스트와 운영 계약 문서를 추가한다.

## 제외 범위

- main 사용자 데이터 삭제와 Flutter UI는 수정하지 않는다.
- 보관 근거가 정의되지 않은 provider message ID나 payload snapshot은 유지하지 않는다.

## 설계

- `DeletedAccount`는 `user_id`, `source_event_id`, `deleted_at`을 유일 키로 보관한다.
- `AccountDeletionService`가 한 트랜잭션에서 연관 발송 이력, 알림, 토큰을 순서대로 삭제하고
  tombstone을 저장한다.
- `NotificationMessageConsumer`는 lifecycle 이벤트만 전용 use case로 보내고 나머지는 기존 흐름을 유지한다.
- 일반 알림 생성은 recipient 중 tombstone 사용자를 제거한다.

## 테스트 계획

- 모든 토큰·알림·발송 이력이 제거되는지 검증한다.
- 동일 source event 및 동일 user의 중복 이벤트가 no-op인지 검증한다.
- 삭제 사용자에게 지연 알림이 생성되지 않고 토큰 재등록이 거부되는지 검증한다.
- 잘못된 payload는 ack되지 않는지 검증한다.
- `./gradlew test`를 실행한다.

## 위험과 확인 사항

- tombstone은 서비스 간 지연 이벤트 차단에 필요한 최소 내부 식별자만 보관한다.
- main과 notification의 event type·version·payload 필드 테스트를 동일하게 유지한다.

## 구현 상태

- `USER_ACCOUNT_DELETED` v1 전용 consumer 분기와 payload 검증을 구현했다.
- 알림·token·delivery attempt hard delete와 tombstone 멱등 처리를 구현했다.
- 삭제 사용자 지연 알림 및 push token 재등록 차단을 구현했다.
- `./gradlew test` 전체 검증을 통과했다.
