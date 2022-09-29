plugins {
    id("java")
}

group = "com.nuix.proserv"
version = "1.0-SNAPSHOT"

repositories {
    //mavenCentral()

    maven {
        // Nuix's internal artifactory
        url = uri("https://artifactory.uat.nuix.com/artifactory/vendor-maven-virtual")
    }

    flatDir {
        // My local Java libs
        dirs("C:\\Projects\\Libraries\\Java_Libs")
    }

    // Nuix Engine local folder
    /*flatDir {
        dirs("C:\\Projects\\Libraries\\nuix-engine\\lib")
    }*/
}

dependencies {
    implementation(":JavaRESTClient:1.0.1-SNAPSHOT")
    implementation("commons-io:commons-io:2.8.0")
    implementation("org.projectlombok:lombok:1.18.24")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.apache.logging.log4j:log4j-api:2.17.2")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("org.apache.httpcomponents:httpmime:4.5.13")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")

    testImplementation("junit:junit:4.13.2")

/*
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
*/
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}