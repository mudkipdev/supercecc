plugins {
    kotlin("jvm") version "2.0.0"
    application
}

application.mainClass = "com.mooncell07.cecc.MainKt"
group = "com.mooncell07"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}