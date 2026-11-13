// 顶层构建脚本：只声明插件版本，具体配置在 app/build.gradle.kts
// 说明：AGP 9 起内置 Kotlin 支持，不再应用 org.jetbrains.kotlin.android；
// Compose 编译仍需单独应用 kotlin compose 插件。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
