# Issue #19 알림 FCM 흐름 리뷰

## 1. main 서버가 SQS에 편지를 넣는다

`main` 서버는 여행 초대, 게시글 작성, 정산 확정 같은 일이 생기면 알림용 이벤트를 만든다.

이 이벤트는 바로 휴대폰으로 가는 것이 아니다. 먼저 SQS라는 줄 서는 공간에 들어간다. 쉽게 말하면 `main` 서버가 “이 사람들에게 알림을 보내 주세요”라고 쓴 편지를 우체통에 넣는 단계다.

`notification` 서버는 이 편지를 직접 만들지 않는다. `notification` 서버는 SQS 우체통에서 편지를 꺼내 읽는 역할이다.

## 2. notification 서버가 SQS에 연결할 준비를 한다

`SqsNotificationConsumerConfig`가 SQS에 연결할 객체를 만든다.

가장 먼저 보는 값은 `notification.sqs.queue-url`이다. 이 값은 환경변수 `NOTIFICATION_QUEUE_URL`에서 온다.

queue URL이 비어 있으면 실제 SQS에 붙지 않는다. 이때는 `NoopNotificationMessageQueue`를 사용한다. 쉽게 말하면 “우체통 주소가 없으니 실제 우체통은 보러 가지 말자”는 상태다.

queue URL이 있으면 `SqsClient`를 만든다. 이때 AWS region은 `AWS_REGION`에서 읽는다.

이번 변경에서 중요한 부분은 AWS 자격증명이다.

notification 서버는 다음 값을 읽을 수 있다.

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN`

`AWS_ACCESS_KEY_ID`와 `AWS_SECRET_ACCESS_KEY`가 둘 다 있으면, 서버가 그 값을 직접 사용해서 SQS에 연결한다.

`AWS_SESSION_TOKEN`까지 있으면 임시 자격증명으로 연결한다. 보통 STS나 임시 토큰을 쓰는 상황에서 필요하다.

반대로 access key와 secret key가 비어 있으면, 서버는 억지로 특정 profile을 고르지 않는다. AWS SDK 기본 credential chain에 맡긴다. 운영 환경에서는 보통 IAM role 같은 방식이 여기에 해당한다.

즉, 현재 기준은 이렇다.

- 로컬 Docker 테스트: env로 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`를 넣으면 그 값을 사용한다.
- 운영 환경: env가 없으면 AWS SDK 기본 방식으로 자격증명을 찾는다.
- `AWS_PROFILE`: 앱에서 직접 선택하지 않는다.

## 3. notification 서버가 SQS에서 편지를 꺼낸다

`NotificationMessageConsumer`가 정해진 시간마다 SQS를 확인한다.

코드 흐름은 이렇다.

1. `NotificationMessageConsumer.poll()`이 실행된다.
2. `NotificationMessageQueue.receive()`로 SQS 메시지를 가져온다.
3. 메시지가 있으면 하나씩 `handle()`로 넘긴다.
4. 메시지 body를 `MainOutboxEventMessage`로 읽는다.

여기서 실패하면 SQS 메시지를 지우지 않는다. 그래야 나중에 다시 처리할 수 있다.

## 4. 메시지 안에서 받을 사람 목록을 찾는다

`MainOutboxEventMessage`는 payload 안의 `recipients`를 읽는다.

예를 들면 이런 모양이다.

```json
{
  "recipients": [
    { "userId": 1 },
    { "userId": 2 }
  ],
  "tripId": 10,
  "tripName": "제주 여행"
}
```

`recipientUserIds()`는 여기서 `1`, `2`를 꺼낸다. 같은 사람이 두 번 들어오면 한 번만 남긴다.

받을 사람이 없으면 알림을 만들지 않는다. 빈 편지를 굳이 저장하지 않는 것이다.

## 5. 이미 만든 알림인지 먼저 확인한다

