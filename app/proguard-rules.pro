# 기본 ProGuard/R8 규칙.
# Hilt/Room/Coil/Compose 는 자체 consumer rules 를 포함하므로 보통 추가 설정 불필요.

# --- 압축 만화 라이브러리 ---
# junrar (cbr/rar)
-keep class com.github.junrar.** { *; }
-dontwarn com.github.junrar.**

# commons-compress (zip/cbz) — 선택적 코덱(xz/zstd/brotli) 미포함 경고 무시
-keep class org.apache.commons.compress.archivers.zip.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.**
-dontwarn com.github.luben.**
-dontwarn org.brotli.**

# junrar/commons-compress 가 참조할 수 있는 로깅 경고 무시
-dontwarn org.slf4j.**
