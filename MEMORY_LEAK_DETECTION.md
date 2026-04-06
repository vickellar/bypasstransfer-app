# Memory Leak Detection Guide

## Quick Memory Test (10 minutes)

### Step 1: Start with Monitoring

```bash
# Run your application
.\mvnw spring-boot:run

# In another terminal, check metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/jvm.gc.pause
```

### Step 2: VisualVM Setup (FREE)

1. **Download**: https://visualvm.github.io/download.html
2. **Run**: Open `visualvm.exe`
3. **Attach**: Find your running Java process
4. **Monitor**: Watch these tabs:
   - **Overview**: See threads and uptime
   - **Monitor**: CPU and memory graphs
   - **Threads**: Check for thread leaks
   - **Profiler**: Find memory hogs

### Step 3: Load Test While Monitoring

```bash
# Run performance test
.\run-performance-test.bat

# Watch VisualVM during test
# Look for:
# ✅ Memory goes up during load, then down after GC
# 🔴 Memory keeps climbing and never comes down (LEAK!)
```

---

## Common Memory Leaks in Spring Boot

### 1. Static Collections
```java
// ❌ BAD - Grows forever
private static List<User> cachedUsers = new ArrayList<>();

public void addUser(User user) {
    cachedUsers.add(user);  // Never cleared!
}

// ✅ GOOD - Use bounded cache
private Cache<String, User> userCache = 
    CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();
```

### 2. Unclosed Resources
```java
// ❌ BAD - Stream not closed
public void readFile() throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader("file.txt"));
    String line = reader.readLine();
    // Missing: reader.close()
}

// ✅ GOOD - Try-with-resources
public void readFile() throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader("file.txt"))) {
        String line = reader.readLine();
    }
}
```

### 3. Event Listeners
```java
// ❌ BAD - Listener never removed
@Component
public class MyListener {
    @Autowired
    private ApplicationEventPublisher publisher;
    
    @PostConstruct
    public void init() {
        // Adds listener but never removes it
        publisher.addApplicationListener(event -> {
            System.out.println("Event: " + event);
        });
    }
}
```

### 4. ThreadLocal Variables
```java
// ❌ BAD - ThreadLocal not cleaned
private static final ThreadLocal<SimpleDateFormat> dateFormat = 
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

// Each thread creates a new instance, never released
```

---

## Detecting Leaks with JConsole

### Built-in Java Tool:
```bash
# Start your app
.\mvnw spring-boot:run

# Get PID
jps -l | findstr bypasstransers

# Open JConsole
jconsole <PID>

# Monitor:
# - Heap Memory Usage tab
# - Threads tab
# - Classes tab
```

### What to Watch:
```
HEAP MEMORY:
├─ Used: Should fluctuate with GC
├─ Max: Fixed limit
└─ If used keeps growing → LEAK!

CLASSES LOADED:
├─ Should stay relatively stable
└─ If constantly increasing → ClassLoader leak!

THREADS:
├─ Active count should stabilize
└─ If keeps growing → Thread leak!
```

---

## Automated Memory Test

Create `memory-stress-test.bat`:

```batch
@echo off
echo Starting Memory Stress Test...
echo.

REM Run continuous requests for 5 minutes
set DURATION=300
set INTERVAL=1
set URL=http://localhost:8080/dashboard

echo Duration: %DURATION% seconds
echo Target: %URL%
echo.

for /L %%i in (1,1,%DURATION%) do (
    curl -s %URL% > nul
    echo [%%i] Request completed
    
    REM Check memory every 10 requests
    set /A remainder=%%i %% 10
    if !remainder! == 0 (
        curl -s http://localhost:8080/actuator/metrics/jvm.memory.used
    )
    
    timeout /t %INTERVAL% /nobreak > nul
)

echo.
echo Test Complete!
```

---

## Analyzing Heap Dumps

### Take Heap Dump:

**Method 1: VisualVM**
```
Right-click application → Heap Dump
Save the file
```

**Method 2: Command Line**
```bash
# Get PID
jps -l

# Take heap dump
jmap -dump:format=b,file=heap.hprof <PID>
```

**Method 3: On OutOfMemoryError**
```properties
# Add to application.properties
spring.jpa.properties.hibernate.generate_statistics=true

# JVM args
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=./heapdumps/
```

### Analyze with Eclipse MAT:

