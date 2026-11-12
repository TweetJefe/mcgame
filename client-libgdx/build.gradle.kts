plugins { alias(libs.plugins.kotlinMultiplatform) }

kotlin {
    jvm()
    jvmToolchain(21)
    sourceSets {
        jvmMain.dependencies {
            implementation(project(":engine"))
            implementation("com.badlogicgames.gdx:gdx:1.14.2")
            implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.2")
            implementation("com.badlogicgames.gdx:gdx-freetype:1.14.2")
            runtimeOnly("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop")
            runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:1.14.2:natives-desktop")
        }
        jvmTest.dependencies { implementation(libs.junit) }
    }
}

tasks.named<Copy>("jvmProcessResources") {
    from(rootProject.file("art")) { into("art"); include("**/*.png") }
    from(rootProject.file("docs/heroes_abilities.txt")) { into("guides") }
}

tasks.register<JavaExec>("run") {
    group = "application"
    description = "Локальный матч Moba Chess на LibGDX"
    dependsOn("jvmMainClasses")
    mainClass.set("org.mobachess.desktop.DesktopLauncherKt")
    classpath(kotlin.targets["jvm"].compilations["main"].output.allOutputs, configurations["jvmRuntimeClasspath"])
    workingDir(rootProject.projectDir)
    providers.gradleProperty("capture").orNull?.let { systemProperty("mobachess.capture", it) }
    if (providers.gradleProperty("smoke").orNull == "true") systemProperty("mobachess.smoke", "true")
    if (providers.gradleProperty("flowSmoke").orNull == "true") systemProperty("mobachess.flowSmoke", "true")
}
