plugins {
    id("java")
    id("fabric-loom").version("1.7-SNAPSHOT").apply(false)
}

subprojects {
    apply<JavaPlugin>()
    apply(plugin = "fabric-loom")
    repositories {
        mavenLocal()
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val archives_base_name: String by project
val mod_version: String by project

val buildAll = tasks.create("buildAll") {

    dependsOn(":v1_20:build")
    dependsOn(":v1_20_4:build")
    dependsOn(":v1_21:build")
    // This isn't working.... you still have to run each build individually
/*    tasks.findByName(":v1_19_3:build")?.mustRunAfter(":v1_19_4:build")
    tasks.findByName(":v1_19:build")?.mustRunAfter(":v1_19_3:build")
    tasks.findByName(":v1_18:build")?.mustRunAfter(":v1_19:build")
    tasks.findByName(":v1_17:build")?.mustRunAfter(":v1_18:build")*/

    doLast {
        println("Copying files...")
        file("v1_20/build/libs/v1_20.jar").copyTo(file("build/${archives_base_name}-1.20-${mod_version}.jar"), true)
        file("v1_20_4/build/libs/v1_20_4.jar").copyTo(file("build/${archives_base_name}-1.20.4-${mod_version}.jar"), true)
        file("v1_21/build/libs/v1_21.jar").copyTo(file("build/${archives_base_name}-1.21-${mod_version}.jar"), true)
    }
}