`CreateNotificationFromOutboxUseCase`는 먼저 DB를 본다.

같은 `sourceEventId`와 같은 `recipientUserId` 조합의 알림이 이미 있으면 다시 만들지 않는다.

이유는 SQS 메시지가 상황에 따라 두 번 배달될 수 있기 때문이다. 같은 편지가 두 번 와도 사용자 알림함에 똑같은 알림이 두 개 생기면 안 된다.

## 6. 이벤트 내용을 사람이 읽을 문장으로 바꾼다

`NotificationPayloadMapper`가 이벤트를 앱에 보여줄 문장으로 바꾼다.

예를 들어 `POST_CREATED` 이벤트가 오면 다음처럼 바꾼다.

- title: 여행 이름
- body: 누가 어떤 게시글을 썼는지
- deeplink: 앱에서 이동할 주소

즉, 서버끼리 주고받는 딱딱한 데이터인 `POST_CREATED`를 사용자가 읽는 “지우님이 첫 일정을 작성했습니다.” 같은 문장으로 바꾸는 단계다.

알 수 없는 이벤트가 와도 서버가 터지지 않도록 기본 문구를 쓴다.

## 7. 알림함에 알림을 저장한다

문장이 준비되면 `notifications` 테이블에 저장한다.

저장되는 핵심 값은 다음과 같다.

- `sourceEventId`: main 서버 outbox 이벤트 ID
- `recipientUserId`: 알림을 받을 사용자 ID
- `eventType`: 어떤 종류의 알림인지
- `payloadSnapshot`: 원본 payload 복사본
- `title`: 앱에 보일 제목
- `body`: 앱에 보일 내용
- `deeplink`: 누르면 이동할 앱 주소
- `occurredAt`: 실제 일이 생긴 시간

이 단계가 성공하면 앱의 알림 목록 API에서 볼 수 있는 알림이 생긴다.

## 8. DB 저장이 끝난 뒤 푸시 발송을 시작한다

`CreateNotificationFromOutboxUseCase`는 알림을 저장한 뒤 바로 푸시를 보내지 않는다.

먼저 DB transaction이 잘 끝났는지 기다린다. 그리고 commit이 끝난 뒤 `NotificationPushDispatchService.dispatch()`를 호출한다.

이 순서가 중요하다.

알림이 DB에 저장되지 않았는데 휴대폰 푸시만 먼저 가면, 사용자가 푸시를 눌렀을 때 앱 알림함에는 아무것도 없을 수 있다.

그래서 “DB에 먼저 저장하고, 성공하면 푸시” 순서로 간다.

## 9. 받을 사람의 push token을 찾는다

`NotificationPushDispatchService`는 알림 받을 사람의 active push token을 찾는다.

push token은 앱이 FCM에서 받은 기기 주소 같은 것이다. 사용자가 여러 기기를 쓰면 token도 여러 개일 수 있다.

토큰이 없으면 푸시를 보내지 않는다. 그래도 알림함 row는 이미 저장되어 있으므로 앱에서 알림 목록을 열면 볼 수 있다.

## 10. FCM에 보낼 명령을 만든다

push token이 있으면 `PushNotificationCommand`를 만든다.

여기에는 다음 값이 들어간다.

- FCM token
- title
- body
- deeplink
- notificationId

이 명령은 아직 FCM JSON이 아니다. 우리 서버 내부에서 쓰기 좋은 모양이다.

## 11. FCM HTTP v1 요청으로 바꿔서 보낸다

`FcmHttpPushNotificationSender`가 `PushNotificationCommand`를 FCM HTTP v1 요청으로 바꾼다.

FCM으로 나가는 요청에는 다음 내용이 들어간다.

- endpoint: `/v1/projects/{projectId}/messages:send`
- header: `Authorization: Bearer {accessToken}`
- notification title/body
- data.notificationId
- data.deeplink

이번 구현 보강에서는 이 요청 모양을 테스트로 고정했다.

