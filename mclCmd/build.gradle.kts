plugins {
    application
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("gov.nih.opa.mcl.MCLCmd")
}

tasks.getByName<Zip>("shadowDistZip").archiveClassifier.set("shadow")
tasks.getByName<Tar>("shadowDistTar").archiveClassifier.set("shadow")

dependencies {
    implementation(project(":mcl"))
    implementation(project(":spreadsheet"))
    implementation(libs.jcommander)
}

