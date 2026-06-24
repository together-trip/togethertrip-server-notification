# Work Plan

## 작업

Issue #10 `feat: main outbox SQS 알림 이벤트 소비 구현`

## 배경

`main` 서버는 알림 후보를 `outbox_events`에 저장하고 SQS 발행 성공까지만 상태를 관리한다. `notification` 서버는 SQS 메시지 소비 이후의 알림 생성, 중복 방지, 실패 시 재시도 경계를 별도로 책임진다.

## 범위

- `main` 서버와 동일한 `.env` import / 환경변수 placeholder 방식으로 `NOTIFICATION_QUEUE_URL`, `AWS_REGION` 기반 SQS consumer 설정 추가
- `main` outbox envelope 수신 DTO 정의
- `outbox event id`를 `sourceEventId`로 사용하는 알림 생성 use case 추가
- `payload.recipients[].userId`를 파싱해 수신자별 알림 데이터 생성
- `sourceEventId + recipientUserId` 유니크 제약과 use case 필터링으로 중복 생성 방지
- 처리 성공 시 SQS 메시지 삭제, 처리 실패 시 삭제하지 않는 consumer 구현
- 메시지 큐 포트를 두고 SQS 연결이 있으면 SQS adapter, 없으면 no-op adapter를 사용하는 구조 추가
- AWS SDK mock 기반 SQS 소비 테스트 추가

## 제외 범위

- FCM/APNs provider 발송
- `main` 서버 SQS sender 구현
- Terraform SQS 리소스 변경
- 앱 알림함 조회/읽음 API 구현

## 설계

- notification bounded context 안에 `Notification` entity, repository, `CreateNotificationFromOutboxUseCase`를 둔다.
- application/service 계층의 consumer는 `NotificationMessageQueue` 포트에만 의존한다.
- SQS adapter는 infrastructure 패키지에서 `SqsClient`와 queue port를 연결한다.
- `NOTIFICATION_QUEUE_URL`이 없거나 consumer가 비활성화된 경우 no-op queue adapter를 사용해 로컬/테스트 환경에서 실제 SQS 연결 없이 기동한다.
- 환경변수 설정은 `main` 서버처럼 `application.yml`에서 `optional:classpath:.env[.properties]`를 import하고 `${ENV_NAME}` placeholder를 직접 참조한다.
- payload 도메인 타입은 notification이 소유하지 않으므로 원본 `JsonNode`를 문자열 snapshot으로 저장하고, 공통 수신자 목록만 파싱한다.
- consumer는 예외를 삼키지 않고 메시지 단위로 기록한 뒤 delete를 생략해 SQS 재시도/DLQ 정책을 따른다.

## 테스트 계획

- use case: 수신자별 알림 생성
- use case: 같은 `sourceEventId + recipientUserId` 재처리 시 중복 미생성
- consumer: 성공 시 queue message acknowledge
- consumer: 처리 예외 시 queue message 미acknowledge
- SQS adapter: acknowledge 시 `deleteMessage` 호출
- 설정: SQS queue url 유무에 따른 SQS/no-op adapter 선택

## 위험과 확인 사항

- outbox payload schema가 확장되어도 `recipients.userId`만 유지되면 소비 가능하다.
- 알림 내용/딥링크 필드 매핑은 후속 알림함 API 또는 push provider 구현에서 구체화해야 한다.
- 민감정보 로그 노출을 피하기 위해 payload 본문은 실패 로그에 포함하지 않는다.
