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
[
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
]
```

## 알림 읽음 처리

```http
PATCH /notification/api/notifications/{notificationId}/read
X-User-Id: 1
```

다른 사용자의 알림 ID로 요청하면 `404`를 반환한다.

## 전체 읽음 처리

```http
PATCH /notification/api/notifications/read-all
X-User-Id: 1
```

응답 예시:

```json
{
  "updatedCount": 3
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

## FCM Token 비활성화

```http
DELETE /notification/api/push-tokens
X-User-Id: 1
Content-Type: application/json

{
  "token": "fcm-registration-token"
}
```

## 운영 환경변수

기존 notification 실행 값에 더해 FCM 발송을 켤 때 다음 값을 추가해야 한다.

```properties
FCM_ENABLED=true
FCM_PROJECT_ID=<firebase-project-id>
FCM_ACCESS_TOKEN=<oauth2-access-token>
FCM_ENDPOINT=https://fcm.googleapis.com
FCM_TIMEOUT=PT3S
```

`FCM_ACCESS_TOKEN`은 단기 토큰이므로 운영에서는 service account 기반 발급/회전 자동화가 필요하다. 값이 없거나 `FCM_ENABLED=false`이면 no-op sender로 동작한다.
