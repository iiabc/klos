package com.hiusers.klos

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class KlosPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 1. 确保 Shadow 插件已被应用
        if (!project.plugins.hasPlugin("com.github.johnrengelman.shadow")) {
            project.plugins.apply("com.github.johnrengelman.shadow")
        }

        // 2. 配置 ShadowJar 任务
        project.tasks.withType<ShadowJar> {
            // 排除 Exposed 和 Kotlin (防止打入 Jar 包)
            dependencies {
                exclude { it.moduleGroup == "org.jetbrains.exposed" }
                exclude { it.moduleGroup == "org.jetbrains.kotlin" }
            }

            // 3. 构建结束时注入 plugin.yml
            doLast {
                val finalJarFile = archiveFile.get().asFile
                if (!finalJarFile.exists()) return@doLast
                
                // 动态获取当前项目中使用的 Exposed 依赖
                val exposedDependencies = project.configurations.getByName("compileClasspath").resolvedConfiguration.firstLevelModuleDependencies
                    .filter { it.moduleGroup == "org.jetbrains.exposed" }
                    .map { "${it.moduleGroup}:${it.moduleName}:${it.moduleVersion}" }

                if (exposedDependencies.isEmpty()) {
                    println("ℹ️ [Klos] No Exposed dependencies found, skipping library injection.")
                    return@doLast
                }

                val jarUri = URI.create("jar:${finalJarFile.toURI()}")

                // 写入 libraries 配置
                try {
                    FileSystems.newFileSystem(jarUri, emptyMap<String, Any>()).use { fs ->
                        val pluginYmlPath = fs.getPath("plugin.yml")
                        if (Files.exists(pluginYmlPath)) {
                            val libConfigBuilder = StringBuilder()
                            libConfigBuilder.append("\n\n# Auto-injected by Klos\nlibraries:\n")
                            exposedDependencies.forEach { dep ->
                                libConfigBuilder.append("  - $dep\n")
                            }

                            Files.write(
                                pluginYmlPath,
                                libConfigBuilder.toString().toByteArray(Charsets.UTF_8),
                                StandardOpenOption.APPEND
                            )
                            println("✅ [Klos] Injected ${exposedDependencies.size} Exposed libraries into plugin.yml")
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ [Klos] Warning: Failed to inject plugin.yml: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}
