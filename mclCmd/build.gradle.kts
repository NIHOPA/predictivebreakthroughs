plugins {
    application
    `java-library`
}

description = "CLI for running MCL operations."

defaultTasks("build", "installDist")

dependencies {
    implementation(project(":mcl"))
    implementation(project(":spreadsheet"))
    implementation(project(":picoCommon"))
}

val ccnScriptTask = tasks.getByName<CreateStartScripts>("startScripts")
ccnScriptTask.applicationName = "mcl"
ccnScriptTask.mainClass.set("gov.nih.opa.mcl.MCL")

tasks.register("autocompleteDir") {
    doLast {
        mkdir("${layout.buildDirectory.get()}/autocomplete")
    }
}

task("picoCliMCLAutoComplete", JavaExec::class) {
    dependsOn("autocompleteDir")
    mainClass.set("picocli.AutoComplete")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--force", "--completionScript", "${layout.buildDirectory.get()}/autocomplete/mcl.sh", "gov.nih.opa.mcl.MCL")
}

tasks.withType<AbstractArchiveTask> {
    dependsOn(
        "picoCliMCLAutoComplete",
    )
}

distributions {
    main {
        contents {
            from(ccnScriptTask) {
                into("bin")
            }
            from("${layout.buildDirectory.get()}/autocomplete/") {
                into("bin/autocomplete")
            }

            fileMode = 777
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

    }
}