1. **Download**: http://www.eclipse.org/mat/
2. **Open**: heap.hprof file
3. **Run**: Leak Suspects Report
4. **Review**: Shows biggest objects and potential leaks

**What to Look For:**
- Large collections
- Cached objects never released
- Circular references
- Orphaned listeners

---

## GC Log Analysis

### Enable GC Logging:

Add to `src/main/resources/application.properties`:
```properties
# Development only! Don't use in production
logging.level.org.springframework.boot.actuate.metrics.export.prometheus=DEBUG
```

Or start JVM with:
```bash
java -Xloggc:gc.log \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+UseGCLogFileRotation \
     -XX:NumberOfGCLogFiles=5 \
     -XX:GCLogFileSize=10M \
     -jar target/bypasstransers.jar
```

### Analyze GC Logs:

**Online Tools:**
- https://gceasy.io/ (Free)
- Upload gc.log file
- Get detailed report

**What to Look For:**
```
GOOD GC Pattern:
├─ Young GC: Frequent, fast (<50ms)
├─ Old GC: Rare, slower
└─ Memory reclaimed each time

BAD GC Pattern (LEAK):
├─ Full GC: Very frequent
├─ Little memory reclaimed
└─ Pause times increasing
```

---

## Quick Health Checks

### Daily Monitoring Script:

```powershell
# monitor-memory.ps1
$pid = (Get-Process java | Where-Object {$_.CommandLine -like "*bypasstransers*"}).Id

Write-Host "=== Memory Check ===" -ForegroundColor Cyan
Write-Host "PID: $pid"

# Get heap usage
Invoke-RestMethod "http://localhost:8080/actuator/metrics/jvm.memory.used" | 
    ConvertTo-Json

# Get GC stats
Invoke-RestMethod "http://localhost:8080/actuator/metrics/jvm.gc.pause" | 
    ConvertTo-Json

# Alert if memory > 80%
$mem = Invoke-RestMethod "http://localhost:8080/actuator/metrics/jvm.memory.used"
if ($mem.measurements[0].value -gt 800000000) {
    Write-Host "WARNING: High memory usage!" -ForegroundColor Red
}
```

Run every hour:
```bash
# Task Scheduler or cron
* * * * * powershell -File monitor-memory.ps1
```

---

## Performance Baseline

### Create Baseline Metrics:

```bash
# Run this when system is healthy
curl http://localhost:8080/actuator/metrics/jvm.memory.used > baseline-memory.json
curl http://localhost:8080/actuator/metrics/jvm.gc.pause > baseline-gc.json
curl http://localhost:8080/actuator/metrics/http.server.requests > baseline-http.json
```

### Compare After Changes:

```bash
# Save current metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used > current-memory.json

# Compare
diff baseline-memory.json current-memory.json
```

---

## Red Flags Checklist

Check these weekly:

- [ ] Heap usage trending upward over days
- [ ] Full GC happening more than once per hour
- [ ] Response times slowly increasing
- [ ] Thread count growing
- [ ] Database connections not returned to pool
- [ ] OutOfMemoryError in logs
- [ ] Application needs regular restarts

If you see 2+ of these → Investigate immediately!

---

## Tools Summary

| Tool | Cost | Best For |
|------|------|----------|
| **VisualVM** | Free | Real-time monitoring |
| **JConsole** | Free | Quick checks |
| **Eclipse MAT** | Free | Heap dump analysis |
| **gceasy.io** | Free | GC log analysis |
| **JProfiler** | $$$$ | Deep profiling |
| **YourKit** | $$$ | Production profiling |
| **New Relic** | $$ | APM monitoring |
| **Datadog** | $$ | Cloud monitoring |

---

## Immediate Action Plan

### Week 1: Setup Monitoring
```bash
✅ Add Actuator dependency
✅ Configure endpoints
✅ Install VisualVM
✅ Take baseline measurements
```

### Week 2: Load Testing
```bash
✅ Run performance tests
✅ Monitor memory during load
✅ Document normal patterns
```

### Week 3: Stress Testing
```bash
✅ Extended runtime tests (1 hour+)
✅ Look for gradual memory growth
✅ Identify bottlenecks
```

### Week 4: Optimization
```bash
✅ Fix any leaks found
✅ Tune GC settings
✅ Optimize slow queries
```

---

**Start with VisualVM + Actuator, then expand based on what you find!** 🎯
