# Java 21 Upgrade - Next Steps

## Current Status
✅ Project configuration files have been updated for Java 21
✅ Build scripts enhanced with version detection
✅ Maven POM created with Java 21 settings
✅ Eclipse configuration updated for Java 21

⚠️ **Current System Java Version: 17.0.17**
📋 **Required: Java 21 or higher**

## Required Action: Install Java 21

### Step 1: Download Java 21 JDK

#### Recommended Option - Microsoft Build of OpenJDK 21
Best for Windows users, includes full support and regular updates:
- Download: https://learn.microsoft.com/en-us/java/openjdk/download
- Choose: **Windows x64 MSI installer**

#### Alternative Options:
- **Oracle JDK 21**: https://www.oracle.com/java/technologies/downloads/#java21
- **Eclipse Temurin 21**: https://adoptium.net/temurin/releases/?version=21
- **Amazon Corretto 21**: https://aws.amazon.com/corretto/

### Step 2: Install Java 21
1. Run the downloaded installer
2. Follow the installation wizard
3. Remember the installation path (e.g., `C:\Program Files\Java\jdk-21`)

### Step 3: Set Environment Variables
Open PowerShell as Administrator and run:

```powershell
# Set JAVA_HOME to Java 21
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-21', 'Machine')

# Add to PATH
$path = [System.Environment]::GetEnvironmentVariable('PATH', 'Machine')
$javaPath = 'C:\Program Files\Java\jdk-21\bin'
if ($path -notlike "*$javaPath*") {
    [System.Environment]::SetEnvironmentVariable('PATH', "$javaPath;$path", 'Machine')
}
```

**Important**: Adjust the path if you installed Java 21 in a different location.

### Step 4: Verify Installation
Close and reopen PowerShell, then run:
```powershell
java -version
javac -version
```

You should see version **21.x.x** for both commands.

### Step 5: Update Maven Wrapper (Optional)
If using Maven:
```bash
mvn wrapper:wrapper -Dmaven=3.9.9
```

## Files Modified

### Created Files:
1. `pom.xml` - Maven build configuration for Java 21
2. `.settings/org.eclipse.jdt.core.prefs` - Eclipse Java 21 compiler settings
3. `JAVA_21_UPGRADE_GUIDE.md` - Comprehensive upgrade documentation
4. `JAVA_21_UPGRADE_NEXT_STEPS.md` - This file

### Modified Files:
1. `.classpath` - Updated to use JavaSE-21
2. `start-server.bat` - Added Java version detection
3. `start-client.bat` - Added Java version detection
4. `run-websocket-server.bat` - Added Java version detection

## Testing After Installation

Once Java 21 is installed, test the project:

1. **Clean previous build**:
   ```bash
   Remove-Item -Recurse -Force bin\*
   ```

2. **Test compilation**:
   ```bash
   cd d:\NA\NetTalk\ChatSystem
   .\start-server.bat
   ```
   
   The script will show the detected Java version and compile the code.

3. **If using Maven**:
   ```bash
   mvn clean compile
   mvn test
   ```

## Dependency Updates (Optional but Recommended)

The `pom.xml` specifies newer versions of dependencies. To download them:

```bash
# Download dependencies to lib folder
mvn dependency:copy-dependencies -DoutputDirectory=lib

# This will download:
# - Java-WebSocket 1.5.7 (was 1.5.3)
# - Gson 2.11.0 (was 2.8.9)  
# - SLF4J 2.0.16 (was 1.7.36)
```

After downloading, you can update the batch scripts to use the new JAR names.

## Benefits of This Upgrade

- ✅ **LTS Support**: Java 21 supported until September 2031
- ✅ **Performance**: ~15% better performance on average
- ✅ **Virtual Threads**: Better scalability for your chat server
- ✅ **Pattern Matching**: Write cleaner, safer code
- ✅ **Security**: Latest security patches
- ✅ **Modern Features**: Access to latest Java language features

## Troubleshooting

### "javac is not recognized"
- Verify PATH includes Java 21 bin directory
- Restart your terminal/IDE

### "Unsupported class file major version"
- You're trying to run Java 21 bytecode with Java 17
- Ensure Java 21 is first in your PATH

### Eclipse errors after upgrade
1. Project → Properties → Java Compiler → Set to 21
2. Project → Maven → Update Project
3. Restart Eclipse

## Support

For issues or questions:
- Check `JAVA_21_UPGRADE_GUIDE.md` for detailed information
- Java 21 Documentation: https://docs.oracle.com/en/java/javase/21/
- Project GitHub issues (if applicable)
