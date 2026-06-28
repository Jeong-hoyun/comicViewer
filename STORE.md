# Paneo — Play Store 등록 자료

앱 이름: **Paneo** (panel = 만화 칸)
패키지: `com.jhyun.comicviewer` *(출시 전 본인 고유값 권장, 예: `com.<도메인>.paneo`)*
카테고리: **만화 (Comics)**

---

## 한국어

### 짧은 설명 (≤ 80자)
> 광고 없는 로컬 만화 뷰어. 폴더·ZIP·CBZ·CBR·PDF를 내 폰에서 바로 읽기.

### 자세한 설명
내 기기에 있는 만화를 **광고 없이, 깔끔하게** 보는 로컬 만화 뷰어 Paneo.

**지원 형식**
- 이미지 폴더 (JPG/PNG/WebP/GIF…)
- 압축본 **ZIP / CBZ / CBR**
- **PDF**

**리더**
- 읽기 방향: 좌→우 · 우→좌 · 세로(웹툰) · 부드러운 스크롤
- 페이지 레이아웃: 1페이지 / 2페이지(양면) / 자동
- 핀치 줌, 화면 탭으로 도구 표시/숨김
- 밝기 · 배경색 · 화면 회전 잠금 · 화면 켜둠 유지

**라이브러리**
- 표지 그리드 / 리스트 보기, 이름·날짜 정렬
- **이어보기**(마지막 페이지 기억) · 읽기 히스토리
- **책갈피**로 원하는 페이지 저장·이동

**프라이버시 우선**
- 저장소 접근 프레임워크(SAF)만 사용 — **저장소 권한 불필요**
- **데이터 수집·전송 없음**, 인터넷 권한 없음, 완전 오프라인

> Paneo 는 뷰어입니다. 만화 콘텐츠를 제공하지 않습니다. 본인이 소유·보유한 파일만 여세요.

### ASO 키워드
만화 뷰어, 만화 리더, 웹툰 뷰어, cbz, cbr, 만화 zip, pdf 만화, 로컬 만화, 광고 없는 만화, comic reader

---

## English

### Short description (≤ 80 chars)
> Ad-free local comic reader. Open folders, ZIP, CBZ, CBR & PDF on your phone.

### Full description
Paneo is an **ad-free, offline** comic reader for files already on your device.

**Formats:** image folders, ZIP / CBZ / CBR, PDF
**Reader:** LTR / RTL / vertical (webtoon) / smooth scroll · single / dual / auto page · pinch-zoom · tap to toggle controls · brightness, background, rotation lock, keep-screen-on
**Library:** cover grid & list, sort by name/date, **continue reading**, history, **bookmarks**
**Privacy-first:** Storage Access Framework only (no storage permission), **no data collection**, no internet permission.

> Paneo is a viewer and does not provide any comic content. Open only files you own.

---

## Play Console 체크리스트

### 그래픽 자산 (`store/` 에 준비됨)
- [x] 앱 아이콘 512×512 — `store/icon_512.png`
- [x] 피처 그래픽 1024×500 — `store/feature_graphic.png`
- [x] 폰 스크린샷 4장 (1080×2160) — `store/screenshot_01_library.png` 외
  - 01 라이브러리 그리드 · 02 리더(칸) · 03 리더(표지) · 04 미리보기
- [ ] (선택) 7인치·10인치 태블릿 스크린샷
- ⚠️ 스크린샷의 만화는 **자체 제작 샘플(저작권 무관)**. 실제 출시 시 더 다양한 컷 권장.

### 설정 항목
- [ ] 카테고리: 만화(Comics)
- [ ] 광고 포함: **아니오**

### Data safety (데이터 보안) 답변 가이드
앱에 **INTERNET 권한이 없고** 수집/전송이 없으므로(병합 매니페스트 확인됨):
- 데이터 수집: **아니오 (No data collected)**
- 데이터 공유: **아니오 (No data shared)**
- 보안 관행: 전송 데이터 없음 → 암호화 항목 해당 없음
- 데이터 삭제 요청: 모든 데이터가 기기 내부에만 있고 앱 삭제 시 제거됨

### 콘텐츠 등급 설문 가이드
앱은 **자체 콘텐츠가 없는 로컬 파일 뷰어**입니다. 설문은 "앱이 제공하는 콘텐츠" 기준:
- 폭력/성적/욕설/약물/도박 콘텐츠 제공: **모두 아니오**
- 사용자 생성/공유 콘텐츠 기능: 없음(로컬 파일만, 업로드·공유 기능 없음)
- 예상 등급: 전체이용가 수준
- ⚠️ 마케팅·스크린샷·설명에 성인/불법 콘텐츠를 노출하지 말 것(중립적 뷰어로 포지셔닝)

### 개인정보처리방침
- 내용: `PRIVACY.md` (수집 없음 — 한/영)
- **공개 URL 필요.** 호스팅 옵션:
  - 공개 GitHub Gist: `gh gist create PRIVACY.md --public`
  - GitHub Pages(공개 레포) / Google Sites / Notion 공개 페이지 등
- 방침 내 **연락 이메일**과 최종 수정일을 본인 값으로 채울 것

### 출시 트랙
내부 테스트 → 비공개 테스트 → 프로덕션
