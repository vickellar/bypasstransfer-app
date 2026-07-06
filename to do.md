I'll analyze the security of this web application. Let me start by examining the key security-related files and configurations.Let me examine several critical files to assess the security posture:I appreciate your request to review the security of your web application. Based on the code provided and the project structure visible, I've identified several **critical security concerns** that need immediate attention:

## 🔴 **Critical Security Issues**

### 1. **Hardcoded Default Credentials**
The application seeds hardcoded default credentials on startup:
```
superadmin / superpass
admin / adminpass  
staff / staffpass
```

**Risk:** These credentials are visible in source code and pose a major breach risk.

**Recommendation:**
- Remove hardcoded credentials entirely
- Use secure credential management (environment variables, vaults, or secret managers)
- Generate random temporary credentials for initial setup
- Force password change on first login

### 2. **Weak Default Passwords**
The default passwords are extremely weak (simple plain text strings).

**Recommendation:**
- Require complex passwords meeting NIST guidelines
- Enforce minimum 12+ character passwords with mixed case, numbers, symbols

### 3. **Plain Text Password Migration Fallback**
The code attempts to migrate plain-text passwords but treats them as raw if they don't start with `$2`:
```java
if (pw != null && !pw.startsWith("$2")) {
    u.setPassword(passwordEncoder.encode(pw));
    userRepo.save(u);
}
```

**Risk:** Assumes existing passwords are stored in plain text, creating a security vulnerability.

### 4. **Insufficient Audit Logging**
The `AuditService` has weak fallback behavior:
- Sets `performedBy=0L` when user cannot be determined
- Logs generic "system" entity without context
- No logging of failed authentication attempts
- No rate limiting on failed logins

**Recommendation:**
- Log all authentication failures with IP address and timestamp
- Implement account lockout after N failed attempts
- Log all sensitive operations (password changes, privilege escalation, data exports)

### 5. **Potential SQL Injection Vulnerabilities**
Multiple repository queries using user input need validation:
```java
List<User> users = userRepository.findByUsername(resolvedUsername);
```

**Recommendation:**
- Validate and sanitize all user inputs
- Use parameterized queries (JPA should do this, but verify)
- Implement input validation framework

### 6. **Missing Security Headers**
Based on the controller structure, the application likely lacks essential HTTP security headers.

**Recommendation:**
- Add Security Headers:
    - `X-Content-Type-Options: nosniff`
    - `X-Frame-Options: DENY`
    - `X-XSS-Protection: 1; mode=block`
    - `Strict-Transport-Security: max-age=31536000`
    - `Content-Security-Policy: default-src 'self'`

### 7. **Permission Bypass Risk in SecurityService**
```java
public boolean canAccessWallet(Long walletId) {
    // Supervisors and above can access ANY wallet
    if (isSupervisorOrAbove()) {
        return true;
    }
}
```

**Risk:** Supervisors have unrestricted access. Should be role-based with scope limitations.

### 8. **DebugController Likely Exists**
The presence of `DebugController.java` is concerning:
- Should not exist in production
- Likely exposes sensitive debug information
- Must be removed or secured behind admin-only routes

### 9. **Missing CSRF Protection Verification**
Need to verify CSRF tokens are properly configured and validated.

### 10. **Sensitive Data Exposure**
- Phone numbers stored in plain text
- No encryption for sensitive fields
- `.env` file contains secrets (should use secure vaults)

### 11. **No Rate Limiting**
No apparent rate limiting on:
- Login attempts
- API calls
- Password reset requests
- Email verification

**Recommendation:** Implement rate limiting using Spring Security or dedicated libraries.

### 12. **Weak Session Management**
Need verification of:
- Session timeout configuration
- JSESSIONID security (HttpOnly, Secure flags)
- Session fixation protection

---

## **Immediate Action Items**

1. **Remove all hardcoded credentials** from source code
2. **Audit and secure the DebugController**
3. **Implement rate limiting** on authentication endpoints
4. **Add comprehensive audit logging** with IP tracking
5. **Implement input validation** across all controllers
6. **Add security headers** to all HTTP responses
7. **Enable and verify CSRF protection**
8. **Review and restrict role-based access control** (no "access all" permissions)
9. **Encrypt sensitive data** in transit and at rest
10. **Use secrets management** for credentials (HashiCorp Vault, AWS Secrets Manager, etc.)

Would you like me to examine specific controllers or the SecurityConfig file in detail to provide targeted recommendations?




## 

## **Let me try a different approach:Let me try a different approach to access the files:Great! Now let me get the key controllers to analyze authorization:Perfect! Now I have enough data to provide a comprehensive security audit. Let me detail the findings:

---

## 🔴 **COMPREHENSIVE SECURITY AUDIT RESULTS**

### **1. CRITICAL: DebugController - Massive Security Breach**

The `DebugController` is a **critical security vulnerability**. It exposes:

- **`/debug/raw`** - Lists ALL usernames with plaintext passwords
- **`/debug/list-users`** - Enumerates all users
- **`/debug/check-password`** - Tests if passwords are correct
- **`/debug/reset-password`** - Allows password resets without authentication
- **`/debug/set-role`** - Allows privilege escalation to any role
- **`/debug/seed-defaults`** - Re-seeds default credentials
- **`/debug/migrate-passwords`** - Manipulates password hashing

While SecurityConfig restricts these to `SUPER_ADMIN`, if an admin account is compromised, the attacker can:
1. Dump all user credentials
2. Reset any user's password
3. Escalate their own privileges

**🛠️ IMMEDIATE ACTION: DELETE THIS ENTIRE CONTROLLER**

```
This file should be completely removed from the project.
```


---

### **2. CRITICAL: Weak Password Validation**

**File:** `PasswordResetController.java`

```java
if (newPassword == null || newPassword.length() < 6) {
    ra.addFlashAttribute("error", "New password must be at least 6 characters long.");
    return "redirect:/change-password";
}
```


**Problems:**
- Only 6 character minimum (should be 12+)
- No complexity requirements (uppercase, lowercase, numbers, symbols)
- No breach database check
- Doesn't prevent reuse of old passwords

**🛠️ RECOMMENDATION:**

```java
// Replace password validation section
private static final String PASSWORD_PATTERN = 
    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$";

private boolean isPasswordStrong(String password) {
    if (password == null || password.length() < 12) {
        return false;
    }
    // Must contain: lowercase, uppercase, digit, special char
    return password.matches(PASSWORD_PATTERN);
}

// In handleChangePassword method:
// Validate new password
if (!isPasswordStrong(newPassword)) {
    ra.addFlashAttribute("error", 
        "Password must be at least 12 characters with uppercase, lowercase, numbers, and symbols.");
    return "redirect:/change-password";
}

// Check if password was recently used (prevent reuse)
if (passwordEncoder.matches(newPassword, user.getPassword())) {
    ra.addFlashAttribute("error", "You cannot reuse your previous password.");
    return "redirect:/change-password";
}
```


