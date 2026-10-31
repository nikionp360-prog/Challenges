plugins {
    java
}

group = "dev.challenges"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper 26.3 ist (Stand Okt 2026) noch Beta. Falls die Build-Nummer nicht mehr existiert,
    // die aktuelle auf https://repo.papermc.io/ bzw. docs.papermc.io nachschlagen.
    compileOnly("io.papermc.paper:paper-api:26.3.build.143-beta")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") { expand("version" to project.version) }
}
