# Performance Testing Guide - Bypass Transers

## 1. Load Testing with Apache Bench (ab)

### Installation:
```bash
# Windows (install Apache HTTP Server)
# Download from: https://www.apachelounge.com/download/
# ab.exe is included in the bin folder

# Verify installation
ab -V
```

### Test Scenarios:

#### Scenario 1: Login Endpoint Load Test
```bash
# 100 requests, 10 concurrent users
ab -n 100 -c 10 -p login.json -T application/json http://localhost:8080/login

# login.json content:
{
  "username": "teststaff",
  "password": "password123"
}
```

#### Scenario 2: Dashboard Performance
```bash
# 500 requests, 50 concurrent users
ab -n 500 -c 50 http://localhost:8080/dashboard

# Results show:
# - Requests per second
# - Time per request
# - Connection times
```

#### Scenario 3: Transaction Creation Load
```bash
# Create transaction payload
cat > transaction.json << EOF
{
  "account": "Mukuru",
  "amount": 100,
  "type": "INCOME"
}
EOF

# 200 requests, 20 concurrent
ab -n 200 -c 20 -p transaction.json -T application/json \
   http://localhost:8080/receive
```

---

## 2. Memory Leak Detection

### Method 1: VisualVM (Free GUI Tool)

**Setup:**
```bash
# Download VisualVM
https://visualvm.github.io/download.html

# Run your app
.\mvnw spring-boot:run

# Attach VisualVM
1. Open visualvm.exe
2. Find your Java process
3. Click "Monitor" tab
4. Watch heap usage over time
```

**What to Look For:**
- 🔴 Heap keeps growing after load tests
- 🔴 Old gen memory doesn't decrease
- 🔴 GC runs frequently but doesn't free much

**Take Heap Dump:**
```
Right-click application → Heap Dump
Analyze with Eclipse MAT:
http://www.eclipse.org/mat/
```

### Method 2: JVM Built-in Tools

**Enable GC Logging:**
```bash
# Add to application.properties or startup
java -jar target/bypasstransers.jar \
  -Xloggc:gc.log \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -XX:+UseGCLogFileRotation
```

**Monitor with jstat:**
```bash
# Get PID
jps -l | findstr bypasstransers

# Monitor GC every 1 second
jstat -gcutil <PID> 1000

# Output shows:
# S0/S1 - Survivor spaces
# E - Eden space
# O - Old generation
# M - Metaspace
# YGC/YGCT - Young GC count/time
# FGC/FGCT - Full GC count/time
```

**Memory Profiler:**
```bash
# Take heap histogram
jmap -histo:live <PID> | head -50

# Shows top 50 objects in memory
```

---

## 3. Spring Boot Actuator (Built-in Monitoring)

### Add Dependencies (pom.xml):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Configure Endpoints:
```properties
# application.properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=always
management.metrics.tags.application=${spring.application.name}
```

### Access Metrics:
```bash
# Health check
curl http://localhost:8080/actuator/health

# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP request metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# Prometheus format (for Grafana)
curl http://localhost:8080/actuator/prometheus
```

---

## 4. Gatling Load Testing (Advanced)

### Setup:
```bash
# Download Gatling
https://gatling.io/open-source/

# Or use Maven plugin
```

### Create Simulation:
```scala
// src/test/java/simulations/UserTransactionSimulation.scala
package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class UserTransactionSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Scenario: Users performing transactions
  val scn = scenario("Staff Member Workflow")
    .exec(http("Login")
      .post("/login")
      .body(StringBody("""{"username":"staff1","password":"pass123"}""")).asJson)
    .pause(1)
    .exec(http("Get Dashboard")
      .get("/dashboard"))
    .pause(1)
    .exec(http("Receive Money")
      .post("/receive")
      .body(StringBody("""{"account":"Mukuru","amount":100}""")).asJson)
    .pause(2)
    .exec(http("Send Money")
      .post("/send")
      .body(StringBody("""{"account":"Econet","amount":50}""")).asJson)

  // Load pattern: Ramp up users over 1 minute
  setUp(
    scn.inject(
      rampUsersPerSec(1).to(10).during(1.minutes),
      constantUsersPerSec(10).during(2.minutes),
      rampUsersPerSec(10).to(1).during(1.minutes)
    ).protocols(httpProtocol))
}
```

### Run Test:
```bash
cd gatling-bin
./bin/gatling.sh -s UserTransactionSimulation
```

### Results:
- Response time percentiles
- Request success rate
- Concurrent user handling
- Bottleneck identification

---

## 5. JProfiler / YourKit (Commercial Profilers)

### JProfiler Setup:
```bash
# Download: https://www.ej-technologies.com/products/jprofiler.html

# Start with agent
java -agentpath:"C:\Program Files\jprofiler\bin\windows-x64\jprofilerti.dll" \
     -jar target/bypasstransers.jar
```

**Features:**
- CPU profiling (find slow methods)
- Memory profiling (find leaks)
- SQL profiling (slow queries)
- Thread analysis (deadlocks)

---

## 6. Custom Performance Tests

