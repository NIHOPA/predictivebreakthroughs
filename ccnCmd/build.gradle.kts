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

val ccnFirstOrderScriptTask = tasks.register<CreateStartScripts>("createCCNFirstOrderScript") {
    applicationName = "ccnFirstOrder"
    mainClass.set("gov.nih.opa.ccn.CCNFirstOrder")
    outputDir = ccnScriptTask.outputDir
    classpath = ccnScriptTask.classpath

    doLast {
        val unixScriptFile = file(unixScript)
        val text = unixScriptFile.readText(Charsets.UTF_8)
        val newText = text.replace("APP_HOME=\"`pwd -P`\"", "export APP_HOME=\"`pwd -P`\"")
        unixScriptFile.writeText(newText, Charsets.UTF_8)
    }
}

val ccnPMIDsScriptTask = tasks.register<CreateStartScripts>("createCCNPMIDsScript") {
    applicationName = "ccnPMIDs"
    mainClass.set("gov.nih.opa.ccn.CCNPMIDs")
    outputDir = ccnScriptTask.outputDir
    classpath = ccnScriptTask.classpath

    doLast {
        val unixScriptFile = file(unixScript)
        val text = unixScriptFile.readText(Charsets.UTF_8)
        val newText = text.replace("APP_HOME=\"`pwd -P`\"", "export APP_HOME=\"`pwd -P`\"")
        unixScriptFile.writeText(newText, Charsets.UTF_8)
    }
}

tasks.register("autocompleteDir") {
    doLast {
        mkdir("${layout.buildDirectory.get()}/autocomplete")
    }
}

task("picoCliCCNFirstOrderAutoComplete", JavaExec::class) {
    dependsOn("autocompleteDir")
    mainClass.set("picocli.AutoComplete")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--force", "--completionScript", "${layout.buildDirectory.get()}/autocomplete/ccnFirstOrder.sh", "gov.nih.opa.ccn.CCNFirstOrder")
}

task("picoCliCCNPMIDsAutoComplete", JavaExec::class) {
    dependsOn("autocompleteDir")
    mainClass.set("picocli.AutoComplete")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--force", "--completionScript", "${layout.buildDirectory.get()}/autocomplete/ccnPMIDs.sh", "gov.nih.opa.ccn.CCNPMIDs")
}

tasks.withType<AbstractArchiveTask> {
    dependsOn(
        "picoCliCCNFirstOrderAutoComplete",
        "picoCliCCNPMIDsAutoComplete",
    )
}

distributions {
    main {
        contents {
            from(ccnFirstOrderScriptTask) {
                into("bin")
            }
            from(ccnPMIDsScriptTask) {
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
