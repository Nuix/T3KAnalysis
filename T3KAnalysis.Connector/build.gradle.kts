plugins {
    id("java")
    id("maven-publish")
}

group = "com.nuix.proserv"
version = "1.0-SNAPSHOT"

val sourceCompatibility = 11
val targetCompatibility = 11

repositories {
    maven {
        // Nuix's internal artifactory
        url = uri("https://artifactory.uat.nuix.com/artifactory/vendor-maven-virtual")
    }

    /*flatDir {
        // My local Java libs
        dirs("C:\\Projects\\Libraries\\Java_Libs")
    }*/

    // Uncomment this if you need access to the Nuix Engine Library on the local machine.
    /*flatDir {
        dirs("C:\\Projects\\Libraries\\nuix-engine\\lib")
    }*/

    // Uncomment the next line if you want to use the local maven, useful to chain local modules
    mavenLocal()

    // Uncomment the next line if you need plugins or libraries that aren't in Nuix Artifactory
    //mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    implementation("com.nuix.proserv:T3KAnalysis:1.0-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.8.9")

//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.gradle.sample"
            artifactId = "library"
            version = "1.1"

            from(components["java"])
        }
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}