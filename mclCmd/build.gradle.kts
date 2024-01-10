plugins {
    application
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("com.lexicalintelligence.mcl.MCLCmd")
}

tasks.getByName<Zip>("shadowDistZip").archiveClassifier.set("shadow")
tasks.getByName<Tar>("shadowDistTar").archiveClassifier.set("shadow")

val knowledgeVersion: String by project

dependencies {
    implementation(project(":mcl"))
    implementation(project(":spreadsheet"))
    implementation(libs.jcommander)
}

