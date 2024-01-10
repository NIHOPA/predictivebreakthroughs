plugins {
    java
    `java-library`
    idea
    signing
    `maven-publish`
    alias(libs.plugins.reckon)
}

reckon {
    setDefaultInferredScope("patch")
    setScopeCalc(calcScopeFromProp())
    snapshots()
    setStageCalc(calcStageFromProp())
}

allprojects {
    group = "gov.nih.opa"
}


val Project.libs by lazy {
    the<org.gradle.accessors.dm.LibrariesForLibs>()
}

defaultTasks("build")
subprojects {

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "idea")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    val sourcesJar = tasks.register<Jar>("sourcesJar") {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allJava)
    }

    val javadocJar = tasks.register<Jar>("javadocJar") {
        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
        archiveClassifier.set("javadoc")
        from(tasks.javadoc)
    }

    artifacts.add("archives", sourcesJar)
    artifacts.add("archives", javadocJar)

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])
                pom {
                    url.set("https://dpcpsi.nih.gov/opa")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("mdavis")
                            name.set("Matt Davis")
                            email.set("matthew.davis3@nih.gov")
                            organization.set("NIH/DPCPSI/OPA")
                            organizationUrl.set("https://dpcpsi.nih.gov/opa")
                        }
                    }
                    scm {
                        connection.set("git@github.com:NIHOPA/predictive_breakthroughs.git")
                        developerConnection.set("git@github.com:NIHOPA/predictive_breakthroughs.git")
                        url.set("https://github.com/NIHOPA/predictive_breakthroughs")
                    }
                    name.set(project.name)
                    description.set(project.name)
                }
            }
        }
    }

    group = "gov.nih.opa"

    repositories {
        mavenCentral()
    }


    dependencies {
        testImplementation(platform(libs.junit.bom))
        testImplementation(libs.jupiter.api)
        testImplementation(libs.jupiter.params)
        testRuntimeOnly(libs.jupiter.engine)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

}
