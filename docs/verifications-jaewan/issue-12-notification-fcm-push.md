# Verification Report

## 검증 대상

Issue #12 `feat: 알림함 API와 FCM push 연동 구현`

## 실행한 명령

```bash
./gradlew test
./gradlew clean build
```

## 결과

- `./gradlew test`: 성공
- `./gradlew clean build`: 성공
- 알림함 조회가 본인 알림만 반환하는지 확인했다.
- 다른 사용자의 알림 읽음 처리가 실패하는지 확인했다.
- FCM token 중복 등록 시 한 행만 유지되는지 확인했다.
- token 비활성화가 본인 token에만 적용되는지 확인했다.
- 주요 outbox event payload가 title/body/deeplink로 매핑되는지 확인했다.
- invalid token 결과가 token 비활성화와 delivery attempt 기록으로 이어지는지 확인했다.

## 실패 또는 미검증 항목

- 실제 Firebase 서버로 push를 보내는 수동 검증은 수행하지 않았다.
- 운영용 `FCM_ACCESS_TOKEN` 자동 발급/회전은 이번 구현 범위에서 제외했다.

## 다음 조치

- PR 본문에 gateway 헤더 주입, FCM 환경변수, access token 발급/회전 작업을 명시한다.