테스트가 확인하는 것:

- FCM endpoint가 올바른지
- Bearer access token이 들어가는지
- `Content-Type`이 JSON인지
- token, title, deeplink, notificationId가 body에 들어가는지
- 성공 응답의 provider message id를 읽는지

## 12. FCM 응답을 성공, 임시 실패, 잘못된 token으로 나눈다

FCM 응답은 그냥 성공/실패 하나로 보면 부족하다.

`FcmHttpPushNotificationSender`는 응답을 이렇게 나눈다.

- 2xx: 성공
- 400 또는 404 중 token 문제: 잘못된 token
- 401 또는 403: 인증 실패이므로 임시 실패
- 그 외: 임시 실패

이번 구현 보강에서는 이 분류도 테스트로 고정했다.

특히 `UNREGISTERED`, `INVALID_ARGUMENT`, `INVALID_REGISTRATION` 같은 응답은 invalid token으로 본다.

## 13. 발송 결과를 DB에 기록한다

`NotificationPushDispatchService`는 FCM 결과를 `push_delivery_attempts`에 저장한다.

기록하는 이유는 나중에 문제가 생겼을 때 확인하기 위해서다.

예를 들어 “알림함에는 알림이 있는데 휴대폰 푸시는 왜 안 왔지?”를 볼 때 이 테이블을 확인할 수 있다.

## 14. 잘못된 token이면 token을 꺼 둔다

FCM이 invalid token이라고 알려주면 그 token은 더 이상 쓸 수 없는 주소다.

이때 `PushToken.deactivate()`가 호출된다.

그러면 다음 알림부터는 그 token으로 푸시를 보내지 않는다. 계속 실패하는 주소로 반복해서 보내지 않게 막는 것이다.

## 15. SQS 메시지를 처리 완료로 지운다

알림 생성 흐름이 성공하면 `NotificationMessageConsumer`가 SQS 메시지를 acknowledge 한다.

SQS에서는 이것이 메시지 삭제다.

즉, “이 편지는 잘 처리했으니 우체통에서 지워도 된다”는 뜻이다.

주의할 점은 푸시 실패가 항상 SQS 재시도로 이어지지는 않는다는 점이다. 현재 구조는 알림함 저장을 중심으로 보고, 푸시 결과는 `push_delivery_attempts`에 기록한다.

## 16. 앱이 알림 목록 API로 알림을 읽는다

앱은 gateway를 통해 notification 서버 API를 호출한다.

주요 API는 다음과 같다.

- `GET /notification/api/notifications`
- `PATCH /notification/api/notifications/{notificationId}/read`
- `PATCH /notification/api/notifications/read-all`
- `POST /notification/api/push-tokens`
- `DELETE /notification/api/push-tokens`

notification 서버는 `X-User-Id` 헤더로 현재 사용자를 구분한다. 이 헤더는 앱이 직접 믿고 보내는 값이 아니라 gateway가 인증 후 넣어줘야 한다.

## 17. 이번에 코드로 보강한 것

이번 작업에서는 두 부분을 보강했다.

첫째, SQS 연결 자격증명 처리를 notification 서버 설정에 추가했다.

추가된 설정 값:

- `notification.sqs.access-key-id`
- `notification.sqs.secret-access-key`
- `notification.sqs.session-token`

