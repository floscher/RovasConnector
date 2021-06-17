
import java.net.URL
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory
import org.openstreetmap.josm.gradle.plugin.GitDescriber
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask

plugins {
  id("org.openstreetmap.josm").version(Version.GRADLE_JOSM_PLUGIN)
  id ("com.github.spotbugs").version(Version.GRADLE_SPOTBUGS_PLUGIN)
  java
  `java-test-fixtures`
  jacoco
  pmd
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
    /* To which `minJosmVersion` we updated and why:
     * 17717: Changeset.getCreatedAt() now returns an Instant instead of a Date
     * 17238: AbstractTextComponentValidator.addChangeListener()
     * 16438: MapFrame.getToggleDialog()
     */
    website = URL("https://wiki.openstreetmap.org/wiki/JOSM/Plugins/RovasConnector")
  }
  i18n {
    pathTransformer = getPathTransformer(projectDir, "gitlab.com/JOSM/plugin/RovasConnector")
  }
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
  options.encoding = Charsets.UTF_8.name()
  sourceCompatibility = JavaVersion.VERSION_1_8.toString()
  targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

tasks.withType(Test::class) {
  useJUnitPlatform()
  options {
    // autoloads the JosmTestRules extension (see `META-INF/services` in the test resources)
    jvmArgs("-Djunit.jupiter.extensions.autodetection.enabled=true")
  }
}

val generatedSrcDir = provider { buildDir }.map { it.resolve("generated/sources/main") }
sourceSets.main {
  java.srcDir(generatedSrcDir)
}

tasks {
  val generateBuildInfoClass by registering {
    val content = provider {
      """
package app.rovas.josm.gen;

import java.net.URL;
import ${ if (JavaVersion.current().isJava9Compatible) "javax.annotation.processing" else "javax.annotation" }.Generated;

import app.rovas.josm.util.UrlProvider;

/** Makes the plugin version and OSM wiki URL accessible at runtime. */
@Generated(value = "gradle", date = "${Instant.now()}")
public final class BuildInfo {
  public static final URL OSM_WIKI_URL = UrlProvider.uncheckedURL("${josm.manifest.website}");
  public static final String VERSION_NAME = "${GitDescriber(projectDir).describe(trimLeading = true)}";
  private BuildInfo() {
    // private constructor to prevent instantiation
  }
}
""".trimIndent()
    }
    val file = generatedSrcDir.map { it.resolve("app/rovas/josm/gen/BuildInfo.java") }

    inputs.property("content", content)
    outputs.file(file)
    actions = listOf(
      Action {
        file.get().apply {
          parentFile.mkdirs()
          writeText(content.get())
        }
      }
    )
  }
  withType(JavaCompile::class) {
    dependsOn(generateBuildInfoClass)
  }
}

/**
 * Setting up tools
 */

// Javadoc
tasks.withType(Javadoc::class) {
  isFailOnError = false
  (options as StandardJavadocDocletOptions).links("https://josm.openstreetmap.de/doc")
}

// JaCoCo code coverage
tasks.withType(JacocoReport::class) {
  reports.xml.isEnabled = true
  reports.html.isEnabled = true
}

// PMD
pmd {
  ruleSets.clear()
  ruleSetConfig = resources.text.fromFile(projectDir.resolve("config/pmd.xml"))
  isIgnoreFailures = true
  toolVersion = Version.PMD
}

// SpotBugs
spotbugs {
  effort.set(Effort.MAX_VALUE)
  ignoreFailures.set(true)
  reportLevel.set(Confidence.MAX_VALUE)
  toolVersion.set(Version.SPOTBUGS)
}
tasks.withType(SpotBugsTask::class) {
  reports.register("XML")
  doLast {
    XPathFactory.newInstance().newXPath().evaluate(
      "/BugCollection/FindBugsSummary/attribute::total_bugs",
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
        reports.first().destination
      )
    )
      ?.takeIf {
        it.matches("^0|[1-9][0-9]*$".toRegex())
      }
      ?.let {
        buildDir
          .resolve("reports/spotbugs/${this@withType.baseName}.txt")
          .writeText(it)
      } ?: throw TaskExecutionException(this, Exception("Could not extract bug count!"))
  }
}
