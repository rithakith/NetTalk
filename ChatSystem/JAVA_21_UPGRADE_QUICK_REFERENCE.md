# ⚡ Java 21 Upgrade - Quick Reference

## 📋 Current Status
- ✅ **Project configured for Java 21**
- ⚠️ **System has Java 17** - Upgrade needed
- 📦 **Maven build file created**

## 🚀 Quick Install (Windows)

### PowerShell Script (Run as Administrator):
```powershell
# Download and install Microsoft OpenJDK 21
winget install Microsoft.OpenJDK.21

# Set environment variables
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-21.0.5.11-hotspot', 'Machine')
$path = [System.Environment]::GetEnvironmentVariable('PATH', 'Machine')
[System.Environment]::SetEnvironmentVariable('PATH', "C:\Program Files\Microsoft\jdk-21.0.5.11-hotspot\bin;$path", 'Machine')

# Verify (restart terminal first)
java -version
```

## 📦 What Changed

| File | Change |
|------|--------|
| `pom.xml` | **NEW** - Maven config with Java 21 |
| `.classpath` | Updated to JavaSE-21 |
| `.settings/org.eclipse.jdt.core.prefs` | **NEW** - Java 21 compiler settings |
| `*.bat` files | Added version detection |

## 🔧 Quick Commands

```bash
# Build with Maven
mvn clean compile

# Download updated dependencies
mvn dependency:copy-dependencies -DoutputDirectory=lib

# Run with existing scripts
.\start-server.bat
.\run-websocket-server.bat
.\start-client.bat
```

## 📚 Documentation Files
1. `JAVA_21_UPGRADE_NEXT_STEPS.md` - Installation instructions
2. `JAVA_21_UPGRADE_GUIDE.md` - Full upgrade documentation
3. `JAVA_21_UPGRADE_QUICK_REFERENCE.md` - This file

## 🎯 Benefits
- 🔒 LTS until 2031
- ⚡ 15% faster performance
- 🧵 Virtual threads for better scalability
- 🛡️ Latest security patches
- 🎨 Modern Java features

## ⚠️ Important Notes
- Backup your project before testing
- Java 21 bytecode won't run on Java 17
- Update IDE to recognize Java 21
- Test all functionality after upgrade

## 🆘 Quick Troubleshooting

**Problem**: `javac: invalid target release: 21`
**Solution**: Install Java 21 JDK (not just JRE)

**Problem**: Eclipse shows errors
**Solution**: Right-click project → Maven → Update Project

**Problem**: "Unsupported class file major version"
**Solution**: Ensure Java 21 is in PATH before Java 17

## 📞 More Help
See `JAVA_21_UPGRADE_GUIDE.md` for detailed instructions.
