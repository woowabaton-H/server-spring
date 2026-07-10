# 1. User 도메인

작업 순서: 1번째 (최초 작업)
의존성: 없음
범위: 인증/게스트 세션 없음. 서버는 항상 고정된 데모 사용자 1명을 대상으로 동작한다고 가정하고 구현한다. 사용자 row 자체는 별도 SQL로 준비하므로, 여기서는 테이블 구조와 조회/수정 API만 다룬다.

## 1.1 데이터 구조

### users

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| id | uuid pk | 사용자 ID |
| name | varchar(40) | 표시 이름 |
| avatar_text | varchar(4) | 아바타 문자 |
| timezone | varchar(64) | 기본 `Asia/Seoul` |
| created_at | timestamptz | 생성 시각 |
| updated_at | timestamptz | 수정 시각 |

인증/권한 관련 컬럼(role, device_id_hash 등)은 이번 범위에서 제외한다.

## 1.2 API 명세

Base URL: `/api/v1`

### GET /me

내 정보 조회.

Response `200`:

```json
{
  "data": {
    "id": "usr_01",
    "name": "보송",
    "avatarText": "보",
    "timezone": "Asia/Seoul",
    "createdAt": "2026-07-10T09:00:00+09:00"
  }
}
```

### PATCH /me

내 정보 수정.

Request:

```json
{
  "name": "보송",
  "avatarText": "보"
}
```

Response `200`:

```json
{
  "data": {
    "id": "usr_01",
    "name": "보송",
    "avatarText": "보",
    "timezone": "Asia/Seoul"
  }
}
```

검증:

- `name`: 1~40자
- `avatarText`: 1~4자

## 1.3 완료 기준

1. `GET /me`가 고정 데모 사용자의 정보를 반환한다.
2. `PATCH /me`로 이름/아바타 문자를 수정할 수 있다.
3. 인증 미들웨어, 토큰 검증, 게스트 세션 생성 로직은 만들지 않는다.