이 값들은 각각 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`에서 온다.

둘째, FCM sender의 실제 HTTP 요청과 응답 분류를 테스트로 보강했다.

추가된 테스트 파일:

- `src/test/kotlin/com/togethertrip/notification/notification/infrastructure/fcm/FcmHttpPushNotificationSenderTest.kt`

테스트가 막아주는 실수:

- SQS access key와 secret key를 넣었는데도 사용하지 않는 실수
- SQS session token을 넣었는데 임시 credential로 만들지 않는 실수
- FCM endpoint를 잘못 부르는 실수
- Authorization header를 빼먹는 실수
- notification title/body/data를 body에 넣지 않는 실수
- 성공 응답의 message id를 못 읽는 실수
- invalid token을 임시 실패로 잘못 분류하는 실수
- FCM 인증 실패를 token 삭제로 잘못 처리하는 실수

## 18. 사람이 직접 해야 하는 일

코드만으로 끝나지 않는다. FCM은 Firebase Console에서 직접 설정해야 한다.

직접 해야 하는 일:

- Firebase project를 만든다.
- FCM service account key를 만든다.
- key 값을 notification 서버 환경변수로 넣는다.
- `FCM_ENABLED=true`로 켠다.
- `FCM_PROJECT_ID`를 넣는다.
- `FCM_CLIENT_ID`를 넣는다.
- `FCM_CLIENT_EMAIL`을 넣는다.
- `FCM_PRIVATE_KEY`를 넣는다.
- `FCM_PRIVATE_KEY_ID`를 넣는다.

중요한 주의점:

- service account JSON 파일은 절대 git에 올리지 않는다.
- private key를 GitHub issue, Slack, 문서에 붙여넣지 않는다.
- private key 줄바꿈은 배포 환경에 따라 `\n` 형태로 넣어야 할 수 있다.

SQS를 실제로 붙여서 테스트하려면 notification 서버 실행 환경에도 AWS 값이 필요하다.

로컬 Docker compose로 띄울 때 직접 확인할 값:

- `NOTIFICATION_QUEUE_URL`
- `AWS_REGION`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- 필요하면 `AWS_SESSION_TOKEN`

여기서 access key와 secret key를 비워 두면 로컬에서는 AWS SDK가 다른 방법으로 자격증명을 찾는다. 로컬 Docker 컨테이너 안에 그런 방법이 없으면 credential 에러가 난다.

## 19. 검증 결과

실행한 명령:

```bash
./gradlew test
```

결과:

- 성공

확인한 것:

- SQS consumer 관련 기존 테스트 통과
- SQS static credential 설정 테스트 통과
- SQS session credential 설정 테스트 통과
- 알림 생성 관련 기존 테스트 통과
- 알림 문구/deeplink 매핑 기존 테스트 통과
- push token 관리 기존 테스트 통과
- push dispatch 기존 테스트 통과
- FCM HTTP sender 신규 테스트 통과

## 20. 아직 실제 기기로 확인해야 하는 것

자동 테스트로는 Firebase 서버와 실제 휴대폰까지 확인하지 못한다.

수동으로 확인해야 하는 것:

- 앱에서 FCM token이 실제로 발급되는지
- 앱이 `POST /notification/api/push-tokens`로 token을 등록하는지
- notification 서버가 실제 Firebase project로 push를 보내는지
- Android 실기기에서 푸시가 오는지
- iOS 실기기에서 푸시가 오는지
- 푸시를 눌렀을 때 앱이 원하는 화면으로 이동하는지

## 21. 최종 정리

이 알림 흐름은 이렇게 움직인다.

`main 이벤트 발생 -> SQS 메시지 생성 -> notification 서버가 SQS 설정과 AWS 자격증명 준비 -> SQS polling -> 받을 사람 찾기 -> 중복 확인 -> title/body/deeplink 생성 -> notifications 저장 -> commit 후 push token 조회 -> FCM HTTP 요청 -> FCM 응답 분류 -> delivery attempt 저장 -> invalid token 비활성화 -> SQS 메시지 삭제 -> 앱 알림함에서 조회`

핵심은 두 가지다.

첫째, 알림함 저장이 중심이다. 푸시가 실패해도 알림 데이터 자체는 남긴다.

둘째, FCM은 외부 서비스라서 설정과 실기기 검증이 꼭 필요하다. 코드는 준비되어 있지만 Firebase Console 설정과 앱 token 등록까지 연결되어야 진짜 푸시가 도착한다.