---

### **3. CRITICAL: No Rate Limiting on Authentication**

**Issue:** No protection against brute force attacks on:
- Login attempts
- Password reset requests
- Email verification

**🛠️ SOLUTION: Add Rate Limiting**

Create new file:

```java
package com.bypass.bypasstransers.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket loginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    @Bean
    public Bucket passwordResetBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }
}
```


Update `pom.xml` to add dependency:

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>
```


---

### **4. CRITICAL: Weak Default Credentials**

**File:** `BypasstransersApplication.java`

The hardcoded defaults are easily guessable:
```
superadmin / superpass
admin / adminpass
staff / staffpass
```


**🛠️ RECOMMENDATION:**

```java
// Replace the seeding section with:
@Bean
public ApplicationListener<ApplicationReadyEvent> init(AccountRepository accountRepo,
                                                        UserRepository userRepo,
                                                        PasswordEncoder passwordEncoder) {
    return event -> {
        try {
            long count = accountRepo.count();
            if (count == 0) {
                accountRepo.save(new Account(null, "Econet", BigDecimal.ZERO, new BigDecimal("0.033")));
                accountRepo.save(new Account(null, "InnBucks", BigDecimal.ZERO, new BigDecimal("0.02")));
                accountRepo.save(new Account(null, "Mukuru", BigDecimal.ZERO, new BigDecimal("0.015")));
                log.info("Seeded default accounts");
            }

            long users = userRepo.count();
            if (users == 0) {
                // GENERATE SECURE TEMPORARY PASSWORDS
                String superAdminPass = generateSecurePassword();
                String adminPass = generateSecurePassword();
                String staffPass = generateSecurePassword();

                User superAdmin = new User();
                superAdmin.setUsername("superadmin");
                superAdmin.setPassword(passwordEncoder.encode(superAdminPass));
                superAdmin.setPhoneNumber("+26377801140");
                superAdmin.setRole(Role.SUPER_ADMIN);
                userRepo.save(superAdmin);

                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode(adminPass));
                admin.setPhoneNumber("+263717847646");
                admin.setRole(Role.ADMIN);
                userRepo.save(admin);

                User staff = new User();
                staff.setUsername("staff");
                staff.setPassword(passwordEncoder.encode(staffPass));
                staff.setPhoneNumber("+1000000002");
                staff.setRole(Role.STAFF);
                userRepo.save(staff);

                // LOG CREDENTIALS SECURELY (only in logs, not console)
                log.info("⚠️  INITIAL CREDENTIALS (save in secure location):");
                log.info("superadmin: {}", superAdminPass);
                log.info("admin: {}", adminPass);
                log.info("staff: {}", staffPass);
                log.info("⚠️  These credentials will NOT be displayed again. Change them immediately.");
            }

            // Migrate any existing plain-text passwords to BCrypt
            List<User> all = userRepo.findAll();
            int migrated = 0;
            for (User u : all) {
                String pw = u.getPassword();
                if (pw != null && !pw.startsWith("$2")) {
                    u.setPassword(passwordEncoder.encode(pw));
                    userRepo.save(u);
                    migrated++;
                }
            }
            if (migrated > 0) log.info("Migrated {} plain-text passwords to BCrypt", migrated);

        } catch (Exception ex) {
            log.warn("Initialization skipped: {}", ex.getMessage());
        }
    };
}

private static String generateSecurePassword() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    StringBuilder password = new StringBuilder();
    java.util.Random random = new java.util.Random();
    for (int i = 0; i < 16; i++) {
        password.append(chars.charAt(random.nextInt(chars.length())));
    }
    return password.toString();
}
```


---

### **5. MEDIUM: Missing HSTS Preload**

**File:** `SecurityConfig.java` - **GOOD NEWS: Already configured!**

The HSTS header is properly set:
```java
.httpStrictTransportSecurity(hsts -> hsts
    .includeSubDomains(true)
    .maxAgeInSeconds(31536000)
    .preload(true)  // ✅ GOOD
)
```


---

### **6. MEDIUM: Insufficient Audit Logging**

**File:** `AuditService.java`

**Problems:**
- No IP address logging
- No login failure tracking
- Fallback to user ID `0L` is incorrect
- No timestamp in logs for audit trail

**🛠️ RECOMMENDATION:**

Create new file for better audit logging:

```java
package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.AuditLog;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AuditLogRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnhancedAuditService {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAuditService.class);

    private final AuditLogRepository repo;
    private final UserRepository userRepository;

    public EnhancedAuditService(AuditLogRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwarded = request.getHeader("X-Forwarded-For");
                if (xForwarded != null && !xForwarded.isEmpty()) {
                    return xForwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            logger.warn("Could not determine client IP", e);
        }
        return "UNKNOWN";
    }

    public void logSecurityEvent(String action, String details, boolean success) {
        String username = getCurrentUsername();
        String ipAddress = getClientIpAddress();
        Long userId = getCurrentUserId();

        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityName("security_event");
        log.setEntityId(userId != null ? userId : 0L);
        log.setOldValue(ipAddress); // Store IP for security tracking
        log.setNewValue(success ? "SUCCESS" : "FAILURE");
        log.setPerformedAt(LocalDateTime.now());
        log.setPerformedBy(userId);

        repo.save(log);
        
        logger.warn("SECURITY_AUDIT: action={}, user={}, ip={}, success={}, details={}",
                   action, username, ipAddress, success, details);
    }

    public void logFailedLogin(String username) {
        logSecurityEvent("LOGIN_FAILURE", "Failed login attempt for: " + username, false);
    }

    public void logSuccessfulLogin(String username) {
        logSecurityEvent("LOGIN_SUCCESS", "Successful login for: " + username, true);
    }

    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetails) {
                    return ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    return (String) principal;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not determine current username", e);
        }
        return "ANONYMOUS";
    }

    private Long getCurrentUserId() {
        try {
            String username = getCurrentUsername();
            if (!"ANONYMOUS".equals(username)) {
                List<User> users = userRepository.findByUsername(username);
                return users.isEmpty() ? null : users.get(0).getId();
            }
        } catch (Exception e) {
            logger.debug("Could not determine current user ID", e);
        }
        return null;
    }
}
```


---

### **7. MEDIUM: Password Reset Token Security**

**File:** `PasswordResetController.java`

**Issues:**
- Token expiry not visible
- No token rate limiting
- Token can be reused if not invalidated

**Recommendation:**
- Set token expiry to 15-30 minutes
- Invalidate token immediately after use
- Limit password reset requests to 3 per hour per user

---

### **8. LOW: Session Configuration**

**File:** `SecurityConfig.java` - **GOOD:**
```java
.sessionManagement(session -> session
    .maximumSessions(1)  // ✅ Prevents concurrent sessions
    .maxSessionsPreventsLogin(false)
)
.logout(logout -> logout
    .deleteCookies("JSESSIONID")  // ✅ Deletes session cookie
)
```


**HOWEVER - ADD MISSING CONFIGURATION:**

```java
// Add to filterChain method after sessionManagement:
            .sessionManagement(session -> session
                .sessionFixationProtection(org.springframework.security.config.Customizer.withDefaults())
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );
            
