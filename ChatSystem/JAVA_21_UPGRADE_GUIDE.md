# Java 21 LTS Upgrade Guide

## Overview
This project has been upgraded to use Java 21 LTS (Long Term Support) from the previous Java version.

## What Was Changed

### 1. Build Configuration
- **pom.xml** (NEW): Created Maven build configuration with Java 21 settings
  - Source/Target: Java 21
  - Updated maven-compiler-plugin to version 3.13.0
  - Updated all dependencies to Java 21-compatible versions:
    - Java-WebSocket: 1.5.3 → 1.5.7
    - Gson: 2.8.9 → 2.11.0
    - SLF4J: 1.7.36 → 2.0.16

### 2. Eclipse Configuration
- **.classpath**: Updated JRE container to JavaSE-21
- **.settings/org.eclipse.jdt.core.prefs** (NEW): Added Java 21 compiler preferences

### 3. Build Scripts
- **start-server.bat**: Added Java version detection
- **start-client.bat**: Added Java version detection
- **run-websocket-server.bat**: Added Java version detection

## Prerequisites

### Install Java 21
You need to install Java 21 JDK to run this project. Choose one:

#### Option 1: Oracle JDK 21 (Recommended)
Download from: https://www.oracle.com/java/technologies/downloads/#java21

#### Option 2: OpenJDK 21
- **Adoptium (Eclipse Temurin)**: https://adoptium.net/
- **Microsoft Build of OpenJDK**: https://learn.microsoft.com/en-us/java/openjdk/download
- **Amazon Corretto 21**: https://aws.amazon.com/corretto/

### Set Up Environment Variables
After installing Java 21:

1. Set `JAVA_HOME` to your JDK 21 installation directory:
   ```
   JAVA_HOME=C:\Program Files\Java\jdk-21
   ```

2. Add to PATH:
   ```
   PATH=%JAVA_HOME%\bin;%PATH%
   ```

3. Verify installation:
   ```
   java -version
   javac -version
   ```
   Both should show version 21.x.x

## Building with Maven

You can now build the project using Maven:

```bash
# Compile the project
mvn clean compile

# Package as JAR
mvn clean package

# Run tests (if any)
mvn test
```

## Building with Batch Scripts (Traditional Method)

The existing batch files continue to work:

```bash
# Start the chat server
start-server.bat

# Start a chat client
start-client.bat

# Start the WebSocket bridge
run-websocket-server.bat
```

## Updating Dependencies (Optional)

If you want to use Maven to manage dependencies instead of the `lib/` folder:

1. Delete old JARs from `lib/` folder
2. Run: `mvn dependency:copy-dependencies -DoutputDirectory=lib`
3. Maven will download the latest compatible versions

## Compatibility Notes

### Java 21 Features Available
You can now use modern Java features:
- **Pattern Matching for switch** (Java 21)
- **Record Patterns** (Java 21)
- **Virtual Threads** (Java 21)
- **Sequenced Collections** (Java 21)
- **String Templates** (Preview in Java 21)

### Breaking Changes
Java 21 includes some breaking changes from older versions:
- Finalization has been deprecated for removal
- Security Manager is deprecated and will be removed
- Some internal APIs are no longer accessible

## Troubleshooting

### Compilation Errors
If you encounter compilation errors:
1. Verify Java 21 is installed: `java -version`
2. Clear compiled classes: Delete `bin/` folder
3. Rebuild the project

### Runtime Errors
If you encounter runtime errors:
1. Check that `JAVA_HOME` points to Java 21
2. Verify classpath includes all required JARs
3. Check for dependency version conflicts

### Eclipse Issues
If Eclipse shows errors:
1. Right-click project → Properties → Java Compiler
2. Set compliance level to 21
3. Right-click project → Maven → Update Project

## Benefits of Java 21 LTS

1. **Performance**: Improved JVM performance and memory management
2. **Virtual Threads**: Lightweight concurrency for better scalability
3. **Pattern Matching**: More expressive and safer code
4. **Security**: Latest security patches and updates
5. **Long-Term Support**: Oracle provides support until September 2031

## Additional Resources

- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/)
- [What's New in Java 21](https://www.oracle.com/java/technologies/javase/21-relnotes.html)
- [Java 21 API Documentation](https://docs.oracle.com/en/java/javase/21/docs/api/)

## Rollback Instructions

If you need to revert to the previous Java version:
1. Restore `pom.xml` and update source/target to previous version
2. Update `.classpath` JRE container reference
3. Install and configure the previous JDK version
4. Recompile the project
