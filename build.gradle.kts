import org.openstreetmap.josm.gradle.plugin.GitDescriber
import proguard.gradle.ProGuardTask
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant

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

pmd {
  ruleSets.clear()
  ruleSetConfig = resources.text.fromFile(projectDir.resolve("config/pmd.xml"))
  isIgnoreFailures = true
  toolVersion = Version.PMD
}

repositories {
  mavenCentral()
}

dependencies {
  testFixturesApi("org.junit.jupiter:junit-jupiter-api:${Version.JUNIT}")
  testFixturesApi("com.github.tomakehurst:wiremock:${Version.WIREMOCK}")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.JUNIT}")
  // The following two lines can be removed, once JOSM drops JUnit 4 support
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:${Version.JUNIT}")
  testCompileOnly("junit:junit:${Version.JUNIT4}")

  testImplementation("org.openstreetmap.josm:josm-unittest:SNAPSHOT"){ isChanging = true }
  testImplementation("org.awaitility:awaitility:${Version.AWAITILITY}")
}

tasks.withType(JavaCompile::class) {
  options.encoding = StandardCharsets.UTF_8.name()
  sourceCompatibility = JavaVersion.VERSION_1_8.toString()
  targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

tasks.withType(Test::class) {
  useJUnitPlatform()
  options {
    jvmArgs("-Djunit.jupiter.extensions.autodetection.enabled=true")
  }
}
tasks.withType(JacocoReport::class) {
  reports.xml.isEnabled = true
  reports.html.isEnabled = true
}

val generatedSrcDir = buildDir.resolve("generated/sources/main")
sourceSets {
  main {
    java {
      srcDir(generatedSrcDir)
    }
  }
}

val generatePluginVersionClass by tasks.registering {
  val content = """
    package app.rovas.josm.gen;
    import javax.annotation.Generated;
    /** Makes the plugin version accessible at runtime. */
    @Generated(value = "gradle", date = "${Instant.now()}")
    public final class PluginVersion {
      public static final String VERSION_NAME = "${GitDescriber(projectDir).describe(trimLeading = true)}";
      private PluginVersion() {
        // private constructor to prevent instantiation
      }
    }""".trimIndent()
  val file = generatedSrcDir.resolve("app/rovas/josm/gen/PluginVersion.java")

  inputs.property("content", content)
  outputs.file(file)
  actions = listOf(
    Action {
      file.parentFile.mkdirs()
      file.writeText(content)
    }
  )
}

tasks.withType(Javadoc::class) {
  options {
    (this as StandardJavadocDocletOptions).links("https://josm.openstreetmap.de/doc")
  }
}

tasks.withType(JavaCompile::class) {
  dependsOn(generatePluginVersionClass)
}
josm {
  initialPreferences.set("<tag key='rovas.developer' value='true'/>")
  josmCompileVersion = "17919"
  manifest {
    author = "Florian Schäfer (floscher)"
    canLoadAtRuntime = true
    description = "A plugin to keep track of the time spent for mapping. Can be used to report that time to https://rovas.app ."
    iconPath = "images/rovas_logo.svg"
    mainClass = "app.rovas.josm.RovasPlugin"
    minJosmVersion = "17717"
    /*
     * 17717: Changeset.getCreatedAt() now returns an Instant instead of a Date
     * 17238: AbstractTextComponentValidator.addChangeListener()
     * 16438: MapFrame.getToggleDialog()
     */
    website = URL("https://wiki.openstreetmap.org/wiki/JOSM/Plugins/RovasConnector")
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
      writeText("app.rovas.josm.gui.RovasDialog -> app.rovas.josm.gui.aDialog:")
      applymapping(absolutePath)
    }
    overloadaggressively()
    keep("public class app.rovas.josm.RovasPlugin { *; }")
  }
)