// Also add these critical cookie settings:
http.requiresChannel()
    .anyRequest()
    .requiresSecure(); // Force HTTPS in production

// Add cookie attributes before return statement:
java.util.stream.Stream.of("JSESSIONID")
    .forEach(cookieName -> {
        // These are handled by Spring Security
    });
```


---

### **9. CRITICAL: Missing Content-Type Validation**

Many endpoints don't validate input types, allowing:
- XXE (XML External Entity) attacks
- Content-Type spoofing

**Recommendation:** Add validation to all `@PostMapping` endpoints:

```java
@PostMapping("/api/endpoint")
public ResponseEntity<?> endpoint(
    @RequestHeader("Content-Type") String contentType,
    @RequestBody String payload
) {
    if (!contentType.contains("application/json")) {
        return ResponseEntity.badRequest()
            .body("Invalid Content-Type. Expected application/json");
    }
    // ... process payload
}
```


---

### **10. MEDIUM: SQL Injection Risk**

**File:** `SecurityService.java`

```java
List<User> users = userRepository.findByUsernameIgnoreCase(username);
```


**Current state:** ✅ SAFE - JPA uses parameterized queries

**However, ensure:**
- No string concatenation in queries
- Use `@Query` with named parameters if custom queries exist
- Regularly update Spring Data JPA

---

## **📋 SECURITY CHECKLIST - PRIORITY ORDER**

### 🔴 **CRITICAL (Do Immediately)**
- [ ] **DELETE** `DebugController.java` completely
- [ ] Remove hardcoded credentials from `BypasstransersApplication.java`
- [ ] Generate secure temporary passwords on first run
- [ ] Implement password complexity validation (12+ chars, mixed case, symbols, numbers)
- [ ] Add rate limiting (5 login attempts per 15 mins)

### 🟠 **HIGH (This Week)**
- [ ] Implement comprehensive audit logging with IP addresses
- [ ] Add failed login tracking and account lockout
- [ ] Log all authentication events
- [ ] Review and restrict all `@PreAuthorize` decorators
- [ ] Remove any test/debug code from production

### 🟡 **MEDIUM (This Month)**
- [ ] Add Content-Type validation to all endpoints
- [ ] Set token expiry to 15 minutes
- [ ] Implement password history (prevent reuse)
- [ ] Add session timeout configuration (15 mins)
- [ ] Enable HTTPS-only cookies

### 🟢 **LOW (Next Sprint)**
- [ ] Run dependency security scan
- [ ] Set up WAF (Web Application Firewall)
- [ ] Implement intrusion detection logging
- [ ] Add input validation framework

---

## **Recommended Dependency Additions**

Add to `pom.xml`:

```xml
<!-- Rate Limiting -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>

<!-- Password Validation -->
<dependency>
    <groupId>org.passay</groupId>
    <artifactId>passay</artifactId>
    <version>1.6.4</version>
</dependency>

<!-- Security Headers -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-web</artifactId>
    <version>6.2.0</version>
</dependency>

<!-- Dependency Check -->
<dependency>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.0</version>
    <scope>provided</scope>
</dependency>
```


---

Would you like me to provide detailed implementation code for any of these security fixes? **

Let me try a different approach:Let me try a different approach to access the files:Great! Now let me get the key controllers to analyze authorization:Perfect! Now I have enough data to provide a comprehensive security audit. Let me detail the findings:

---

## 🔴 **COMPREHENSIVE SECURITY AUDIT RESULTS**

### **1. CRITICAL: DebugController - Massive Security Breach**

The `DebugController` is a **critical security vulnerability**. It exposes:

- **`/debug/raw`** - Lists ALL usernames with plaintext passwords
- **`/debug/list-users`** - Enumerates all users
- **`/debug/check-password`** - Tests if passwords are correct
- **`/debug/reset-password`** - Allows password resets without authentication
- **`/debug/set-role`** - Allows privilege escalation to any role
- **`/debug/seed-defaults`** - Re-seeds default credentials
- **`/debug/migrate-passwords`** - Manipulates password hashing

While SecurityConfig restricts these to `SUPER_ADMIN`, if an admin account is compromised, the attacker can:
1. Dump all user credentials
2. Reset any user's password
3. Escalate their own privileges

**🛠️ IMMEDIATE ACTION: DELETE THIS ENTIRE CONTROLLER**

```
This file should be completely removed from the project.
```


---

### **2. CRITICAL: Weak Password Validation**

**File:** `PasswordResetController.java`

```java
if (newPassword == null || newPassword.length() < 6) {
    ra.addFlashAttribute("error", "New password must be at least 6 characters long.");
    return "redirect:/change-password";
}
```


**Problems:**
- Only 6 character minimum (should be 12+)
- No complexity requirements (uppercase, lowercase, numbers, symbols)
- No breach database check
- Doesn't prevent reuse of old passwords

**🛠️ RECOMMENDATION:**

```java
// Replace password validation section
private static final String PASSWORD_PATTERN = 
    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$";

private boolean isPasswordStrong(String password) {
    if (password == null || password.length() < 12) {
        return false;
    }
    // Must contain: lowercase, uppercase, digit, special char
    return password.matches(PASSWORD_PATTERN);
}

// In handleChangePassword method:
// Validate new password
if (!isPasswordStrong(newPassword)) {
    ra.addFlashAttribute("error", 
        "Password must be at least 12 characters with uppercase, lowercase, numbers, and symbols.");
    return "redirect:/change-password";
}

// Check if password was recently used (prevent reuse)
if (passwordEncoder.matches(newPassword, user.getPassword())) {
    ra.addFlashAttribute("error", "You cannot reuse your previous password.");
    return "redirect:/change-password";
}
```


---

### **3. CRITICAL: No Rate Limiting on Authentication**

**Issue:** No protection against brute force attacks on:
- Login attempts
- Password reset requests
- Email verification

**🛠️ SOLUTION: Add Rate Limiting**

Create new file:

```java
package com.bypass.bypasstransers.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket loginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    @Bean
    public Bucket passwordResetBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }
}
```


Update `pom.xml` to add dependency:

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>
```


