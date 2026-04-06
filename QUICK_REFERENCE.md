# 🚀 Quick Reference - Phone Testing Setup

## One-Line Setup
Your `.env` file is already configured! Just run:
```bash
docker-compose up
```

---

## Find Your Computer's IP (Windows)
```cmd
ipconfig
```
Look for: **IPv4 Address** (e.g., `192.168.1.100`)

---

## Access from Phone
Open browser on phone and go to:
```
http://YOUR_IP:8080
```
Example: `http://192.168.1.100:8080`

---

## Switch Profiles
```bash
# Development (phone testing)
$env:SPRING_PROFILES_ACTIVE=dev

# Production (deployment)
$env:SPRING_PROFILES_ACTIVE=prod
```

---

## Test Connection
- ✅ Computer: `http://localhost:8080`
- ✅ Phone: `http://YOUR_IP:8080`
- ✅ Login should work
- ✅ Navigation should work

---

## Troubleshooting
| Problem | Solution |
|---------|----------|
| 403 Error | Check `.env` has `SESSION_COOKIE_SECURE=false` |
| Won't connect | Verify same WiFi network |
| Firewall block | Allow port 8080 in Windows Firewall |
| Login fails | Clear phone browser cache |

---

## Files Changed
1. ✅ `application-dev.properties` (NEW)
2. ✅ `application.properties` (UPDATED)
3. ✅ `WebConfig.java` (UPDATED)
4. ✅ `SecurityConfig.java` (UPDATED)
5. ✅ `.env` (UPDATED)

---

## Before Deploying
Change `.env`:
```properties
SPRING_PROFILES_ACTIVE=prod
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=strict
```

---

**That's it!** 📱✅
