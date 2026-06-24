# Notification API

## 공통 헤더

notification API는 현재 사용자를 다음 헤더로 식별한다.

```http
X-User-Id: 1
```

운영에서는 gateway 또는 인증 계층이 인증된 사용자 ID를 이 헤더로 주입해야 한다.

## 알림 목록 조회

```http
GET /notification/api/notifications?limit=100
X-User-Id: 1
```

응답 예시:

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "sourceEventId": 201,
      "eventType": "POST_CREATED",
      "aggregateType": "POST",
      "aggregateId": 20,
      "title": "제주 여행",
      "body": "지우님이 첫 일정을 작성했습니다.",
      "deeplink": "togethertrip://trips/10/posts/20",
      "occurredAt": "2026-06-24T00:00:00Z",
      "readAt": null,
      "createdAt": "2026-06-24T00:00:01Z"
    }
  ],
  "message": null
}
```

## 알림 읽음 처리

```http
PATCH /notification/api/notifications/{notificationId}/read
X-User-Id: 1
```

다른 사용자의 알림 ID로 요청하면 `404`를 반환한다.

응답 예시:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "sourceEventId": 201,
    "eventType": "POST_CREATED",
    "aggregateType": "POST",
    "aggregateId": 20,
    "title": "제주 여행",
    "body": "지우님이 첫 일정을 작성했습니다.",
    "deeplink": "togethertrip://trips/10/posts/20",
    "occurredAt": "2026-06-24T00:00:00Z",
    "readAt": "2026-06-24T00:10:00Z",
    "createdAt": "2026-06-24T00:00:01Z"
  },
  "message": null
}
```

## 전체 읽음 처리

```http
PATCH /notification/api/notifications/read-all
X-User-Id: 1
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "updatedCount": 3
  },
  "message": null
}
```

## FCM Token 등록/갱신

```http
POST /notification/api/push-tokens
X-User-Id: 1
Content-Type: application/json

{
  "token": "fcm-registration-token",
  "platform": "ANDROID",
  "deviceId": "device-1"
}
```

응답에는 token 원문을 포함하지 않는다.

응답 예시:

```json
{
  "success": true,
  "data": {
    "id": 1,
    "platform": "ANDROID",
    "deviceId": "device-1",
    "active": true,
    "lastRegisteredAt": "2026-06-24T00:00:00Z"
  },
  "message": null
}
```

## FCM Token 비활성화

```http
DELETE /notification/api/push-tokens
X-User-Id: 1
Content-Type: application/json

{
  "token": "fcm-registration-token"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

## 에러 응답

main 서버와 같은 공통 에러 응답 형식을 사용한다.

```json
{
  "success": false,
  "code": "NOTIFICATION_NOT_FOUND",
  "message": "알림을 찾을 수 없습니다."
}
```

## 운영 환경변수

기존 notification 실행 값에 더해 FCM 발송을 켤 때 다음 값을 추가해야 한다.

```properties
FCM_ENABLED=true
FCM_PROJECT_ID=<firebase-project-id>
FCM_CLIENT_ID=<service-account-client-id>
FCM_CLIENT_EMAIL=<service-account-client-email>
FCM_PRIVATE_KEY_ID=<service-account-private-key-id>
FCM_PRIVATE_KEY=<service-account-private-key-with-escaped-newlines>
FCM_TOKEN_URI=https://oauth2.googleapis.com/token
FCM_ENDPOINT=https://fcm.googleapis.com
FCM_TIMEOUT=PT3S
```

`FCM_PRIVATE_KEY`는 service account JSON의 `private_key` 값을 사용하되, 환경변수에 넣을 때 줄바꿈을 `\n`으로 이스케이프한다.
서버는 이 값을 원래 줄바꿈으로 복원한 뒤 `https://www.googleapis.com/auth/firebase.messaging` scope로 OAuth2 access token을 자동 발급/갱신한다.

초기 수동 검증이 필요하면 `FCM_ACCESS_TOKEN=<oauth2-access-token>`을 fallback으로 사용할 수 있지만, 운영에서는 service account 값 기반 자동 발급을 사용한다.
값이 없거나 `FCM_ENABLED=false`이면 no-op sender로 동작한다.