---

### **4. CRITICAL: Weak Default Credentials**

**File:** `BypasstransersApplication.java`

The hardcoded defaults are easily guessable:
```
superadmin / superpass
admin / adminpass
staff / staffpass
```


**🛠️ RECOMMENDATION:**

```java
// Replace the seeding section with:
@Bean
public ApplicationListener<ApplicationReadyEvent> init(AccountRepository accountRepo,
                                                        UserRepository userRepo,
                                                        PasswordEncoder passwordEncoder) {
    return event -> {
        try {
            long count = accountRepo.count();
            if (count == 0) {
                accountRepo.save(new Account(null, "Econet", BigDecimal.ZERO, new BigDecimal("0.033")));
                accountRepo.save(new Account(null, "InnBucks", BigDecimal.ZERO, new BigDecimal("0.02")));
                accountRepo.save(new Account(null, "Mukuru", BigDecimal.ZERO, new BigDecimal("0.015")));
                log.info("Seeded default accounts");
            }

            long users = userRepo.count();
            if (users == 0) {
                // GENERATE SECURE TEMPORARY PASSWORDS
                String superAdminPass = generateSecurePassword();
                String adminPass = generateSecurePassword();
                String staffPass = generateSecurePassword();

                User superAdmin = new User();
                superAdmin.setUsername("superadmin");
                superAdmin.setPassword(passwordEncoder.encode(superAdminPass));
                superAdmin.setPhoneNumber("+26377801140");
                superAdmin.setRole(Role.SUPER_ADMIN);
                userRepo.save(superAdmin);

                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode(adminPass));
                admin.setPhoneNumber("+263717847646");
                admin.setRole(Role.ADMIN);
                userRepo.save(admin);

                User staff = new User();
                staff.setUsername("staff");
                staff.setPassword(passwordEncoder.encode(staffPass));
                staff.setPhoneNumber("+1000000002");
                staff.setRole(Role.STAFF);
                userRepo.save(staff);

                // LOG CREDENTIALS SECURELY (only in logs, not console)
                log.info("⚠️  INITIAL CREDENTIALS (save in secure location):");
                log.info("superadmin: {}", superAdminPass);
                log.info("admin: {}", adminPass);
                log.info("staff: {}", staffPass);
                log.info("⚠️  These credentials will NOT be displayed again. Change them immediately.");
            }

            // Migrate any existing plain-text passwords to BCrypt
            List<User> all = userRepo.findAll();
            int migrated = 0;
            for (User u : all) {
                String pw = u.getPassword();
                if (pw != null && !pw.startsWith("$2")) {
                    u.setPassword(passwordEncoder.encode(pw));
                    userRepo.save(u);
                    migrated++;
                }
            }
            if (migrated > 0) log.info("Migrated {} plain-text passwords to BCrypt", migrated);

        } catch (Exception ex) {
            log.warn("Initialization skipped: {}", ex.getMessage());
        }
    };
}

private static String generateSecurePassword() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    StringBuilder password = new StringBuilder();
    java.util.Random random = new java.util.Random();
    for (int i = 0; i < 16; i++) {
        password.append(chars.charAt(random.nextInt(chars.length())));
    }
    return password.toString();
}
```


---

### **5. MEDIUM: Missing HSTS Preload**

**File:** `SecurityConfig.java` - **GOOD NEWS: Already configured!**

The HSTS header is properly set:
```java
.httpStrictTransportSecurity(hsts -> hsts
    .includeSubDomains(true)
    .maxAgeInSeconds(31536000)
    .preload(true)  // ✅ GOOD
)
```


---

### **6. MEDIUM: Insufficient Audit Logging**

**File:** `AuditService.java`

**Problems:**
- No IP address logging
- No login failure tracking
- Fallback to user ID `0L` is incorrect
- No timestamp in logs for audit trail

**🛠️ RECOMMENDATION:**

Create new file for better audit logging:

```java
package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.AuditLog;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AuditLogRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnhancedAuditService {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAuditService.class);

    private final AuditLogRepository repo;
    private final UserRepository userRepository;

    public EnhancedAuditService(AuditLogRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwarded = request.getHeader("X-Forwarded-For");
                if (xForwarded != null && !xForwarded.isEmpty()) {
                    return xForwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            logger.warn("Could not determine client IP", e);
        }
        return "UNKNOWN";
    }

    public void logSecurityEvent(String action, String details, boolean success) {
        String username = getCurrentUsername();
        String ipAddress = getClientIpAddress();
        Long userId = getCurrentUserId();

        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityName("security_event");
        log.setEntityId(userId != null ? userId : 0L);
        log.setOldValue(ipAddress); // Store IP for security tracking
        log.setNewValue(success ? "SUCCESS" : "FAILURE");
        log.setPerformedAt(LocalDateTime.now());
        log.setPerformedBy(userId);

        repo.save(log);
        
        logger.warn("SECURITY_AUDIT: action={}, user={}, ip={}, success={}, details={}",
                   action, username, ipAddress, success, details);
    }

    public void logFailedLogin(String username) {
        logSecurityEvent("LOGIN_FAILURE", "Failed login attempt for: " + username, false);
    }

    public void logSuccessfulLogin(String username) {
        logSecurityEvent("LOGIN_SUCCESS", "Successful login for: " + username, true);
    }

    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetails) {
                    return ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    return (String) principal;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not determine current username", e);
        }
        return "ANONYMOUS";
    }

    private Long getCurrentUserId() {
        try {
            String username = getCurrentUsername();
            if (!"ANONYMOUS".equals(username)) {
                List<User> users = userRepository.findByUsername(username);
                return users.isEmpty() ? null : users.get(0).getId();
            }
        } catch (Exception e) {
            logger.debug("Could not determine current user ID", e);
        }
        return null;
    }
}
```


---

### **7. MEDIUM: Password Reset Token Security**

**File:** `PasswordResetController.java`

**Issues:**
- Token expiry not visible
- No token rate limiting
- Token can be reused if not invalidated

**Recommendation:**
- Set token expiry to 15-30 minutes
- Invalidate token immediately after use
- Limit password reset requests to 3 per hour per user

---

### **8. LOW: Session Configuration**

**File:** `SecurityConfig.java` - **GOOD:**
```java
.sessionManagement(session -> session
    .maximumSessions(1)  // ✅ Prevents concurrent sessions
    .maxSessionsPreventsLogin(false)
)
.logout(logout -> logout
    .deleteCookies("JSESSIONID")  // ✅ Deletes session cookie
)
```


**HOWEVER - ADD MISSING CONFIGURATION:**

