# Verification Report

## 검증 대상

Issue #10 `feat: main outbox SQS 알림 이벤트 소비 구현`

## 실행한 명령

```bash
./gradlew test
./gradlew clean build
```

## 결과

- `./gradlew test`: 성공
- `./gradlew clean build`: 성공
- AWS SDK mock 기반 SQS consumer 테스트로 성공 시 `deleteMessage`, 실패 시 미삭제 동작을 확인했다.
- use case 테스트로 수신자별 알림 생성과 `sourceEventId + recipientUserId` 중복 미생성을 확인했다.
- 환경변수 설정은 `main` 서버와 동일하게 `application.yml`에서 `.env` import와 `${ENV_NAME}` placeholder를 사용하도록 확인했다.

## 실패 또는 미검증 항목

- 실제 LocalStack 큐를 띄워 메시지를 주입하는 수동 검증은 수행하지 않았다. 이 이슈 범위의 자동화 테스트는 AWS SDK mock 방식으로 대체했다.
- FCM/APNs provider 발송, 앱 알림함 조회/읽음 API는 이슈 제외 범위라 검증하지 않았다.

## 다음 조치

- 후속 알림함 API 구현 시 `payloadSnapshot`에서 사용자에게 보여줄 title/body/deeplink 매핑 정책을 확정한다.
