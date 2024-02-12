plugins {
    application
    `java-library`
}

dependencies {
    annotationProcessor(libs.picocli.codegen)
    api(libs.picocli.base)
}