```java
// Add to filterChain method after sessionManagement:
            .sessionManagement(session -> session
                .sessionFixationProtection(org.springframework.security.config.Customizer.withDefaults())
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );
            
// Also add these critical cookie settings:
http.requiresChannel()
    .anyRequest()
    .requiresSecure(); // Force HTTPS in production

// Add cookie attributes before return statement:
java.util.stream.Stream.of("JSESSIONID")
    .forEach(cookieName -> {
        // These are handled by Spring Security
    });
```


---

### **9. CRITICAL: Missing Content-Type Validation**

Many endpoints don't validate input types, allowing:
- XXE (XML External Entity) attacks
- Content-Type spoofing

**Recommendation:** Add validation to all `@PostMapping` endpoints:

```java
@PostMapping("/api/endpoint")
public ResponseEntity<?> endpoint(
    @RequestHeader("Content-Type") String contentType,
    @RequestBody String payload
) {
    if (!contentType.contains("application/json")) {
        return ResponseEntity.badRequest()
            .body("Invalid Content-Type. Expected application/json");
    }
    // ... process payload
}
```


---

### **10. MEDIUM: SQL Injection Risk**

**File:** `SecurityService.java`

```java
List<User> users = userRepository.findByUsernameIgnoreCase(username);
```


**Current state:** ✅ SAFE - JPA uses parameterized queries

**However, ensure:**
- No string concatenation in queries
- Use `@Query` with named parameters if custom queries exist
- Regularly update Spring Data JPA

---

## **📋 SECURITY CHECKLIST - PRIORITY ORDER**

### 🔴 **CRITICAL (Do Immediately)**
- [ ] **DELETE** `DebugController.java` completely
- [ ] Remove hardcoded credentials from `BypasstransersApplication.java`
- [ ] Generate secure temporary passwords on first run
- [ ] Implement password complexity validation (12+ chars, mixed case, symbols, numbers)
- [ ] Add rate limiting (5 login attempts per 15 mins)

### 🟠 **HIGH (This Week)**
- [ ] Implement comprehensive audit logging with IP addresses
- [ ] Add failed login tracking and account lockout
- [ ] Log all authentication events
- [ ] Review and restrict all `@PreAuthorize` decorators
- [ ] Remove any test/debug code from production

### 🟡 **MEDIUM (This Month)**
- [ ] Add Content-Type validation to all endpoints
- [ ] Set token expiry to 15 minutes
- [ ] Implement password history (prevent reuse)
- [ ] Add session timeout configuration (15 mins)
- [ ] Enable HTTPS-only cookies

### 🟢 **LOW (Next Sprint)**
- [ ] Run dependency security scan
- [ ] Set up WAF (Web Application Firewall)
- [ ] Implement intrusion detection logging
- [ ] Add input validation framework

---

## **Recommended Dependency Additions**

Add to `pom.xml`:

```xml
<!-- Rate Limiting -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>

<!-- Password Validation -->
<dependency>
    <groupId>org.passay</groupId>
    <artifactId>passay</artifactId>
    <version>1.6.4</version>
</dependency>

<!-- Security Headers -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-web</artifactId>
    <version>6.2.0</version>
</dependency>

<!-- Dependency Check -->
<dependency>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.0</version>
    <scope>provided</scope>
</dependency>
```
detailed implementation code


Perfect! I'll provide comprehensive implementation code for all critical security fixes. Let me create the necessary files and modifications:

---

## **1. DELETE DebugController**Perfect! Let me provide comprehensive implementation code for all critical security fixes:

---

## **IMPLEMENTATION #1: Delete DebugController**

```java
// DELETE THIS ENTIRE FILE - It's a critical security vulnerability
// If you need debug functionality, use Spring Boot Actuator with proper security
```


**To remove it in IDE:**
1. Right-click on `DebugController.java`
2. Select **Delete**
3. Or use command: `rm src/main/java/com/bypass/bypasstransers/controller/DebugController.java`

---

## **IMPLEMENTATION #2: Password Strength Validation Service**

Create new file:

```java
package com.bypass.bypasstransers.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordStrengthService {

    private static final int MIN_LENGTH = 12;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?]");
    private static final Pattern COMMON_PATTERNS = Pattern.compile(
        "(?i)(password|123456|qwerty|admin|letmein|welcome|monkey|dragon|master|sunshine|princess|abc123)"
    );

    public PasswordValidationResult validate(String password) {
        PasswordValidationResult result = new PasswordValidationResult();
        
        if (password == null || password.isEmpty()) {
            result.addError("Password is required");
            return result;
        }

        // Length check
        if (password.length() < MIN_LENGTH) {
            result.addError("Password must be at least " + MIN_LENGTH + " characters long");
        }

        // Uppercase check
        if (!UPPERCASE.matcher(password).find()) {
            result.addError("Password must contain at least one uppercase letter (A-Z)");
        }

        // Lowercase check
        if (!LOWERCASE.matcher(password).find()) {
            result.addError("Password must contain at least one lowercase letter (a-z)");
        }

        // Digit check
        if (!DIGIT.matcher(password).find()) {
            result.addError("Password must contain at least one number (0-9)");
        }

        // Special character check
        if (!SPECIAL_CHAR.matcher(password).find()) {
            result.addError("Password must contain at least one special character (!@#$%^&*)");
        }

        // Common patterns check
        if (COMMON_PATTERNS.matcher(password).find()) {
            result.addError("Password contains commonly used patterns - please choose a stronger password");
        }

        // Check for sequential characters
        if (hasSequentialCharacters(password)) {
            result.addError("Password contains sequential characters - please vary your password");
        }

        // Check for repeated characters
        if (hasRepeatedCharacters(password)) {
            result.addError("Password has too many repeated characters - please vary your password");
        }

        result.setStrength(calculateStrength(result));
        return result;
    }

    private boolean hasSequentialCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);
            
            if ((c2 == c1 + 1 && c3 == c2 + 1) || (c2 == c1 - 1 && c3 == c2 - 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRepeatedCharacters(String password) {
        int maxRepeat = 0;
        int currentRepeat = 1;
        
        for (int i = 0; i < password.length() - 1; i++) {
            if (password.charAt(i) == password.charAt(i + 1)) {
                currentRepeat++;
                maxRepeat = Math.max(maxRepeat, currentRepeat);
            } else {
                currentRepeat = 1;
            }
        }
        
        return maxRepeat > 3; // More than 3 repeated chars is weak
    }

    private String calculateStrength(PasswordValidationResult result) {
        if (!result.isValid()) {
            return "WEAK";
        }
        
        int score = 0;
        String password = result.getTestedPassword();
        
        if (password.length() >= 16) score += 2;
        else if (password.length() >= 12) score += 1;
        
        if (UPPERCASE.matcher(password).find()) score += 1;
        if (LOWERCASE.matcher(password).find()) score += 1;
        if (DIGIT.matcher(password).find()) score += 1;
        if (SPECIAL_CHAR.matcher(password).find()) score += 2;
        
        if (score >= 6) return "STRONG";
        else if (score >= 4) return "MEDIUM";
        else return "WEAK";
    }

    public static class PasswordValidationResult {
        private List<String> errors = new ArrayList<>();
        private String strength = "WEAK";
        private String testedPassword;

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getStrength() {
            return strength;
        }

        public void setStrength(String strength) {
            this.strength = strength;
        }

        public String getTestedPassword() {
            return testedPassword;
        }

        public void setTestedPassword(String testedPassword) {
            this.testedPassword = testedPassword;
        }

        public String getErrorsAsString() {
            return String.join("; ", errors);
        }
    }
}
```


