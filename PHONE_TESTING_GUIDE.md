# 📱 Testing Your App from Phone - Quick Guide

## ✅ Configuration Complete!

Your application is now configured to support **both**:
- 🔒 **Production** (HTTPS, secure cookies) - when deployed
- 📲 **Development** (HTTP, local network access) - for testing on your phone

---

## 🚀 How to Run for Phone Testing

### Option 1: Using Docker Compose (Recommended)
```bash
# Start the application with development profile
docker-compose up
```

The `.env` file already has `SPRING_PROFILES_ACTIVE=dev`, so it will automatically use the relaxed security settings.

### Option 2: Running Directly with Maven
```bash
# Make sure you're in the project directory
$env:SPRING_PROFILES_ACTIVE=dev
.\mvnw spring-boot:run
```

---

## 📡 Find Your Computer's IP Address

Before accessing from your phone, you need your computer's IP address:

### Windows:
```cmd
ipconfig
```
Look for **"IPv4 Address"** under your active network adapter (WiFi or Ethernet).
Example: `192.168.1.100`

### What you'll see:
```
Wireless LAN adapter Wi-Fi:
   IPv4 Address. . . . . . . . . . . : 192.168.1.100  ← THIS IS WHAT YOU NEED
   Subnet Mask . . . . . . . . . . . : 255.255.255.0
   Default Gateway . . . . . . . . . : 192.168.1.1
```

---

## 📱 Access from Your Phone

### Step 1: Connect to Same WiFi
Make sure your phone is connected to the **same WiFi network** as your computer.

### Step 2: Open Browser on Phone
On your phone's browser, go to:
```
http://YOUR_COMPUTER_IP:8080
```

For example:
```
http://192.168.1.100:8080
```

### Step 3: Login & Test
You should now be able to:
- ✅ See the login page
- ✅ Login successfully
- ✅ Navigate all pages without 403 errors

---

## 🔧 Troubleshooting

### Problem: Still getting 403 errors
**Solution:** Check your firewall settings

#### Windows Firewall:
1. Open **Windows Defender Firewall**
2. Click "Advanced settings"
3. Create new **Inbound Rule**:
   - Type: Port
   - Port: 8080 (TCP)
   - Action: Allow the connection
   - Profile: Check Domain, Private, Public
   - Name: "Spring Boot Dev"

### Problem: Can't connect at all
**Solutions:**
1. Verify both devices are on same WiFi
2. Check if app is running: Open `http://localhost:8080` on your computer
3. Try pinging your computer from phone (use network scanner app)
4. Temporarily disable firewall to test

### Problem: Page loads but login fails
**Solution:** Clear browser cache on phone and try again

---

## 🎯 What Changed?

### Files Modified:

1. **`application-dev.properties`** (NEW)
   - Development-specific settings
   - Relaxed security for local testing
   - Debug-friendly configurations

2. **`application.properties`** (UPDATED)
   - Now uses environment variables for security settings
   - Defaults to production-safe values

3. **`WebConfig.java`** (UPDATED)
   - Extended CORS to all endpoints (`/**`)
   - Allows local network requests

4. **`SecurityConfig.java`** (UPDATED)
   - Enabled CORS support
   - Works with WebConfig for cross-origin access

5. **`.env`** (UPDATED)
   - Activates `dev` profile automatically
   - Sets `SESSION_COOKIE_SECURE=false`
   - Configures allowed CORS origins

---

## 🔐 Security Comparison

| Setting | Development | Production |
|---------|-------------|------------|
| **Profile** | `dev` | `prod` |
| **Cookie Secure Flag** | `false` (allows HTTP) | `true` (HTTPS only) |
| **Same-Site** | `lax` (permits local) | `strict` (maximum security) |
| **CORS** | All local IPs | Restricted |
| **Use Case** | Local testing | Live deployment |

---

## ⚠️ Important Notes

### Before Deploying to Production:

1. **Change profile to prod:**
   ```bash
   $env:SPRING_PROFILES_ACTIVE=prod
   ```

2. **Or in Docker (.env file):**
   ```properties
   SPRING_PROFILES_ACTIVE=prod
   SESSION_COOKIE_SECURE=true
   SESSION_COOKIE_SAME_SITE=strict
   ```

3. **Ensure HTTPS is configured** on your production server

### Why This Matters:
- Development settings are **NOT safe for production**
- Production settings block local testing (by design)
- Spring profiles keep them separate automatically

---

## 🎓 Learning Points

### What is Spring Profiles?
Spring Profiles allow you to define **environment-specific configurations**. When you activate a profile (e.g., `dev`), Spring loads:
1. `application.properties` (base config)
2. `application-dev.properties` (overrides for dev)

### How It Works:
```
application.properties (always loaded)
         ↓
application-{PROFILE}.properties (loaded based on active profile)
         ↓
Environment variables (highest priority, override everything)
```

### Why Environment Variables?
They allow you to change behavior **without modifying code**:
```bash
# Override cookie security just for this run
$env:SESSION_COOKIE_SECURE=false
.\mvnw spring-boot:run
```

---

## 📚 Next Steps

1. ✅ Test from your phone
2. ✅ Try logging in
3. ✅ Navigate different pages
4. ✅ If it works, celebrate! 🎉
5. ❓ If not, check troubleshooting section above

---

## 💡 Tips

### Add Your IP to CORS List
If you want to make phone access easier, add your IP permanently:

Edit `.env`:
```properties
APP_CORS_ALLOWED_ORIGINS=http://localhost:8080,http://127.0.0.1:8080,http://192.168.1.100:8080
```

### Quick Profile Switch
Create a batch file for easy switching:

**run-dev.bat:**
```batch
@echo off
set SPRING_PROFILES_ACTIVE=dev
echo Starting in DEVELOPMENT mode...
mvnw spring-boot:run
```

**run-prod.bat:**
```batch
@echo off
set SPRING_PROFILES_ACTIVE=prod
echo Starting in PRODUCTION mode...
mvnw spring-boot:run
```

---

## ✅ Success Checklist

- [ ] Application compiles successfully
- [ ] `.env` has `SPRING_PROFILES_ACTIVE=dev`
- [ ] Found your computer's IP address
- [ ] Phone is on same WiFi as computer
- [ ] Application is running
- [ ] Can access `http://YOUR_IP:8080` from phone
- [ ] Can login without 403 error
- [ ] Can navigate pages freely

**If all checked → SUCCESS! 🎉**

---

**Questions?** Review the configuration files or ask for clarification!
