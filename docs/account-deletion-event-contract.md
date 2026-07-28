# 회원 탈퇴 알림 데이터 정리 계약

notification은 main의 `USER_ACCOUNT_DELETED` v1 이벤트를 수신하면 다음을 한 트랜잭션에서 처리한다.

1. `user_id`, `source_event_id`, `deleted_at` tombstone 저장
2. 사용자 notification 또는 push token에 연결된 delivery attempt hard delete
3. 사용자 notification hard delete
4. 사용자 push token hard delete

같은 사용자 또는 같은 `source_event_id`의 후속 이벤트는 no-op이다. 잘못된 event version, `aggregateType`,
`userId`, `occurredAt`은 예외로 처리하며 SQS message를 acknowledge하지 않는다.

tombstone 사용자는 지연된 일반 알림의 recipients에서 제외하고 push token 재등록을 `410 ACCOUNT_DELETED`로
거부한다. tombstone에는 외부 provider ID, token, 알림 본문 등 개인정보를 저장하지 않는다.