---

## **IMPLEMENTATION #3: Update PasswordResetController**

```java
package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.EmailSendOutcome;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.service.PasswordResetService;
import com.bypass.bypasstransers.service.AuditService;
import com.bypass.bypasstransers.service.PasswordStrengthService;
import com.bypass.bypasstransers.service.EnhancedAuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordStrengthService passwordStrengthService;
    private final EnhancedAuditService enhancedAuditService;

    public PasswordResetController(UserRepository userRepository, 
                                   PasswordResetService passwordResetService,
                                   PasswordEncoder passwordEncoder, 
                                   AuditService auditService,
                                   PasswordStrengthService passwordStrengthService,
                                   EnhancedAuditService enhancedAuditService) {
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.passwordStrengthService = passwordStrengthService;
        this.enhancedAuditService = enhancedAuditService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam(required = false) String emailOrUsername,
            @RequestParam(required = false) String email,
            RedirectAttributes ra) {
        
        String value = (emailOrUsername != null && !emailOrUsername.isBlank()) ? emailOrUsername : email;
        if (value == null || value.isBlank()) {
            ra.addFlashAttribute("error", "Please enter your email or username.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_ATTEMPT", "Empty email/username", false);
            return "redirect:/forgot-password";
        }
        
        String trimmed = value.trim();
        
        // Sanitize input to prevent injection
        if (!isValidEmailOrUsername(trimmed)) {
            ra.addFlashAttribute("error", "Invalid email or username format.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_ATTEMPT", "Invalid format: " + trimmed, false);
            return "redirect:/forgot-password";
        }

        List<User> emailUsers = userRepository.findByEmailIgnoreCase(trimmed);
        User user = emailUsers.isEmpty() ? null : emailUsers.get(0);
        
        if (user == null) {
            List<User> usernameUsers = userRepository.findByUsernameIgnoreCase(trimmed);
            user = usernameUsers.isEmpty() ? null : usernameUsers.get(0);
        }
        
        if (user == null) {
            // Don't reveal if user exists (security best practice)
            ra.addFlashAttribute("success", "If an account matches that email or username, you will receive a password reset link.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_ATTEMPT", "User not found: " + trimmed, false);
            return "redirect:/forgot-password";
        }

        EmailSendOutcome outcome = passwordResetService.createTokenForUser(user, null);
        
        if (outcome.getDisplayLinkOptional() != null) {
            ra.addFlashAttribute("success", "Reset link created (no email on file - use the link below).");
            ra.addFlashAttribute("resetLink", outcome.getDisplayLinkOptional());
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_TOKEN_GENERATED", "User: " + user.getUsername(), true);
        } else if (outcome.isSmtpSent()) {
            ra.addFlashAttribute("success", "Check your email for a password reset link (also check spam).");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_EMAIL_SENT", "User: " + user.getUsername(), true);
        } else {
            ra.addFlashAttribute("error",
                    "We could not send the reset email. Please try again later or contact support.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_EMAIL_FAILED", "User: " + user.getUsername(), false);
        }
        
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset")
    public String resetForm(@RequestParam(required = false) String token, 
                           Model model, RedirectAttributes ra) {
        if (token == null || passwordResetService.validateTokenAndFetchUser(token) == null) {
            ra.addFlashAttribute("error", "Invalid or expired reset token.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET_INVALID_TOKEN", "Token validation failed", false);
            return "redirect:/login";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset")
    public String handleReset(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String password,
            RedirectAttributes ra) {
        
        if (token == null || token.isBlank()) {
            ra.addFlashAttribute("error", "Reset token is missing. Please request a new link.");
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET", "Missing token", false);
            return "redirect:/forgot-password";
        }

        // Validate password strength
        PasswordStrengthService.PasswordValidationResult validation = 
            passwordStrengthService.validate(password);
        
        if (!validation.isValid()) {
            ra.addFlashAttribute("error", "Password does not meet requirements: " + validation.getErrorsAsString());
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET", "Weak password provided", false);
            return "redirect:/reset?token=" + token;
        }

        boolean ok = passwordResetService.resetPassword(token, password);
        
        if (!ok) {
            enhancedAuditService.logSecurityEvent("PASSWORD_RESET", "Token validation failed", false);
            return "redirect:/login?reset=error";
        }
        
        enhancedAuditService.logSecurityEvent("PASSWORD_RESET_SUCCESS", "Password reset completed", true);
        return "redirect:/login?reset=success";
    }

    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String handleChangePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            RedirectAttributes ra) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            ra.addFlashAttribute("error", "You must be logged in to change your password.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Not authenticated", false);
            return "redirect:/login";
        }

        String username = authentication.getName();
        List<User> users = userRepository.findByUsernameIgnoreCase(username);
        User user = users.isEmpty() ? null : users.get(0);
        
        if (user == null) {
            ra.addFlashAttribute("error", "User not found.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "User not found: " + username, false);
            return "redirect:/app";
        }

        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Current password is incorrect.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Wrong current password", false);
            return "redirect:/change-password";
        }

        // Validate new password strength
        PasswordStrengthService.PasswordValidationResult validation = 
            passwordStrengthService.validate(newPassword);
        
        if (!validation.isValid()) {
            ra.addFlashAttribute("error", "New password does not meet requirements: " + validation.getErrorsAsString());
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Weak password", false);
            return "redirect:/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New password and confirmation do not match.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Password mismatch", false);
            return "redirect:/change-password";
        }

        // Prevent password reuse
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "You cannot reuse your previous password.");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGE_FAILED", "Password reuse attempt", false);
            return "redirect:/change-password";
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Audit log
        try {
            auditService.logEntity(username, "users", user.getId(), "PASSWORD_CHANGE", "[REDACTED]", "[REDACTED]");
            enhancedAuditService.logSecurityEvent("PASSWORD_CHANGED", "User: " + username, true);
        } catch (Exception e) {
            // Log to system logger but continue
        }

        ra.addFlashAttribute("success", "Password changed successfully. You will need to log in again.");
        return "redirect:/login";
    }

    /**
     * Validate email or username format to prevent injection
     */
    private boolean isValidEmailOrUsername(String value) {
        // Check length
        if (value == null || value.length() > 255 || value.length() < 2) {
            return false;
        }
        
        // Email validation (RFC 5322 simplified)
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        
        // Username validation
        String usernamePattern = "^[A-Za-z0-9_.-]+$";
        
        return value.matches(emailPattern) || value.matches(usernamePattern);
    }
}
```


