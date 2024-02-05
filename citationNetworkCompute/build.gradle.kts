plugins {
    application
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":spreadsheet"))
    api(libs.guava)
    api(libs.koloboke.api)
    api(libs.mongodb.driver.sync)
    implementation(libs.koloboke.impl)
}

application {
    mainClass.set("gov.nih.opa.ccn.driver.CocitationVectorComputeDriverAllVsCCN")
}