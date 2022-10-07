plugins {
    id("java")
    id("maven-publish")
}

group = "com.nuix.proserv"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        // Nuix's internal artifactory
        url = uri("https://artifactory.uat.nuix.com/artifactory/vendor-maven-virtual")
    }

    //flatDir {
        // My local Java libs
    //    dirs("C:\\Projects\\Libraries\\Java_Libs")
    //}

    // Uncomment this if you need access to the Nuix Engine Library on the local machine.
    /*flatDir {
        dirs("C:\\Projects\\Libraries\\nuix-engine\\lib")
    }*/

    mavenLocal()

    // Uncomment the next line if you need plugins or libraries that aren't in Nuix Artifactory
    //mavenCentral()
}

dependencies {
    // Compile time pre-processor to make code a little cleaner...
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    // If using mavenLocal use this
    implementation("com.nuix.proserv:JavaRESTClient:1.0.3-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.8.9")

    // If using a file in a folder use this
    //implementation(":JavaRESTClient:1.0.1-SNAPSHOT")
    //implementation("commons-io:commons-io:2.8.0")
    //implementation("org.apache.logging.log4j:log4j-core:2.17.1")
    //implementation("org.apache.logging.log4j:log4j-api:2.17.1")
    //implementation("org.apache.httpcomponents:httpclient:4.5.13")
    //implementation("org.apache.httpcomponents:httpmime:4.5.13")
    //implementation("com.googlecode.json-simple:json-simple:1.1.1")

    testImplementation("junit:junit:4.13.2")


    //testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    //testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group.toString()
            artifactId = project.name
            version = version

            from(components["java"])
        }
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.test {
    useJUnit()
    maxHeapSize = "1G"
}