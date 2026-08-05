# Main #112 notification 순서 보정 검증

## 검증 대상

- `occurredAt DESC` 알림함 정렬
- 동일 발생 시각 deterministic tie-breaker
- 누락 발생 시각 `createdAt` fallback
- 메시지 역순 소비와 중복 재전달 멱등성
- SQS outbox event ID correlation과 MDC 복원
- 기존 알림·push·계정 삭제 기능 회귀

## 실행한 명령

```bash
./gradlew compileKotlin
./gradlew test --tests 'com.togethertrip.notification.notification.service.NotificationServiceTest' \
  --tests 'com.togethertrip.notification.notification.service.CreateNotificationFromOutboxUseCaseTest' \
  --tests 'com.togethertrip.notification.notification.service.NotificationMessageConsumerTest'
./gradlew test
git diff --check origin/develop..HEAD
```

## 결과

- Kotlin main/test compilation 성공
- 정렬·소비 targeted 테스트 성공
- 전체 테스트 58개 성공, 실패 0개, 오류 0개
- 최신 이벤트를 먼저 소비하고 오래된 이벤트를 나중에 소비해도 조회 결과는 `occurredAt DESC`
- 동일 `occurredAt`에서 `sourceEventId DESC` 순서 확인
- null `occurredAt`에서 `createdAt DESC` fallback 확인
- 같은 최신 메시지 재전달 시 `createdCount=0`, 사용자 알림 총 2개 유지
- SQS 처리 중 `requestId=outbox:201`, event type MDC 확인 및 기존 MDC 복원 확인
- H2 JPA 통합 테스트에서 `COALESCE` JPQL query와 pagination 실행 성공
- whitespace 오류 없음

## 실패 또는 미검증 항목

- 실제 SQS standard queue의 역순 전달은 로컬 fake queue로 재현했고 AWS 환경에서는 실행하지 않았다.
- FIFO queue, `MessageGroupId`, main aggregate claim 순서와 push 역순 방지는 notification 범위 밖이라 검증하지 않았다.
- 운영 PostgreSQL query plan과 대규모 사용자 알림 데이터 성능은 미검증이다.

## 다음 조치

1. main에서 aggregate별 enqueue 순서와 안정적인 sequence/group metadata를 결정한다.
2. infra에서 push 순서 보장 대상 queue의 FIFO 전환과 DLQ·head-of-line blocking 운영 정책을 검증한다.
3. 운영 데이터에서 알림 조회 query plan을 확인하고 필요하면 recipient + ordering expression index를 별도 이슈로 추가한다.
