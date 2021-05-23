import proguard.gradle.ProGuardTask
import java.nio.charset.StandardCharsets

buildscript {
  dependencies {
    classpath("net.sf.proguard:proguard-gradle:${Version.PROGUARD}")
  }
}
plugins {
  id("org.openstreetmap.josm").version(Version.GRADLE_JOSM_PLUGIN)
  java
  `java-test-fixtures`
  jacoco
  pmd
}

repositories {
  mavenCentral()
}

dependencies {
  testFixturesApi("org.junit.jupiter:junit-jupiter-api:${Version.JUNIT}")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.JUNIT}")
  // The following two lines can be removed, once JOSM drops JUnit 4 support
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:${Version.JUNIT}")
  testCompileOnly("junit:junit:${Version.JUNIT4}")

  testImplementation("org.openstreetmap.josm:josm-unittest:SNAPSHOT"){ isChanging = true }
  testImplementation("com.github.tomakehurst:wiremock:${Version.WIREMOCK}")
  testImplementation("org.awaitility:awaitility:${Version.AWAITILITY}")
}

tasks.withType(JavaCompile::class) {
  options.encoding = StandardCharsets.UTF_8.name()
  sourceCompatibility = JavaVersion.VERSION_1_8.toString()
  targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

tasks.withType(Test::class) {
  useJUnitPlatform()
}

josm {
  initialPreferences.set("<tag key='rovas.developer' value='true'/>")
  josmCompileVersion = "17833" // "15660"
  manifest {
    description = "A plugin to keep track of the time spent for mapping. Can be used to report that time to https://rovas.app ."
    mainClass = "app.rovas.josm.RovasPlugin"
    minJosmVersion = "17238"
    /*
     * 17238: AbstractTextComponentValidator.addChangeListener()
     * 16438: MapFrame.getToggleDialog()
     */
    iconPath = "images/rovas_logo.svg"
  }
}

tasks.getByName("runJosm").dependsOn(
  tasks.register("minDist", ProGuardTask::class) {
    dependsOn(tasks.named("dist"))

    injars("build/dist/rovas.jar")
    libraryjars(mapOf("filter" to "!META-INF/**"), sourceSets.main.map { it.compileClasspath })
    outjars("build/.josm/userdata/plugins/rovas.jar")

    if (JavaVersion.current().isJava9Compatible) { // >= JDK9
      libraryjars("${System.getProperty("java.home")}/jmods")
    } else { // < JDK9
      libraryjars("${System.getProperty("java.home")}/lib/rt.jar")
    }

    printmapping()
    buildDir.resolve("proguard-mapping.txt").apply {
      writeText("app.rovas.josm.RovasDialog -> app.rovas.josm.aDialog:")
      applymapping(absolutePath)
    }
    overloadaggressively()
    keep("public class app.rovas.josm.RovasPlugin { *; }")
  }
)
