---
name: seed-test-comics
description: 테스트용 만화 라이브러리(폴더/하위폴더/zip)를 기기의 Download/MangaLib 에 만든다. DB 초기화 후 다시 데이터가 필요하거나 "테스트 데이터 넣어줘", "샘플 만화 만들어줘" 할 때 사용.
---

# 테스트 만화 데이터 시드

자연정렬·표지·zip·이어보기·혼합폴더를 검증할 수 있는 고정 구조를 기기에 만든다.

## 만들 구조 (`/sdcard/Download/MangaLib/`)
- `SeriesA/` — 이미지 4장 (`p_1, p_2, p_3, p_10`) : 자연정렬 확인
- `SeriesB/` — 이미지 2장
- `Mixed/` — 이미지 1장 + `SubChapter/`(이미지 1장) : 이미지+하위폴더 혼합
- `SeriesZip.zip` — 이미지 4장 압축 (zip/cbz 읽기 확인)

## 절차
1. 작은 PNG 1장을 만든다(스크래치패드의 기존 png 가 있으면 재사용, 없으면 base64 로 생성):
   ```bash
   # 최소 유효 PNG 생성 (한 색 픽셀)
   printf '\x89PNG\r\n\x1a\n...' > /tmp/seed.png   # 또는 sips/magick 로 생성
   ```
   실무에선 세션 스크래치패드에 이미 만들어 둔 `page_N.png` 를 복사해 쓰면 된다.
2. 기기에 폴더 구조 생성 + 푸시 (대상 기기 serial 지정):
   ```bash
   ADB="~/Library/Android/sdk/platform-tools/adb -s <serial>"
   $ADB shell mkdir -p /sdcard/Download/MangaLib/SeriesA /sdcard/Download/MangaLib/SeriesB /sdcard/Download/MangaLib/Mixed/SubChapter
   # 이미지 푸시 (page_*.png 를 각 폴더에 알맞은 이름으로)
   # SeriesZip.zip 은 로컬에서 zip 으로 묶어 push:  (cd src && zip -X ../SeriesZip.zip p_1.png p_2.png p_3.png p_10.png)
   ```
3. 앱에서 SAF 로 `Download/MangaLib` 를 추가한다.
   - **중요**: Download "루트"는 OS 정책상 선택 불가 → 반드시 그 하위인 `MangaLib` 를 선택.
   - 폴더 추가 FAB → (피커가 MangaLib 기억) → USE THIS FOLDER → 권한 ALLOW.

## 참고
- DB 버전이 오르면 라이브러리 폴더가 초기화되므로, 이 시드 후 앱에서 SAF 재추가가 필요할 수 있다.
- 이미지 내용은 더미라도 무방(기능 검증용). 표지는 각 폴더/zip 의 첫 이미지.
