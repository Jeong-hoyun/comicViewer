---
name: release-checklist
description: ComicViewer 를 Google Play 에 출시하기 위한 단계별 체크리스트(버전/서명/AAB/Play Console/CD). "출시 준비", "릴리스 빌드", "Play 스토어 올리기", "AAB 만들기", "서명" 같은 요청에 사용.
---

# ComicViewer 출시 체크리스트

현재 상태(확인 후 갱신): `applicationId=com.jhyun.comicviewer`, `versionCode=1`, `versionName=0.1.0`,
`compileSdk/targetSdk=34`, `minSdk=26`, `isMinifyEnabled=false`. (app/build.gradle.kts)

진행 시 **하나씩 사용자에게 확인**받으며 간다. 서명키·계정 생성 등 되돌리기 어려운 단계는 반드시 사전 동의.

## 1) 코드/빌드 설정
- [ ] **applicationId 확정** — 출시 후 변경 불가. 본인 고유 도메인 역순 권장(예: `com.<본인>.comicviewer`).
- [ ] **targetSdk 35** 로 상향 (Play 신규앱 요건). `android-35` SDK 설치 필요. compileSdk 도 35.
- [ ] **versionCode / versionName** 정리 (출시마다 versionCode 증가).
- [ ] **R8/난독화 켜기**: release `isMinifyEnabled = true`, `isShrinkResources = true`.
  - Coil/Telephoto/Hilt/Room/Commons-Compress 관련 keep 규칙을 `proguard-rules.pro` 에 확인·추가.
  - **반드시 release 빌드를 실기기에서 스모크 테스트** (난독화로 깨지는지).
- [ ] 디버그 흔적 제거: 로그, 테스트용 시드 데이터 의존 없음 확인.
- [ ] 권한 재확인: 저장소 권한 미선언(=SAF only) 유지 → 심사 빠름, 권한 특별승인 불필요.

## 2) 앱 아이콘 / 이름 / 브랜딩
- [ ] 런처 아이콘(적응형) 다듬기, `app_name` 확정.
- [ ] 스플래시(선택), 스토어용 스크린샷/피처 그래픽 준비.

## 3) 서명 (업로드 키)
- [ ] 업로드 키스토어 생성:
  ```bash
  keytool -genkey -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 9125 -alias upload
  ```
- [ ] **키스토어와 비밀번호는 절대 커밋 금지.** `keystore.properties`(gitignore) 에 두고 build.gradle 에서 읽기:
  ```
  # keystore.properties  (← .gitignore 에 추가)
  storeFile=../upload-keystore.jks
  storePassword=...
  keyAlias=upload
  keyPassword=...
  ```
- [ ] `signingConfigs { release { ... } }` 구성, release buildType 에 연결.
- [ ] **Play App Signing** 사용 권장(구글이 앱 서명키 관리, 업로드키만 본인 보관).

## 4) AAB 빌드 (Play 는 APK 가 아닌 AAB)
```bash
./gradlew bundleRelease
# 산출물: app/build/outputs/bundle/release/app-release.aab
```
- [ ] 로컬 검증: `bundletool` 로 기기 설치 테스트(선택).

## 5) Play Console 최초 등록 (수동 — API로 앱 생성 불가)
- [ ] 개발자 계정($25 1회), 앱 생성.
- [ ] 스토어 등록정보(설명/스크린샷/아이콘/카테고리).
- [ ] **콘텐츠 등급** 설문 — 만화 뷰어이고 사용자 로컬 파일을 여는 앱임을 정확히 기재. 성인물 열람 가능성 있으면 등급 정직하게.
- [ ] **데이터 보안(Data safety)**: 이 앱은 데이터 수집/전송 없음(로컬 전용) → 그대로 신고.
- [ ] **개인정보처리방침 URL** 필요(수집 없어도 정책 페이지 요구될 수 있음).
- [ ] **내부 테스트 트랙**에 첫 AAB 업로드 → 동작 확인.

## 6) 출시 트랙
내부 테스트 → 비공개(Closed) → 프로덕션 순으로 승급.

## 7) CD 자동화 (최초 수동 출시 "이후"에만 가능)
- [ ] Play Console 서비스 계정 + JSON 키 발급, GitHub Secret 으로 등록.
- [ ] 업로드 키스토어/비번도 Secret 으로.
- [ ] `.github/workflows/ci.yml` 에 deploy 잡 추가(`needs: build`):
  - `r0adkll/upload-google-play@v1` 또는 Gradle Play Publisher(Triple-T) 사용.
  - 처음엔 `track: internal` 로.
- [ ] Branch protection 못 쓰는 무료 비공개 환경이면, 태그 push(`v*`) 트리거로 배포하도록 구성 가능.

## 참고
- 비공개 GitHub 무료 플랜은 브랜치 보호/Ruleset 불가 → 출시 게이트는 pre-push 훅 + CI 로.
- 자세한 진행은 단계별로 사용자와 확인하며, 키/계정 관련은 사용자가 직접 수행(인증·결제 필요).
