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
                
                val compileClasspath = project.configurations.getByName("compileClasspath").resolvedConfiguration
                val allDependencies = compileClasspath.firstLevelModuleDependencies
                
                // 动态获取当前项目中使用的 Exposed 依赖
                val exposedDependencies = allDependencies
                    .filter { it.moduleGroup == "org.jetbrains.exposed" }
                    .map { "${it.moduleGroup}:${it.moduleName}:${it.moduleVersion}" }

                // 动态获取 Kotlin 版本（优先从已解析的依赖中获取，其次从 gradle.properties）
                val kotlinStdlib = allDependencies
                    .firstOrNull { it.moduleGroup == "org.jetbrains.kotlin" && it.moduleName == "kotlin-stdlib" }
                
                val kotlinVersion = kotlinStdlib?.moduleVersion
                    ?: (project.findProperty("kotlinVersion") as? String)

                // 构建库列表
                val libraries = mutableListOf<String>()
                
                // 添加 Kotlin 库（如果找到版本）
                // 始终添加 kotlin-stdlib 和 kotlin-reflect，因为 TabooLib 可能会自动检测并尝试加载它们
                if (kotlinVersion != null) {
                    libraries.add("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                    libraries.add("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
                }
                
                // 添加 Exposed 库
                libraries.addAll(exposedDependencies)

                if (libraries.isEmpty()) {
                    println("ℹ️ [Klos] No libraries to inject, skipping.")
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
                            libraries.forEach { dep ->
                                libConfigBuilder.append("  - $dep\n")
                            }

                            Files.write(
                                pluginYmlPath,
                                libConfigBuilder.toString().toByteArray(Charsets.UTF_8),
                                StandardOpenOption.APPEND
                            )
                            val kotlinCount = if (kotlinVersion != null) 2 else 0
                            val exposedCount = exposedDependencies.size
                            println("✅ [Klos] Injected ${kotlinCount} Kotlin and ${exposedCount} Exposed libraries into plugin.yml")
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
