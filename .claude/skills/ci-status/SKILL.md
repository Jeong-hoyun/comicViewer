---
name: ci-status
description: GitHub Actions CI 실행 상태를 gh CLI 로 확인/추적한다. "CI 확인", "CI 통과했어?", "액션 상태", push 후 결과 확인에 사용.
---

# CI 상태 확인 (gh)

저장소 `Jeong-hoyun/comicViewer` 는 **비공개**라 웹 API 무인증 조회는 안 된다. `gh` CLI(로그인됨)로 확인한다.

## 명령
- 최근 실행 목록:
  ```bash
  gh run list --limit 5
  ```
- 최신 실행 상태(브랜치 main):
  ```bash
  gh run list --branch main --limit 1
  ```
- 진행 중인 실행 실시간 추적:
  ```bash
  gh run watch
  ```
- 특정 실행 상세/실패 로그:
  ```bash
  gh run view <run-id>
  gh run view <run-id> --log-failed
  ```

## CI 구성 (.github/workflows/ci.yml)
- `build` 잡: ktlintCheck → testDebugUnitTest → assembleDebug → APK 아티팩트
- `instrumented` 잡: `needs: build`, 에뮬레이터(API 34) 에서 connectedDebugAndroidTest
- 에뮬레이터 잡은 부팅 포함 5~10분. status check 이름: `Lint · Test · Build`, `계측 테스트 (에뮬레이터)`

## 실패 시
`gh run view <id> --log-failed` 로 원인 확인. 흔한 원인:
- ktlint 위반 → `run-checks` 스킬 참고
- 계측 테스트 함수명 공백(DEX) / 에뮬레이터 환경 이슈
- 로컬 pre-push 훅이 ktlint+단위는 막아주므로 보통 build 잡은 통과. 계측 잡이 새 변수.
