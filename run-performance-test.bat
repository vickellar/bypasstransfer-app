@echo off
REM ========================================
REM Quick Performance Test for Bypass Transers
REM ========================================

echo.
echo =============================================
echo  BYPASS TRANSERS - PERFORMANCE TEST
echo =============================================
echo.

REM Check if Apache Bench is available
where ab >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Apache Bench (ab) not found!
    echo Please install Apache HTTP Server or use alternative tools.
    echo Download from: https://www.apachelounge.com/download/
    echo.
    pause
    exit /b 1
)

echo [INFO] Apache Bench detected
echo.

REM Create results directory
if not exist "performance-results" mkdir performance-results

set BASE_URL=http://localhost:8080
set CONCURRENCY=5
set REQUESTS=50

echo Starting tests...
echo Base URL: %BASE_URL%
echo Concurrency: %CONCURRENCY%
echo Requests: %REQUESTS%
echo.

REM Test 1: Health Check
echo [TEST 1/6] Testing Actuator Health Endpoint...
call ab -n %REQUESTS% -c %CONCURRENCY% "%BASE_URL%/actuator/health" > "performance-results\01-health.txt"
echo Done! Results saved to performance-results\01-health.txt
echo.

REM Test 2: Dashboard
echo [TEST 2/6] Testing Dashboard Page...
call ab -n %REQUESTS% -c %CONCURRENCY% "%BASE_URL%/dashboard" > "performance-results\02-dashboard.txt"
echo Done! Results saved to performance-results\02-dashboard.txt
echo.

REM Test 3: Users List
echo [TEST 3/6] Testing Users Management...
call ab -n %REQUESTS% -c %CONCURRENCY% "%BASE_URL%/users" > "performance-results\03-users.txt"
echo Done! Results saved to performance-results\03-users.txt
echo.

REM Test 4: Admin Console
echo [TEST 4/6] Testing Admin Console...
call ab -n %REQUESTS% -c %CONCURRENCY% "%BASE_URL%/admin" > "performance-results\04-admin.txt"
echo Done! Results saved to performance-results\04-admin.txt
echo.

REM Test 5: Branch Management
echo [TEST 5/6] Testing Branch Management...
call ab -n %REQUESTS% -c %CONCURRENCY% "%BASE_URL%/admin/branches" > "performance-results\05-branches.txt"
echo Done! Results saved to performance-results\05-branches.txt
echo.

REM Test 6: Metrics
echo [TEST 6/6] Fetching Application Metrics...
call curl -s "%BASE_URL%/actuator/metrics/jvm.memory.used" > "performance-results\06-memory-metrics.json"
echo Done! Results saved to performance-results\06-memory-metrics.json
echo.

echo =============================================
echo  ALL TESTS COMPLETED!
echo =============================================
echo.
echo Results are in: performance-results\ folder
echo.
echo Next Steps:
echo 1. Open the text files to see response times
echo 2. Look for "Requests per second" metric
echo 3. Check "Time per request" values
echo 4. Review memory metrics in JSON file
echo.
echo Recommended Tools for Analysis:
echo - VisualVM: Monitor memory and CPU in real-time
echo - JProfiler: Deep dive into performance bottlenecks
echo - Chrome DevTools: Network tab for frontend timing
echo.

pause
