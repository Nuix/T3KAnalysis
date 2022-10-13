plugins {
    id("java")
}

group = "com.nuix.proserv"
version = "1.0.3-SNAPSHOT"
val sourceCompatibility = 11
val targetCompatibility = 11
val nuixEngineVersion = "9.+"


repositories {
    maven {
        // Nuix's internal artifactory copy
        url = uri("https://artifactory.uat.nuix.com/artifactory/vendor-maven-virtual")
    }

    // Uncomment this for a repo with the Nuix content.  Can replace the internal cache as well?
    /*maven {
        name = "nuixInternal"
        url = uri("https://artifactory.uat.nuix.com/ui/native/rest-maven-virtual")
    }*/

    // My local Java libs
    /*flatDir {
        dirs("C:\\Projects\\Libraries\\Java_Libs")
    }*/

    // Uncomment this if you need access to the Nuix Engine Library on the local machine.
    flatDir {
        dirs("C:\\Projects\\Libraries\\nuix-engine\\lib")
    }

    // Uncomment the next line if you want to use the local maven, useful to chain local modules
    mavenLocal()

    // Uncomment the next line if you need plugins or libraries that aren't in Nuix Artifactory
    //mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.10")

    implementation("com.nuix.proserv:T3KAnalysis:1.0.6-SNAPSHOT")
    implementation("com.nuix.proserv:T3KAnalysis.Connector:1.0.5-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.apache.logging.log4j:log4j-api:2.17.1")
    implementation("org.apache.logging.log4j:log4j-core:2.17.1")
    implementation("org.apache.commons:commons-lang3:3.4")
    implementation(":nuix-scripting-api:$nuixEngineVersion")
    implementation(":nuix-scripting-impl:$nuixEngineVersion")

    testImplementation("com.nuix.proserv:EngineTestHarness:1.0-SNAPSHOT")
    testImplementation("commons-io:commons-io:2.8.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.test {
    useJUnit()
    maxHeapSize = "10G"
}