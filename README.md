swarm4j
=======

Java implementation of Swarm (see: [SwarmJS](https://github.com/gritzko/swarm))

## Building

`mvn clean install`

Maven3 must be installed

## Modules

  * [swarm4j-core](swarm4j-core) - core classes
  * [swarm4j-server](swarm4j-server) - server specific classes
  * [swarm4j-client](swarm4j-client) - client specific classes
  * [swarm4j-android](swarm4j-android) - contains maven build script to build the project for android platform

## Usage

### in Java-client

pom.xml:

```xml
    <dependency>
        <groupId>swarm4j</groupId>
        <artifactId>swarm4j-client</artifactId>
        <version>0.3.0-SNAPSHOT</version>
    </dependency>
```

### in Java-server

pom.xml:

```xml
    <dependency>
        <groupId>swarm4j</groupId>
        <artifactId>swarm4j-server</artifactId>
        <version>0.3.0-SNAPSHOT</version>
    </dependency>
```

### in Android application

build.gradle:

```groovy
repositories {
    mavenLocal()
}

dependencies {
    compile "com.eclipsesource.minimal-json:minimal-json:0.9.2-SNAPSHOT"
    compile "org.java-websocket:Java-WebSocket:1.3.0"
    compile "swarm4j:swarm4j-android:0.3.0-SNAPSHOT"
    compile "org.slf4j:slf4j-api:1.7.6"
    compile "org.slf4j:slf4j-android:1.7.6"
}
```
