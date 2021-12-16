import io.github.astrarre.amalgamation.gradle.dependencies.LibrariesDependency
import io.github.astrarre.amalgamation.gradle.dependencies.remap.SingleRemapDependency
import io.github.astrarre.amalgamation.gradle.plugin.minecraft.MinecraftAmalgamation
import io.github.astrarre.amalgamation.gradle.tasks.remap.RemapJar as RemapJarType
import io.github.astrarre.amalgamation.gradle.tasks.remap.RemapSourcesJar as RemapSourcesJarType

plugins {
    id("maven-publish")
    id("java-library")
    id("amalgamation-minecraft") version "3.0.0"
}

version = properties["mod_version"]!!
group = properties["maven_group"]!!

class Version {
    lateinit var mc: SingleRemapDependency
    lateinit var libs: LibrariesDependency
}

var map = HashMap<String, Version>()

allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.hydos.cf/releases")
        maven("https://maven.legacyfabric.net")
        maven("https://maven.fabricmc.net")
    }
}

subprojects {
    group = "net.legacyfabric"
    apply(plugin = "amalgamation-minecraft")
    apply(plugin = "java-library")

    dependencies {
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        withSourcesJar()
    }

    val accessWidener = project.properties["accessWidener"] as String?
    configure(project, "1.9.4", "1.8.9-1.12.2", accessWidener)
    configure(project, "1.13.2", "1.13.2", accessWidener)
    configure(project, "1.9.4", "main", accessWidener)

    configurations {
        annotationProcessor.get().extendsFrom(implementation.get())
        annotationProcessor.get().extendsFrom(api.get())
        //testmodAnnotationProcessor.extendsFrom(annotationProcessor)
    }
}

fun configure(project: Project, version: String, name: String = version, accessWidener: String? = null) {
    val src = project.sourceSets.maybeCreate(name)
    val libs = project.configurations.maybeCreate("minecraftLibraries$version") // we have to do this conq because maven-publish moment
    project.configurations.getByName(src.compileOnlyConfigurationName).extendsFrom(libs)
    val vers = version(version)
    val isMain = name == "main"

    // kotlin dsl makes this so much more pain than it needs to be

    if(accessWidener == null) {
        vers.mc.forEach {
            project.dependencies.add(src.implementationConfigurationName, it)
        }
    } else {
        (ag.accessWidener(vers.mc) {
            accessWidener(files("${project.projectDir}/src/main/resources/$accessWidener"))
        } as Iterable<*>).forEach {
            project.dependencies.add(src.implementationConfigurationName, it!!)
        }
    }

    vers.libs.forEach {
        // have to use CompileOnly to avoid it showing up in the pom
        project.dependencies.add(libs.name, it)
    }

    ag.fabricLoader("0.12.12").forEach {
        project.dependencies.add(src.implementationConfigurationName, it)
    }

    if(!isMain) {
        project.dependencies.add(src.implementationConfigurationName, project.sourceSets.getByName("main").output)
        project.tasks.create(src.jarTaskName, Jar::class.java) {
            this.with(project.tasks.getByName("jar") as CopySpec) // include main stuff
            this.from(src.output)
        }
        project.tasks.create(src.sourcesJarTaskName, Jar::class.java) {
            this.from(src.allSource)
            this.from(project.sourceSets.getByName("main").allSource)
        }
    }

    project.tasks.create(name + "RemapJar", RemapJarType::class.java) {
        mappings(ag.intermediary(version).forward(), "named", "intermediary")
        remapAw()
        with(project.tasks.getByName(src.jarTaskName) as CopySpec)
    }

    project.tasks.create(name + "RemapSourcesJar", RemapSourcesJarType::class.java) {
        mappings(ag.intermediary(version).forward(), "named", "intermediary")
        with(project.tasks.getByName(src.sourcesJarTaskName) as CopySpec)
    }

    project.tasks.create(name + "RunClient", JavaExec::class.java) {
        group = "Minecraft"
        classpath += src.runtimeClasspath
        classpath += libs
        main = "net.fabricmc.loader.launch.knot.KnotClient"
        val natives = ag.natives(version)
        systemProperty("fabric.development", true)
        systemProperty("fabric.gameVersion", version)
        systemProperty("java.library.globalCache", natives)
        systemProperty("org.lwjgl.librarypath", natives)
        val assets = ag.assets(version)
        args("--assetIndex", assets.getAssetIndex(), "--assetsDir", assets.getAssetsDir())
        workingDir("$rootDir/run")
        dependsOn(project.tasks.getByName(src.classesTaskName))
    }
}

fun version(version: String): Version {
    return map.computeIfAbsent(version) {
        val vers = Version()
        project.configure<MinecraftAmalgamation> {
            val ag = this
            map {
                mappings("net.fabricmc:yarn:${properties["yarn_mappings"]}:v2", "intermediary", "named")
                vers.mc = inputLocal(ag.map {
                    mappings(ag.intermediary(version))
                    inputLocal(ag.merged(version) {
                        client = ag.client(version)
                        server = ag.server(version)
                    })
                }) as SingleRemapDependency
            }
            vers.libs = ag.libraries(version) as LibrariesDependency
        }
        vers
    }
}