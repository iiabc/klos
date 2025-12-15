# Klos Gradle Plugin

A Gradle plugin designed to simplify the integration of [JetBrains Exposed](https://github.com/JetBrains/Exposed) framework into TabooLib/Spigot projects.

它主要解决了以下痛点：
1. **自动配置依赖加载**：无需手动在 `plugin.yml` 中编写繁琐的 `libraries` 配置。
2. **自动构建优化**：自动在 `ShadowJar` 任务中排除 Exposed 和 Kotlin 依赖，防止包体积膨胀。
3. **版本自由**：插件不强制绑定 Exposed 版本，完全尊重项目自身的依赖配置。

## Features

- ✅ **零配置**：应用插件即可生效，无需额外 DSL 配置。
- ✅ **智能扫描**：自动检测项目引用的 Exposed 模块（core, dao, jdbc 等）及其版本。
- ✅ **自动注入**：构建时自动将检测到的依赖写入生成的 Jar 包内的 `plugin.yml`。
- ✅ **自动排除**：自动配置 `shadowJar` exclude 规则，避免将库代码打包进插件 Jar。

## Publishing

### 1. Publish to Local Maven (For Testing)

If you want to test the plugin locally or use it without a remote repository:

```bash
./gradlew publishToMavenLocal
```

### 2. Publish to Remote Maven

Configure your repository credentials in `~/.gradle/gradle.properties` or project properties:

```properties
mavenUsername=your_username
mavenPassword=your_password
```

Then run:

```bash
./gradlew publish
```

To configure the remote repository URL, edit `publishing.repositories.maven.url` in `build.gradle.kts`.

## Installation & Usage

### 1. Configure Plugin Repository

```kotlin
repositories {
        mavenLocal() // If you published locally
        maven("https://your-maven-repo.com/releases") // If you published remotely
    }
```

### 2. Apply the Plugin

In your `build.gradle.kts`:

```kotlin
plugins {
    id("io.izzel.taboolib") version "..."
    id("com.hiusers.klos") version "1.0.0"
}
```

### 3. Add Dependencies

Add Exposed dependencies as you normally would (using `compileOnly` is recommended since they will be loaded by Spigot libraries at runtime):

```kotlin
dependencies {
    val exposedVersion = "0.41.1" // Use any version you like
    compileOnly("org.jetbrains.exposed:exposed-core:$exposedVersion")
    compileOnly("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    // ... other modules
}
```

### 4. Build

Run the build task:

```bash
./gradlew build
```

The plugin will:
1. Detect `exposed-core`, `exposed-dao`, etc. from your classpath.
2. Exclude them from the final jar.
3. Update `plugin.yml` inside the jar to include them in the `libraries` section.

## Requirements

- Gradle 7.0+
- TabooLib 6.2.3+ (Recommended)
- Spigot/Paper 1.16.5+ (For `libraries` support)
