pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "HalfOf2"
            url = uri("https://storage.googleapis.com/devan-maven/")
        }
        maven {
            name = "MinecraftForge"
            url = uri("https://files.minecraftforge.net/maven")
        }
        maven {
            url = uri("https://maven.hydos.cf/releases")
        }
    }
}
include("fabric-registries-v1")