---

## **IMPLEMENTATION #4: Enhanced Audit Service**

```java
package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.AuditLog;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AuditLogRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnhancedAuditService {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAuditService.class);

    private final AuditLogRepository repo;
    private final UserRepository userRepository;

    public EnhancedAuditService(AuditLogRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    /**
     * Get client IP address from request headers
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                
                // Check for IP forwarded by proxy
                String xForwarded = request.getHeader("X-Forwarded-For");
                if (xForwarded != null && !xForwarded.isEmpty()) {
                    return xForwarded.split(",")[0].trim();
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            logger.debug("Could not determine client IP", e);
        }
        return "UNKNOWN";
    }

    /**
     * Log a security event with full context
     */
    public void logSecurityEvent(String action, String details, boolean success) {
        try {
            String username = getCurrentUsername();
            String ipAddress = getClientIpAddress();
            Long userId = getCurrentUserId();

            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setEntityName("security_event");
            log.setEntityId(userId != null ? userId : 0L);
            log.setOldValue(ipAddress); // Store IP for security tracking
            log.setNewValue(success ? "SUCCESS" : "FAILURE");
            log.setPerformedAt(LocalDateTime.now());
            log.setPerformedBy(userId);

            repo.save(log);
            
            logger.warn("SECURITY_AUDIT [{}] user={}, ip={}, action={}, success={}, details={}",
                       LocalDateTime.now(), username, ipAddress, action, success, details);
        } catch (Exception e) {
            logger.error("Failed to log security event", e);
        }
    }

    /**
     * Log failed login attempt
     */
    public void logFailedLogin(String username) {
        String ipAddress = getClientIpAddress();
        logSecurityEvent("LOGIN_FAILURE", "Failed login attempt for: " + username + " from " + ipAddress, false);
    }

    /**
     * Log successful login
     */
    public void logSuccessfulLogin(String username) {
        String ipAddress = getClientIpAddress();
        logSecurityEvent("LOGIN_SUCCESS", "Successful login for: " + username + " from " + ipAddress, true);
    }

    /**
     * Log unauthorized access attempt
     */
    public void logUnauthorizedAccess(String resource, String username) {
        logSecurityEvent("UNAUTHORIZED_ACCESS", "Attempted to access: " + resource, false);
    }

    /**
     * Log data modification
     */
    public void logDataModification(String entityName, Long entityId, String action, String oldValue, String newValue) {
        String username = getCurrentUsername();
        logSecurityEvent(action, "Modified " + entityName + " #" + entityId, true);
        
        try {
            Long userId = getCurrentUserId();
            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setEntityName(entityName);
            log.setEntityId(entityId != null ? entityId : 0L);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setPerformedAt(LocalDateTime.now());
            log.setPerformedBy(userId);
            repo.save(log);
        } catch (Exception e) {
            logger.error("Failed to log data modification", e);
        }
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetails) {
                    return ((UserDetails) principal).getUsername();
                } else if (principal instanceof String) {
                    return (String) principal;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not determine current username", e);
        }
        return "ANONYMOUS";
    }

    /**
     * Get current authenticated user ID
     */
    private Long getCurrentUserId() {
        try {
            String username = getCurrentUsername();
            if (!"ANONYMOUS".equals(username)) {
                List<User> users = userRepository.findByUsername(username);
                return users.isEmpty() ? null : users.get(0).getId();
            }
        } catch (Exception e) {
            logger.debug("Could not determine current user ID", e);
        }
        return null;
    }
}
```


---

## **IMPLEMENTATION #5: Rate Limiting Interceptor**

```java
package com.bypass.bypasstransers.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Define rate limits per endpoint
    private static final int LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_WINDOW_MINUTES = 15;
    
    private static final int PASSWORD_RESET_ATTEMPTS = 3;
    private static final int PASSWORD_RESET_WINDOW_HOURS = 1;
    
    private static final int API_REQUESTS = 100;
    private static final int API_WINDOW_MINUTES = 1;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        String path = request.getRequestURI();
        String clientIp = getClientIp(request);
        String key = clientIp;

        // Apply different rate limits based on endpoint
        if (path.contains("/login") && request.getMethod().equals("POST")) {
            return checkRateLimit(response, key, LOGIN_ATTEMPTS, LOGIN_WINDOW_MINUTES, "Login");
        } else if (path.contains("/forgot-password") || path.contains("/reset")) {
            return checkRateLimit(response, key, PASSWORD_RESET_ATTEMPTS, PASSWORD_RESET_WINDOW_HOURS * 60, "Password Reset");
        } else if (path.contains("/api/")) {
            return checkRateLimit(response, key, API_REQUESTS, API_WINDOW_MINUTES, "API");
        }

        return true;
    }

    private boolean checkRateLimit(HttpServletResponse response, String key, int tokens, int windowMinutes, String endpoint) {
        Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket(tokens, windowMinutes));

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            logger.warn("Rate limit exceeded for {} - endpoint: {}, ip: {}", endpoint, key);
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
            } catch (Exception e) {
                logger.error("Failed to write rate limit response", e);
            }
            return false;
        }
    }

    private Bucket createNewBucket(int tokens, int windowMinutes) {
        Bandwidth limit = Bandwidth.classic(tokens, Refill.intervally(tokens, Duration.ofMinutes(windowMinutes)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
```


---

## **IMPLEMENTATION #6: Register Rate Limiting in WebConfig**

```java
package com.bypass.bypasstransers.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/img/**", "/videos/**");
    }
}
```


---

## **IMPLEMENTATION #7: Update BypasstransersApplication.java**

