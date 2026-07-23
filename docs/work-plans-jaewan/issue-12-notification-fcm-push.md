# Work Plan

## 작업

Issue #12 `feat: 알림함 API와 FCM push 연동 구현`

## 배경

Issue #10은 `main` outbox SQS 메시지를 소비해 `notifications` 데이터를 생성하는 단계까지 완료했다. 앱에서 실제 알림을 사용하려면 사용자별 알림함 API, FCM token 관리, payload 표시 매핑, FCM provider 발송, 실패 처리가 이어져야 한다.

## 범위

- 사용자별 알림 목록 조회와 읽음 처리 API
- `X-User-Id` 헤더 기반 본인 알림 접근 제한
- FCM push token 등록/갱신/비활성화 API
- `eventType`, `aggregateType`, `payload` 기반 title/body/deeplink 매핑
- `PushNotificationSender` 포트와 no-op/FCM HTTP adapter
- FCM 미설정 환경 no-op 동작
- 알림 생성 후 push 발송 연결
- invalid token 비활성화와 delivery attempt 기록
- 필요한 운영 환경변수와 후속 작업을 PR에 명시

## 제외 범위

- APNs provider 발송
- `main` 서버 outbox/SQS 발행 구현
- Terraform SQS 리소스 변경
- FCM service account 기반 access token 자동 발급/회전

## 설계

- 알림 API는 `X-User-Id` 헤더로 현재 사용자를 식별한다. gateway/main 인증 연동 시 해당 헤더 주입이 필요하다.
- `NotificationPayloadMapper`가 outbox payload를 표시용 title/body/deeplink로 변환한다.
- push token은 token 원문을 응답하지 않고, 같은 token 재등록 시 기존 행을 갱신한다.
- `PushNotificationSender` 포트 뒤에 no-op sender와 FCM HTTP v1 sender를 둔다.
- FCM provider 실패는 알림 데이터 생성을 롤백하지 않고 `push_delivery_attempts`에 기록한다.
- invalid token 응답은 token을 비활성화한다.

## 테스트 계획

- 알림함 조회/읽음 처리 본인 접근 테스트
- push token 중복 등록/비활성화 테스트
- payload 매핑 테스트
- invalid token 처리와 delivery attempt 기록 테스트
- `./gradlew test`
- `./gradlew clean build`

## 위험과 확인 사항

- 운영 FCM 발송을 켜려면 `FCM_ENABLED`, `FCM_PROJECT_ID`, `FCM_CLIENT_ID`, `FCM_CLIENT_EMAIL`, `FCM_PRIVATE_KEY`, `FCM_PRIVATE_KEY_ID`, `FCM_TOKEN_URI`를 배포 환경에 추가해야 한다.
- notification 서버는 service account 값으로 FCM OAuth2 access token을 자동 발급/갱신한다.
- `FCM_ACCESS_TOKEN`은 초기 수동 검증용 fallback으로만 사용한다.
- gateway가 notification으로 전달하는 요청에는 인증된 사용자 ID를 `X-User-Id`로 주입해야 한다.
- FCM token 원문은 API 응답과 로그에 노출하지 않는다.
