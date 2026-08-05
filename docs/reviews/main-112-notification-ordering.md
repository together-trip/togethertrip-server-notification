# Main #112 notification 순서 보정 리뷰

## 요약

알림 목록을 소비 시각에서 실제 발생 시각 중심으로 전환하고, 역순 소비와 중복 재전달을 통합 테스트로 고정했다. SQS event ID correlation은 공통 로깅의 안전한 MDC scope로 연결했다.

최종 변경에서 차단할 버그나 보안 발견 사항은 없다.

## 발견 사항

| 심각도 | 파일 | 내용 | 조치 |
| --- | --- | --- | --- |
| 높음(수정됨) | `NotificationRepository.kt` | 기존 `createdAt DESC`는 메시지 역순 소비를 사용자 알림함 순서로 노출했다. | `COALESCE(occurredAt, createdAt) DESC`, `sourceEventId DESC`, `id DESC`로 변경했다. |
| 보통(수정됨) | `NotificationMessageConsumer.kt` | SQS 처리 로그와 service AOP 로그를 동일 event로 연결할 correlation ID가 없었다. | 파싱 전 SQS ID, 파싱 후 outbox event ID를 requestId로 사용하고 scope 종료 시 MDC를 복원했다. |
| 보통(수정됨) | `NotificationMessageConsumer.kt` | 실패 로그의 throwable은 Jackson 예외 메시지 등을 통해 payload 일부를 노출할 수 있었다. | correlation ID와 예외 클래스만 기록하고 메시지·stack trace를 제외했다. |
| 보통(문서화) | push 경계 | notification 즉시 dispatch와 standard queue로는 역순 push를 막을 수 없다. | main aggregate enqueue 순서와 infra FIFO `MessageGroupId`가 함께 필요한 계약을 ADR에 기록했다. |
| 낮음(수정됨) | `NotificationLoggingContext.kt` | 메시지 scope에서 이전 MDC 상세 키가 처리 중 섞일 가능성이 있었다. | scope 진입 시 MDC를 격리하고 종료 후 전체 이전 context를 복원했다. |

## 보안·도메인 확인

- 알림 payload, recipient 목록, receipt handle과 push token을 MDC에 기록하지 않는다.
- correlation ID는 공통 sanitizer를 거쳐 줄바꿈·과다 길이 로그 인젝션을 차단한다.
- 자기 자신 알림 제외와 deleted account filtering 로직은 변경하지 않았다.
- `(source_event_id, recipient_user_id)` unique key와 재전달 선조회가 유지된다.
- 정렬 변경은 조회와 mark-all 대상 선택에만 적용되며 읽음·soft delete 권한은 유지된다.
- notification 저장소는 main/infra 순서 설정을 추정하거나 변경하지 않는다.

## 확인한 명령

```bash
./gradlew test --tests 'com.togethertrip.notification.notification.service.NotificationServiceTest' \
  --tests 'com.togethertrip.notification.notification.service.CreateNotificationFromOutboxUseCaseTest' \
  --tests 'com.togethertrip.notification.notification.service.NotificationMessageConsumerTest'
./gradlew test
git diff --check origin/develop..HEAD
```

## 남은 위험

- 운영 PostgreSQL의 데이터 분포에서 `COALESCE` 정렬 비용을 확인해야 한다. 현재 최근 100개 제한과 recipient 조건이 있지만 전용 expression index는 추가하지 않았다.
- `occurredAt` 누락 이벤트는 소비 시각 fallback이므로 실제 순서를 복원하지 못한다.
- push 순서 보장은 main과 infra 후속 구현·검증 전까지 제공되지 않는다.
