plugins {
    application
    java
}

group = "com.imdbselector"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

val jooqCodegen by configurations.creating

dependencies {
    // Web / HTTP
    implementation("io.javalin:javalin:6.4.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    // Persistence & Migrations
    implementation("org.jooq:jooq:3.19.18")
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-core:11.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.3.0")

    jooqCodegen("org.jooq:jooq-codegen:3.19.18")
    jooqCodegen("org.jooq:jooq-meta-extensions:3.19.18")
    jooqCodegen("com.h2database:h2:2.3.232")

    // Auth & Security
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("org.mindrot:jbcrypt:0.4")

    // CSV Parsing
    implementation("com.opencsv:opencsv:5.9")

    // Testing & Architecture Guardrails
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
}

val jooqOutputDir = layout.buildDirectory.dir("generated-src/jooq/main")

sourceSets {
    main {
        java.srcDir(jooqOutputDir)
    }
}

val generateJooq by tasks.registering(JavaExec::class) {
    group = "jooq"
    description = "Generates jOOQ classes from Flyway SQL migrations"
    classpath = jooqCodegen
    mainClass.set("org.jooq.codegen.GenerationTool")
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    args("jooq-config.xml")
    inputs.files(fileTree("src/main/resources/db/migration"), file("jooq-config.xml"))
    outputs.dir(jooqOutputDir)
}

tasks.compileJava {
    dependsOn(generateJooq)
}

application {
    mainClass.set(
        providers.gradleProperty("mainClass")
            .orElse("com.imdbselector.infrastructure.adapter.in.http.AppServer")
    )
}

tasks.register<JavaExec>("migrateLegacy") {
    group = "application"
    description = "Migrates legacy JSON user and watchlist files into PostgreSQL"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.imdbselector.infrastructure.adapter.in.cli.LegacyJsonMigrator")
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

tasks.test {
    useJUnitPlatform()
}
