# 코믹뷰어 (ComicViewer)

광고 없는 **로컬 만화 뷰어** (Android). 내가 가진 만화 파일(이미지 폴더, 추후 CBZ/CBR/PDF)을
Storage Access Framework 로 읽어 쾌적하게 보는 것이 목표.

## 현재 상태: 스캐폴딩 + SAF PoC
- Compose + Hilt + Room 골격
- 폴더 추가(SAF, 영구 권한) → 폴더 스캔 → 이미지 파일 목록 표시
- 추가한 폴더는 Room 에 저장되어 앱 재실행 후에도 유지

## 기술 스택
- Kotlin / Jetpack Compose / Material3
- Hilt (DI), Room (DB), Coil (이미지), Telephoto (줌)
- Storage Access Framework — 저장소 권한 불필요(`MANAGE_EXTERNAL_STORAGE` 미사용)

## 빌드 / 실행
```bash
./gradlew assembleDebug          # 디버그 APK 빌드
./gradlew installDebug           # 연결된 기기에 설치
./gradlew bundleRelease          # 서명된 릴리스 AAB (Play 업로드용)
```
Android Studio 에서 이 폴더를 열어도 됩니다.

## 릴리스 서명
릴리스 빌드는 `keystore.properties`(git 제외)의 정보로 서명합니다. **클론 후 본인 키스토어로 한 번 설정**하세요:
```bash
# 1) 업로드 키스토어 생성 (한 번만)
keytool -genkeypair -v -keystore app/upload-keystore.jks \
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
# 2) 프로젝트 루트에 keystore.properties 작성 (git 제외됨)
cat > keystore.properties <<'PROP'
storeFile=upload-keystore.jks
storePassword=<스토어 비밀번호>
keyAlias=upload
keyPassword=<키 비밀번호>
PROP
```
- `keystore.properties` / `*.jks` 는 **절대 커밋 금지**(`.gitignore` 처리됨).
- 파일이 없으면 릴리스 빌드는 **디버그 서명으로 폴백**(설치 테스트용, Play 업로드 불가).
- Play **App Signing** 사용 권장: 이 키는 "업로드 키"로만 쓰고 앱 서명 키는 Google 이 관리.

## Git 훅 (ktlint)
코드 스타일을 자동 강제하기 위해 git 훅을 사용합니다. **클론 후 한 번만** 설치하세요:
```bash
./gradlew installGitHooks
```
- **pre-commit**: 스테이징된 Kotlin 파일에 `ktlintFormat` 자동 적용 후 재-stage
- **pre-push**: `ktlintCheck` + 단위 테스트. 실패 시 푸시 차단
- 수동: `./gradlew ktlintFormat` (수정) / `./gradlew ktlintCheck` (검사)
- 긴급 우회: `git commit/push --no-verify`
- 규칙은 `.editorconfig` 에서 조정 (Compose 친화 설정 포함).

## 환경 메모
- compileSdk / targetSdk = 35 (Play 신규 앱/업데이트 요건)
- minSdk = 26
- applicationId = `com.jhyun.comicviewer` (출시 전 본인 고유값으로 변경)
- 릴리스: R8 minify + 리소스 축소 활성화 (proguard-rules.pro)

## 다음 단계 (로드맵)
1. **리더 화면**: HorizontalPager 페이지 모드(LTR/RTL) + 세로 연속(웹툰) 모드 + 줌
2. **읽기 진행도**: 마지막 페이지 저장 / 이어보기 (Room)
3. **표지 그리드**: 폴더/책 단위 라이브러리 + 표지 썸네일 캐시
4. **아카이브 파서**: CBZ/ZIP(zip4j) → CBR(junrar) → PDF(PdfRenderer/Pdfium)
5. **설정**: 다크모드, 탭존, 볼륨키 넘김, 배경색
6. **출시 준비**: AAB, 개인정보처리방침, Data safety, 내부→비공개→프로덕션 트랙

## 코드 구조
```
app/src/main/java/com/jhyun/comicviewer/
 ├─ core/        # 공통 유틸 (자연정렬 등)
 ├─ data/        # SafScanner, LibraryRepository, local/ (Room)
 ├─ di/          # Hilt 모듈
 └─ ui/
     ├─ library/ # LibraryScreen + ViewModel
     └─ theme/
```
