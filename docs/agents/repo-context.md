# Repo Context

## 리포 역할

`notification`은 TogetherTrip의 알림/푸시 서버다.

## 책임 범위

- 앱 알림함 생성, 조회, 읽음 처리
- 푸시 알림 발송
- 알림 대상 검증과 자기 자신 알림 제외
- 딥링크 대상 관리
- FCM/APNs 등 외부 push provider 연동

## 아키텍처 원칙

- notification bounded context를 독립적으로 유지한다.
- feature-based MVC 패턴을 기본으로 하고, 알림 Controller/API, Service, Repository, domain 책임을 분리한다.
- 외부 provider 토큰과 응답은 민감정보로 취급한다.
- 알림 payload에는 필요한 최소 정보만 포함한다.

## 통신 규칙

- `app -> gateway -> notification` 알림함 조회/읽음 처리를 기본으로 한다.
- `main -> notification`, `chat -> notification` 알림 요청을 허용한다.
- `notification -> FCM/APNs` provider 호출은 Service 내부의 명시적인 클라이언트/설정으로 분리한다.
- 다른 서비스의 DB에 직접 접근하지 않는다.

## 핵심 도메인 주의점

- 모든 알림에서 자기 자신은 알림 대상에서 제외한다.
- 알림은 최신순이며 API 조회는 최근 100개로 제한한다.
- 개별 알림 삭제는 `deleted_at`을 기록하는 soft delete로 처리한다.
- 딥링크 대상은 app/main 권한 정책과 충돌하지 않아야 한다.
