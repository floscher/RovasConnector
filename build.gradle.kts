import proguard.gradle.ProGuardTask

buildscript {
  dependencies {
    classpath("net.sf.proguard:proguard-gradle:6.3.0beta1")
  }
}
plugins {
  id("org.openstreetmap.josm").version("0.7.1")
  java
  `java-test-fixtures`
  jacoco
  pmd
}

dependencies {
  testFixturesApi("org.junit.jupiter:junit-jupiter:5.7.1")
}

tasks.withType(JavaCompile::class) {
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
}

tasks.withType(Test::class) {
  useJUnitPlatform()
}

repositories {
  mavenCentral()
}

josm {
  josmCompileVersion = "17833" // "15660"
  manifest {
    description = "A plugin to keep track of the time spent for mapping. Can be used to report that time to https://rovas.app ."
    mainClass = "app.rovas.josm.RovasPlugin"
    minJosmVersion = "16438" // MapFrame.getToggleDialog // "15650"
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
