// 口木科技 Komutech —— 独立 Slimefun 附属插件构建脚本（对齐 WorldTaste-Plugin 结构）
// 仅依赖 Paper API 与本地 Slimefun / JEG jar，不联网下载 Slimefun。
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
}

group = "tech.komutech"
version = "3.0.1-standalone"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(21)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // 本地 Slimefun 依赖（compileOnly，运行期由服务器提供）
    compileOnly(files("libs/Slimefun4-347d926-Beta.jar"))
    // JEG（JustEnoughGuide）软依赖
    compileOnly(files("libs/JEG.jar"))
}

configurations.compileClasspath {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
}

// 把 content/ 下的内容 YAML 与数值配置模板一并打入 jar（插件运行期从自身资源读取）
// default_config.yml 是主配置模板；4 个 json 是数值配置模板，
// 首次启动都会释放到 plugins/Komutech/（已存在则不覆盖，服主改动优先）
val contentYaml = listOf(
    "groups.yml", "recipe_types.yml", "items.yml", "machines.yml", "foods.yml",
    "mob_drops.yml", "geo_resources.yml", "recipe_machines.yml", "mb_machines.yml",
    "linked_recipe_machines.yml", "template_machines.yml", "workbenches.yml", "menus.yml",
    "armors.yml", "capacitors.yml", "mat_generators.yml", "generations.yml",
    "researches.yml", "default_config.yml",
    "卷轴属性.json", "灵杖属性.json", "蒲团配置.json", "属性加点限制.json"
)

tasks.processResources {
    filteringCharset = "UTF-8"
    from(rootProject.projectDir.resolve("content")) {
        include(contentYaml)
        into("")
    }
}

tasks.jar {
    archiveBaseName.set("Komutech")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.jar)
}
