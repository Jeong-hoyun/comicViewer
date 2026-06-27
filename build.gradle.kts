// Top-level build file. Plugin versions are declared in gradle/libs.versions.toml
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint)
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        android.set(true)
        // 위반이 있으면 빌드(및 커밋/푸시)를 실패시킵니다.
        ignoreFailures.set(false)
    }
}

/**
 * git hook 설치 태스크.
 * - pre-commit: ktlint 자동 포맷 후 재-stage (ktlint-gradle 제공)
 * - pre-push:   ktlintCheck + 단위 테스트 (아래 스크립트 복사)
 *
 * 클론 후 한 번만 실행하면 됩니다:  ./gradlew installGitHooks
 */
tasks.register("installPrePushHook") {
    description = "scripts/git-hooks/pre-push 를 .git/hooks 로 복사합니다."
    group = "git hooks"
    // ktlint 의 pre-commit 설치 태스크와 같은 .git/hooks 를 만지므로 순서를 고정.
    mustRunAfter("addKtlintFormatGitPreCommitHook")
    val source = layout.projectDirectory.file("scripts/git-hooks/pre-push").asFile
    val target =
        layout.projectDirectory
            .dir(".git/hooks")
            .asFile
            .resolve("pre-push")
    doLast {
        if (!source.exists()) error("훅 스크립트가 없습니다: $source")
        source.copyTo(target, overwrite = true)
        target.setExecutable(true)
        println("설치됨: $target")
    }
}

tasks.register("installGitHooks") {
    description = "pre-commit(ktlint) + pre-push 훅을 모두 설치합니다."
    group = "git hooks"
    dependsOn("addKtlintFormatGitPreCommitHook", "installPrePushHook")
}
