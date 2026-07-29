# 🚀 Performance Optimization Guide

## ⚡ Quick Start Options

### **Option 1: Run Locally (FASTEST)** ⭐ RECOMMENDED

**Double-click:** `run-local.bat`

**Or manually:**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/bypass_records"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="NjisweVic~2030"
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw spring-boot:run
```

**Speed:** 🚀🚀🚀🚀🚀 (Very Fast)
- No Docker overhead
- Direct file access
- Faster startup (~15-20 seconds)
- Hot reload with DevTools

---

### **Option 2: Run with Docker (Optimized)**

**Double-click:** `run-docker.bat`

**Or manually:**
```bash
docker-compose up --build
```

**Speed:** 🚀🚀🚀 (Moderate)
- Container overhead
- Isolated environment
- Slower startup (~30-45 seconds)
- Better for production testing

---

## 🔧 What Was Optimized

### **1. JVM Settings**
```properties
-Xmx512m          # Max heap size (512 MB)
-Xms256m          # Initial heap size (256 MB)
-XX:+UseG1GC      # Modern garbage collector (faster)
-XX:+UseStringDeduplication  # Reduce memory usage
```

### **2. Spring Boot Settings**
```properties
SPRING_JPA_OPEN_IN_VIEW=false  # Better database performance
SPRING_THYMELEAF_CACHE=false   # Faster template reloading (dev only)
SPRING_PROFILES_ACTIVE=dev     # Development optimizations
```

### **3. Spring DevTools**
- ✅ Automatic restart on code changes
- ✅ Live reload for templates
- ✅ Faster development cycle
- ✅ No manual restart needed

### **4. Docker Optimizations**
- ✅ CPU limit: 2 cores
- ✅ Memory limit: 1 GB
- ✅ Volume mount for logs
- ✅ Optimized JVM for containers

---

## 📊 Performance Comparison

| Method 			| Startup Time | Memory Usage | Best For |
|-------------------|--------------|--------------|----------|
| **Local (Maven)** | 15-20 sec	   |   ~400 MB    |Development |
| **Docker**        | 30-45 sec    | ~800 MB      | Testing/Production |

---

## 🎯 Windows Docker Performance Tips

### **If Docker is Slow on Windows:**

#### **1. Use WSL 2 Backend**
1. Open Docker Desktop
2. Go to **Settings** → **General**
3. ✅ Enable "Use WSL 2 based engine"
4. Apply & Restart

#### **2. Increase Docker Resources**
1. Open Docker Desktop
2. Go to **Settings** → **Resources**
3. Set:
   - **CPUs:** 4 or more
   - **Memory:** 4 GB or more
   - **Swap:** 2 GB

#### **3. Use Volume Mounts Carefully**
- Avoid mounting too many directories
- Use named volumes for databases
- Keep source code mounts minimal

#### **4. Exclude Project from Antivirus**
Add project folder to Windows Defender exclusions:
```
Settings → Virus & threat protection → Exclusions
Add: C:\Users\Vickeller.01\Desktop\bypasstransers (1)\bypasstransers
```

---

## 🔥 Development Speed Tips

### **Hot Reload (With DevTools)**

When running locally or with Docker:
1. Make code changes
2. Save file (Ctrl+S)
3. App automatically restarts (~3-5 seconds)
4. Refresh browser

**No need to restart manually!**

### **Skip Tests for Faster Builds**
```bash
.\mvnw clean package -DskipTests
```

### **Use Dev Profile**
```properties
SPRING_PROFILES_ACTIVE=dev
```
- Disables template caching
- Shows SQL queries
- Faster iteration

---

## 🐛 Troubleshooting Slow Performance

### **Problem: Docker startup > 2 minutes**

**Solutions:**
1. ✅ Use `run-local.bat` instead (much faster)
2. ✅ Increase Docker memory to 4GB+
3. ✅ Enable WSL 2 backend
4. ✅ Close other Docker containers

### **Problem: App feels sluggish**

**Solutions:**
1. ✅ Increase JVM heap: `-Xmx1024m`
2. ✅ Check database connection pool settings
3. ✅ Enable database query logging to find slow queries
4. ✅ Use `dev` profile for development

### **Problem: High CPU usage**

**Solutions:**
1. ✅ Limit Docker CPU: `cpus: '2.0'`
2. ✅ Close other applications
3. ✅ Check for infinite loops in code
4. ✅ Reduce connection pool size

---

## 📈 Monitoring Performance

### **Check App Performance:**
```
http://localhost:8080/actuator/metrics
```

### **Check Docker Stats:**
```bash
docker stats
```

### **View Application Logs:**
```bash
# Docker
docker-compose logs -f app

# Local
# Logs appear in terminal
```

---

## 🎓 Best Practices

### **For Development:**
- ✅ Use `run-local.bat` (Maven direct)
- ✅ Enable DevTools (already added)
- ✅ Use `dev` profile
- ✅ Disable template caching

### **For Testing:**
- ✅ Use `run-docker.bat`
- ✅ Test with production-like settings
- ✅ Use `prod` profile
- ✅ Enable template caching

### **For Production (Render):**
- ✅ Use optimized JVM settings
- ✅ Enable template caching
- ✅ Use `prod` profile
- ✅ Monitor with actuator endpoints

---

## 🚀 Quick Commands

### **Start Local (Fast):**
```bash
.\run-local.bat
```

### **Start Docker:**
```bash
.\run-docker.bat
```

### **Stop Docker:**
```bash
docker-compose down
```

### **View Logs:**
```bash
docker-compose logs -f
```

### **Clean and Rebuild:**
```bash
.\mvnw clean package -DskipTests
```

---

## 💡 Recommendation

**For daily development:**
```
Use: run-local.bat
Speed: ⚡⚡⚡⚡⚡
```

**For testing Docker deployment:**
```
Use: run-docker.bat
Speed: ⚡⚡⚡
```

**For production:**
```
Deploy to Render with optimized settings
Speed: ⚡⚡⚡⚡
```

---

## ✅ Performance Checklist

- [x] JVM optimized (-Xmx512m, G1GC)
- [x] DevTools added for hot reload
- [x] Dev profile configured
- [x] Docker resources limited
- [x] Template caching disabled (dev)
- [x] Open-in-view disabled
- [x] Startup scripts created

**Your app should now be significantly faster!** 🚀

---

## 🆘 Still Slow?

Try these:
1. Restart Docker Desktop
2. Close unused applications
3. Use `run-local.bat` instead of Docker
4. Check antivirus scanning
5. Increase Docker resources

**Let me know if you need more help!** 💪