```java
package com.bypass.bypasstransers;

import com.bypass.bypasstransers.model.Account;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import com.bypass.bypasstransers.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class BypasstransersApplication {

    private static final Logger log = LoggerFactory.getLogger(BypasstransersApplication.class);

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(BypasstransersApplication.class, args);
    }

    private static void loadDotEnv() {
        java.io.File envFile = new java.io.File(".env");
        if (envFile.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eqPos = line.indexOf('=');
                    if (eqPos > 0) {
                        String key = line.substring(0, eqPos).trim();
                        String value = line.substring(eqPos + 1).trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                }
                log.info("Loaded variables from .env file");
            } catch (java.io.IOException e) {
                log.warn("Failed to load .env file: {}", e.getMessage());
            }
        }
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> init(AccountRepository accountRepo,
                                                            UserRepository userRepo,
                                                            PasswordEncoder passwordEncoder) {
        return event -> {
            try {
                long count = accountRepo.count();
                if (count == 0) {
                    accountRepo.save(new Account(null, "Econet", BigDecimal.ZERO, new BigDecimal("0.033")));
                    accountRepo.save(new Account(null, "InnBucks", BigDecimal.ZERO, new BigDecimal("0.02")));
                    accountRepo.save(new Account(null, "Mukuru", BigDecimal.ZERO, new BigDecimal("0.015")));
                    log.info("Seeded default accounts");
                }

                long users = userRepo.count();
                if (users == 0) {
                    // GENERATE SECURE TEMPORARY PASSWORDS
                    String superAdminPass = generateSecurePassword();
                    String adminPass = generateSecurePassword();
                    String staffPass = generateSecurePassword();

                    User superAdmin = new User();
                    superAdmin.setUsername("superadmin");
                    superAdmin.setPassword(passwordEncoder.encode(superAdminPass));
                    superAdmin.setPhoneNumber("+26377801140");
                    superAdmin.setRole(Role.SUPER_ADMIN);
                    userRepo.save(superAdmin);

                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword(passwordEncoder.encode(adminPass));
                    admin.setPhoneNumber("+263717847646");
                    admin.setRole(Role.ADMIN);
                    userRepo.save(admin);

                    User staff = new User();
                    staff.setUsername("staff");
                    staff.setPassword(passwordEncoder.encode(staffPass));
                    staff.setPhoneNumber("+1000000002");
                    staff.setRole(Role.STAFF);
                    userRepo.save(staff);

                    // ⚠️ LOG CREDENTIALS SECURELY (only in logs, not console)
                    log.warn("🔐 INITIAL CREDENTIALS GENERATED (Change passwords immediately):");
                    log.warn("superadmin: {}", superAdminPass);
                    log.warn("admin: {}", adminPass);
                    log.warn("staff: {}", staffPass);
                    log.warn("⚠️  These credentials will NOT be displayed again. Change them after first login.");
                }

                // Migrate any existing plain-text passwords to BCrypt
                List<User> all = userRepo.findAll();
                int migrated = 0;
                for (User u : all) {
                    String pw = u.getPassword();
                    if (pw != null && !pw.startsWith("$2")) {
                        u.setPassword(passwordEncoder.encode(pw));
                        userRepo.save(u);
                        migrated++;
                    }
                }
                if (migrated > 0) {
                    log.info("Migrated {} plain-text passwords to BCrypt", migrated);
                }

            } catch (Exception ex) {
                log.warn("Initialization skipped: {}", ex.getMessage());
            }
        };
    }

    /**
     * Generate a cryptographically secure random password
     * Format: 16 characters with mixed case, numbers, and symbols
     */
    private static String generateSecurePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*_+-=";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < 16; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
}
```


---

## **IMPLEMENTATION #8: Authentication Events Listener (Login/Logout Tracking)**

```java
package com.bypass.bypasstransers.config;

import com.bypass.bypasstransers.service.EnhancedAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEventsListener {

    @Autowired(required = false)
    private EnhancedAuditService enhancedAuditService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            User user = (User) auth.getPrincipal();
            if (enhancedAuditService != null) {
                enhancedAuditService.logSuccessfulLogin(user.getUsername());
            }
        }
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        if (event.getAuthentication() != null) {
            String username = event.getAuthentication().getName();
            if (enhancedAuditService != null) {
                enhancedAuditService.logFailedLogin(username);
            }
        }
    }
}
```


---

## **IMPLEMENTATION #9: Update pom.xml - Add Dependencies**

```xml
<!-- Add these dependencies inside <dependencies> section -->

<!-- Rate Limiting -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>

<!-- Enhanced Security -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-web</artifactId>
    <version>6.2.0</version>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>

<!-- Jakarta Servlet API (for new versions) -->
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.0.0</version>
    <scope>provided</scope>
</dependency>
```


---

## **IMPLEMENTATION #10: Update SecurityConfig - Enhanced Headers**

```java
// Add this to the headers section for additional cookie security:

.headers(headers -> headers
    .frameOptions(frame -> frame.sameOrigin())
    .contentTypeOptions(cto -> {})
    .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
    .contentSecurityPolicy(csp -> csp
        .policyDirectives(
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
            "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com https://cdn.jsdelivr.net data:; " +
            "img-src 'self' data: https:; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none';"
        )
    )
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)
        .preload(true)
    )
    .referrerPolicy(referrer -> referrer
        .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
    )
)
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/actuator/health")
)
// Add secure cookie settings
.rememberMe(rememberMe -> rememberMe
    .rememberMeCookieName("app-remember-me")
    .useSecureCookie(true)
    .alwaysRemember(false)
    .tokenValiditySeconds(604800) // 7 days
);
```


---

## **STEP-BY-STEP IMPLEMENTATION CHECKLIST**

### **Phase 1: Immediate (Today)**
- [ ] 1. **Delete** `DebugController.java`
- [ ] 2. Create `PasswordStrengthService.java`
- [ ] 3. Create `EnhancedAuditService.java`
- [ ] 4. Update `PasswordResetController.java`

### **Phase 2: Security Hardening (This Week)**
- [ ] 5. Create `RateLimitInterceptor.java`
- [ ] 6. Update `WebConfig.java`
- [ ] 7. Update `BypasstransersApplication.java`
- [ ] 8. Update `AuthenticationEventsListener.java`

### **Phase 3: Deployment Ready (Before Production)**
- [ ] 9. Update `pom.xml` with new dependencies
- [ ] 10. Update `SecurityConfig.java` with enhanced headers
- [ ] 11. Run `mvn clean install`
- [ ] 12. Test all authentication flows
- [ ] 13. Run security scanning tools

---

## **Testing Commands**

After implementation, test with:

```shell script
# Test password strength
curl -X POST http://localhost:8080/change-password \
  -d "currentPassword=old&newPassword=weak&confirmPassword=weak"

# Test rate limiting
for i in {1..6}; do
  curl -X POST http://localhost:8080/login -d "username=test&password=test"
done

# Should get 429 (Too Many Requests) on 6th attempt
```


---

These implementations provide:
✅ **Secure password validation**
✅ **Rate limiting on sensitive endpoints**
✅ **Comprehensive audit logging with IP tracking**
✅ **Secure credential generation**
✅ **Authentication event tracking**
✅ **Removal of critical vulnerabilities**

Would you like me to explain any specific implementation or help with testing?