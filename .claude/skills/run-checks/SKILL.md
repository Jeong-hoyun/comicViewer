---
name: run-checks
description: ComicViewer 의 ktlint·단위 테스트·(선택)계측 테스트를 실행하고 결과를 요약한다. "테스트 돌려줘", "린트 확인", "검사", "커밋 전 점검" 같은 요청에 사용.
---

# ComicViewer 품질 검사

푸시 전/커밋 전에 도는 게이트와 동일한 검사를 수동으로 돌린다.

## 단계별 명령
1. **ktlint** (스타일):
   ```bash
   ./gradlew ktlintCheck        # 검사만
   ./gradlew ktlintFormat       # 자동 수정
   ```
   - ⚠️ `ktlintFormat ktlintCheck` 를 한 번에 돌리면 포맷이 파일을 고치는 도중 검사가 겹쳐 일시적 FAILED 가 날 수 있다. **따로** 돌릴 것.
2. **단위 테스트** (JVM, 에뮬레이터 불필요):
   ```bash
   ./gradlew testDebugUnitTest
   ```
   결과 수 확인:
   ```bash
   grep -ho 'tests="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml | grep -o '[0-9]*' | paste -sd+ - | bc
   ```
3. **계측 테스트** (에뮬레이터 필요, Room DAO + Compose UI):
   ```bash
   ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
   ```
4. **빌드 확인**: `./gradlew assembleDebug`

## 자주 걸리는 ktlint 규칙 (이 프로젝트에서 실제로 막혔던 것들)
- `function-naming`: 한글 테스트 함수명은 **백틱**으로 감싼다. (예: `` fun `이름 테스트`() ``)
- **계측(androidTest) 테스트 함수명에는 공백 금지** — DEX(minSdk 26)에서 거부됨. 백틱을 쓰되 공백 대신 밑줄. (단위 테스트는 공백 OK)
- `backing-property-naming`: `_foo` 백킹 프로퍼티는 매칭되는 공개 `foo` 가 있을 때만 허용. 아니면 다른 이름으로.
- Compose `@Composable` 함수의 PascalCase 와 일부 Color 상수 PascalCase 는 `.editorconfig` 에서 이미 허용 처리됨.

## 보고
통과/실패 개수와, 실패 시 해당 리포트 경로(`app/build/reports/ktlint/.../*.txt`)의 위반 내용을 요약해 알려준다.
