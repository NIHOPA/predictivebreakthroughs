plugins {
    application
    `java-library`
}

description = "CCN CMD"

defaultTasks("build", "installDist")

dependencies {
    implementation(project(":citationNetworkCompute"))
    implementation(project(":spreadsheet"))
    annotationProcessor(libs.picocli.codegen)
    implementation(libs.picocli.base)
}

val ccnScriptTask = tasks.getByName<CreateStartScripts>("startScripts")
ccnScriptTask.applicationName = "ccn"
ccnScriptTask.mainClass.set("gov.nih.opa.ccn.CCN")


tasks.register("autocompleteDir") {
    doLast {
        mkdir("${layout.buildDirectory.get()}/autocomplete")
    }
}

task("picoCliCCNDAutoComplete", JavaExec::class) {
    dependsOn("autocompleteDir")
    mainClass.set("picocli.AutoComplete")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--force", "--completionScript", "${layout.buildDirectory.get()}/autocomplete/ccn.sh", "gov.nih.opa.ccn.CCN")
}

tasks.withType<AbstractArchiveTask> {
    dependsOn(
        "picoCliCCNDAutoComplete",
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
