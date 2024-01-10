val utilVersion: String by project


dependencies {
    implementation(project(":spreadsheet"))
    api(libs.koloboke.api)
    implementation(libs.koloboke.impl)
}

