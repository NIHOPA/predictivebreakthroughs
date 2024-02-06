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

val ccnDScriptTask = tasks.register<CreateStartScripts>("createCCNDScript") {
    applicationName = "ccnD"
    mainClass.set("gov.nih.opa.ccn.CCND")
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

task("picoCliCCNDAutoComplete", JavaExec::class) {
    dependsOn("autocompleteDir")
    mainClass.set("picocli.AutoComplete")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--force", "--completionScript", "${layout.buildDirectory.get()}/autocomplete/ccnD.sh", "gov.nih.opa.ccn.CCND")
}

tasks.withType<AbstractArchiveTask> {
    dependsOn(
        "picoCliCCNDAutoComplete",
    )
}

distributions {
    main {
        contents {
            from(ccnDScriptTask) {
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
