plugins {
    id("java")
}

group = "me.ajh123"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    listOf("fnuecke/buildroot", "fnuecke/ceres", "fnuecke/sedna").forEach { repo ->
        maven {
            url = uri("https://maven.pkg.github.com/$repo")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("li.cil.sedna:sedna:2.0.9")
    implementation("li.cil.sedna:sedna-buildroot:0.0.8")
}

tasks.test {
    useJUnitPlatform()
}