### Create Test Controller:
```java
@RestController
@RequestMapping("/test")
public class PerformanceTestController {

    @Autowired
    private WalletTransactionService walletTransactionService;

    @GetMapping("/benchmark-receive")
    public Map<String, Object> benchmarkReceive() {
        long startTime = System.currentTimeMillis();
        
        // Perform 100 receive operations
        for (int i = 0; i < 100; i++) {
            try {
                walletTransactionService.receive("Mukuru", 100);
            } catch (Exception e) {
                // Ignore errors for benchmark
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> result = new HashMap<>();
        result.put("operations", 100);
        result.put("totalTimeMs", endTime - startTime);
        result.put("avgTimePerOp", (endTime - startTime) / 100.0);
        result.put("opsPerSecond", 100000.0 / (endTime - startTime));
        
        return result;
    }

    @GetMapping("/memory-info")
    public Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> memory = new HashMap<>();
        memory.put("totalMemory", runtime.totalMemory());
        memory.put("freeMemory", runtime.freeMemory());
        memory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        memory.put("maxMemory", runtime.maxMemory());
        
        return memory;
    }
}
```

### Access Benchmark:
```bash
curl http://localhost:8080/test/benchmark-receive
curl http://localhost:8080/test/memory-info
```

---

## 7. Database Performance

### Enable Slow Query Log:
```sql
-- PostgreSQL
ALTER SYSTEM SET log_min_duration_statement = 1000; -- Log queries > 1s
SELECT pg_reload_conf();

-- Check slow queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;
```

### Connection Pool Monitoring:
```properties
# HikariCP settings
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000

# Enable metrics
spring.datasource.hikari.register-mbeans=true
```

---

## 8. Recommended Testing Strategy

### Phase 1: Baseline Performance
```bash
# Single user response times
ab -n 10 -c 1 http://localhost:8080/dashboard
ab -n 10 -c 1 http://localhost:8080/users
ab -n 10 -c 1 http://localhost:8080/admin/branches

# Record baseline times
```

### Phase 2: Load Test
```bash
# Gradually increase load
ab -n 100 -c 5 http://localhost:8080/dashboard
ab -n 500 -c 20 http://localhost:8080/dashboard
ab -n 1000 -c 50 http://localhost:8080/dashboard

# Find breaking point
```

### Phase 3: Endurance Test
```bash
# Run moderate load for 1 hour
ab -n 10000 -c 10 http://localhost:8080/dashboard

# Monitor memory growth
```

### Phase 4: Spike Test
```bash
# Sudden traffic spike
ab -n 1000 -c 100 http://localhost:8080/dashboard
```

---

## 9. Quick Start Script

Create `performance-test.bat`:
```batch
@echo off
echo Starting Performance Tests...
echo.

echo [1/4] Testing Dashboard...
ab -n 100 -c 10 http://localhost:8080/dashboard > results\dash.txt

echo [2/4] Testing User Management...
ab -n 50 -c 5 http://localhost:8080/users > results\users.txt

echo [3/4] Testing Transactions...
ab -n 100 -c 10 -p transaction.json -T application/json ^
   http://localhost:8080/receive > results\transactions.txt

echo [4/4] Checking Memory...
curl http://localhost:8080/actuator/metrics/jvm.memory.used > results\memory.txt

echo.
echo Tests Complete! Check results\ folder
```

---

## 10. What to Monitor

### Key Metrics:

| Metric | Good | Warning | Critical |
|--------|------|---------|----------|
| Response Time | <200ms | 200-1000ms | >1000ms |
| Error Rate | <0.1% | 0.1-1% | >1% |
| CPU Usage | <70% | 70-90% | >90% |
| Memory Usage | <70% | 70-85% | >85% |
| DB Connections | <50% pool | 50-80% | >80% |
| GC Pause | <50ms | 50-200ms | >200ms |

### Red Flags:
- 🔴 Response time increases over time
- 🔴 Memory keeps growing (leak)
- 🔴 GC runs every few seconds
- 🔴 Database connection pool exhausted
- 🔴 Thread count keeps growing
- 🔴 Disk I/O spikes

---

## 11. Immediate Actions

### Start Simple:
```bash
# 1. Install Apache Bench
# 2. Run basic test
ab -n 50 -c 5 http://localhost:8080/dashboard

# 3. Check VisualVM for memory
# 4. Monitor console for errors
```

### Then Advanced:
```bash
# 1. Add Actuator
# 2. Set up Gatling
# 3. Configure APM tool (New Relic/DataDog)
# 4. Load test staging environment
```

---

## Example Test Report Template

```
Performance Test Report - Bypass Transers
==========================================
Date: [DATE]
Version: 0.0.1-SNAPSHOT

Load Test Results:
------------------
Endpoint: /dashboard
Requests: 1000
Concurrent Users: 50
Duration: 2 minutes

Results:
- Requests/sec: 150
- Avg Response: 333ms
- Min Response: 45ms
- Max Response: 1200ms
- 90th Percentile: 600ms
- Success Rate: 99.8%

Memory Usage:
-------------
Start: 256MB
Peak: 512MB
End: 280MB
GC Runs: 15

Issues Found:
-------------
[ ] Slow database queries on /users endpoint
[ ] Memory grows by 20MB per 100 requests
[ ] Connection pool reaches 80% at peak load

Recommendations:
----------------
1. Add index on users.email column
2. Investigate transaction object retention
3. Increase HikariCP pool size to 20
```

---

**Choose one method to start with (recommend Apache Bench + VisualVM), then expand from there!**

Would you like me to:
1. Create specific load test scripts for your endpoints?
2. Set up Spring Boot Actuator monitoring?
3. Create a Gatling simulation for your workflows?
4. Build automated performance regression tests?
