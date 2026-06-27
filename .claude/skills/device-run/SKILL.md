---
name: device-run
description: ComicViewer 앱을 에뮬레이터 또는 실기기에 빌드·설치·실행하고 화면을 캡처한다. "앱 실행", "기기에 설치", "스크린샷", "빌드해서 띄워줘" 같은 요청에 사용.
---

# ComicViewer 기기 실행 + 스크린샷

## 환경
- adb: `~/Library/Android/sdk/platform-tools/adb`
- 패키지: `com.jhyun.comicviewer`
- 기기 식별자
  - 에뮬레이터: `emulator-5554` (AVD: `Pixel_3a_API_34_extension_level_7_arm64-v8a`)
  - 실기기: `R3CY30GR9DW` (SM-S936N)
- 스크린샷은 항상 **스크래치패드 디렉토리**에 저장하고 Read 로 확인한다.

## 절차
1. 대상 기기 확인 — `adb devices`. 둘 다 붙어 있으면 사용자에게 어느 기기인지 묻거나, 명시 안 되면 에뮬레이터를 기본으로.
2. 에뮬레이터가 꺼져 있으면 부팅:
   ```bash
   ~/Library/Android/sdk/emulator/emulator -avd Pixel_3a_API_34_extension_level_7_arm64-v8a -no-snapshot-save &
   ~/Library/Android/sdk/platform-tools/adb wait-for-device
   # sys.boot_completed == 1 까지 대기
   ```
3. 빌드·설치 (대상 기기 지정):
   ```bash
   ANDROID_SERIAL=<serial> ./gradlew installDebug
   ```
4. 실행:
   ```bash
   adb -s <serial> shell am force-stop com.jhyun.comicviewer
   adb -s <serial> shell monkey -p com.jhyun.comicviewer -c android.intent.category.LAUNCHER 1
   ```
5. 스크린샷 (탭/스와이프 후 2~3초 대기 후):
   ```bash
   adb -s <serial> exec-out screencap -p > <scratchpad>/shot.png
   ```
   그리고 Read 로 이미지를 확인한다.

## UI 자동화 팁
- 화면 좌표는 보통 1080x2220. 스크린샷 표시 좌표에 약 1.11을 곱해 실제 좌표로.
- 탭: `adb -s <serial> shell input tap <x> <y>` / 스와이프: `... input swipe x1 y1 x2 y2 <ms>` / 뒤로: `... input keyevent 4`
- SAF 폴더 선택기는 시스템 UI다. "USE THIS FOLDER" 후 권한 다이얼로그의 ALLOW 를 눌러야 한다.

## 주의
- DB 스키마 버전이 오르면(파괴적 마이그레이션) **추가했던 라이브러리 폴더가 초기화**된다 → SAF 로 다시 추가해야 함.
- 디버그 빌드는 디버그 키로 서명된다(출시용 아님).
