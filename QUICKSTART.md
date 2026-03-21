# Quick Start Guide - Multi-Currency & Branch System

## 🎯 What's Been Implemented

Your bypass transers system now supports **international branches** with their own currencies:

### Supported Countries & Currencies:
- 🇿🇼 Zimbabwe → USD (US Dollar)
- 🇿🇦 South Africa → ZAR (Rand)
- 🇷🇺 Russia → RUB (Ruble)
- 🇪🇺 Europe → EUR (Euro)
- 🇬🇧 UK → GBP (British Pound)
- 🇳🇬 Nigeria → NGN (Naira)
- 🇰🇪 Kenya → KES (Shilling)
- 🇿🇼 Zimbabwe Local → ZWL

## 🚀 First-Time Setup

### Step 1: Database Migration (Automatic)
When you first start the application, Flyway will automatically:
- Create `branch` table with 3 default branches
- Create `exchange_rate` table with initial rates
- Add `branch_id` and `base_currency` to users
- Add `branch_id` to wallets/accounts

**Just start the app normally:**
```bash
./mvnw spring-boot:run
```

### Step 2: Migrate Existing Data (Manual - IMPORTANT!)
After the app starts for the first time, you MUST run the data migration script to assign existing users to branches.

**Option A: Via psql command line**
```bash
psql -U postgres -d bypass_records -f src/main/resources/data-migration.sql
```

**Option B: Via pgAdmin**
1. Open pgAdmin
2. Connect to `bypass_records` database
3. Open Query Tool
4. Copy and paste contents of `src/main/resources/data-migration.sql`
5. Execute

**What this does:**
- Assigns all existing users to Zimbabwe branch (default)
- Sets their base currency to USD
- Assigns all wallets to the same branch

### Step 3: Verify Installation

**Test API endpoints:**
```bash
# List all branches
curl http://localhost:8080/admin/branches

# Should return JSON with 3 branches:
# [
#   {"id":1,"name":"Zimbabwe Headquarters","country":"Zimbabwe","currency":"USD",...},
#   {"id":2,"name":"South Africa Branch","country":"South Africa","currency":"ZAR",...},
#   {"id":3,"name":"Russia Branch","country":"Russia","currency":"RUB",...}
# ]
```

**Check database:**
```sql
-- Should show 3 branches
SELECT * FROM branch;

-- Should show exchange rates
SELECT COUNT(*) FROM exchange_rate;

-- Users should have branch assignments
SELECT username, base_currency, b.name as branch 
FROM users u 
JOIN branch b ON u.branch_id = b.id;
```

## 📱 How to Use

### 1. Access Branch Management Dashboard
Open your browser and go to:
```
http://localhost:8080/admin-branches.html
```

You'll see:
- Statistics bar showing total branches, active branches, currencies
- Grid of branch cards with details
- "Add New Branch" button

### 2. Create a New Branch (e.g., Kenya)
Click "Add New Branch" and fill in:
- **Branch Name**: Kenya Branch
- **Country**: Kenya
- **Currency**: KES (auto-suggested based on country)
- **Address**: Nairobi Office, Kenyatta Avenue
- **Email**: kenya@bypasstransers.com
- **Phone**: +254-700-123456

Click "Save Branch" - it's now active!

### 3. View/Update Exchange Rates
The system automatically updates rates daily at 9 AM UTC.

**To manually trigger update:**
```bash
curl -X POST http://localhost:8080/admin/exchange-rates/update
```

**To check current USD to ZAR rate:**
```bash
curl http://localhost:8080/admin/exchange-rates/USD/ZAR
```

**To manually override a rate:**
```bash
curl -X PUT http://localhost:8080/admin/exchange-rates/USD/ZAR \
  -H "Content-Type: application/json" \
  -d '{"rate": 18.50}'
```

### 4. Currency Conversion in Code
```java
@Autowired
private CurrencyConversionService conversionService;

// Convert 100 USD to South African Rand
Double zarAmount = conversionService.convert(100.0, "USD", "ZAR");
// Result: ~1845.00 ZAR (depending on current rate)

// Get exchange rate directly
Double rate = conversionService.getExchangeRate("USD", "ZAR");
// Result: 18.45
```

## 🔧 Configuration

### Change Exchange Rate Update Time
Edit `application.properties`:
```properties
# Default: 9 AM UTC daily
# Format: second minute hour day month weekday
exchange.rate.update.cron=0 0 9 * * *

# Example: Update every 6 hours
exchange.rate.update.cron=0 0 */6 * * *

# Example: Update at midnight
exchange.rate.update.cron=0 0 0 * * *
```

### Use Production API Key
Get free key from https://www.exchangerate-api.com/

Update `application.properties`:
```properties
exchange.rate.api.key=YOUR_API_KEY_HERE
```

## 📊 Testing Scenarios

### Test 1: Create Branch via API
```bash
curl -X POST http://localhost:8080/admin/branches \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Branch",
    "country": "Test Country",
    "currency": "EUR",
    "address": "Test Address 123",
    "contactEmail": "test@example.com",
    "contactPhone": "+1234567890"
  }'
```

### Test 2: Convert Between Currencies
```java
// Test direct conversion
Double result1 = conversionService.convert(100.0, "USD", "ZAR");

// Test reverse conversion
Double result2 = conversionService.convert(1000.0, "ZAR", "USD");

// Test triangular arbitrage (no direct rate)
Double result3 = conversionService.convert(100.0, "NGN", "KES");
// Will convert: NGN -> USD -> KES
```

### Test 3: Branch Filtering
```java
// Get all users in South Africa branch
List<User> saUsers = userRepository.findByBranchId(2L);

// Get all wallets in Russia branch  
List<Wallet> ruWallets = walletRepository.findByBranchId(3L);
```

## ⚠️ Troubleshooting

### Issue: "Table 'branch' doesn't exist"
**Solution:** The migration didn't run. Check:
- Flyway is enabled: `spring.flyway.enabled=true`
- Migration file exists: `src/main/resources/db/migration/V4__add_branch_and_exchange_rate.sql`
- Check logs for Flyway errors

### Issue: "Users without branch assignment"
**Solution:** Run the data migration script (Step 2 above)

### Issue: "Exchange rates not updating"
**Solution:**
1. Check internet connectivity
2. Test API manually: https://api.exchangerate-api.com/v4/latest/USD
3. Check logs for scheduler errors
4. Trigger manual update via API

### Issue: "Currency conversion fails"
**Solution:**
```sql
-- Check if rate exists
SELECT * FROM exchange_rate 
WHERE from_currency = 'USD' AND to_currency = 'ZAR';

-- If missing, insert manually
INSERT INTO exchange_rate (from_currency, to_currency, rate, source)
VALUES ('USD', 'ZAR', 18.45, 'MANUAL');
```

## 📋 API Endpoints Cheat Sheet

### Branch Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/branches` | List active branches |
| GET | `/admin/branches/all` | List all branches |
| GET | `/admin/branches/{id}` | Get specific branch |
| POST | `/admin/branches` | Create new branch |
| PUT | `/admin/branches/{id}` | Update branch |
| DELETE | `/admin/branches/{id}` | Deactivate branch |
| POST | `/admin/branches/{id}/activate` | Activate branch |

### Exchange Rates
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/exchange-rates` | Get all rates |
| GET | `/admin/exchange-rates/{from}/{to}` | Get specific rate |
| POST | `/admin/exchange-rates/update` | Trigger API update |
| PUT | `/admin/exchange-rates/{from}/{to}` | Manual override |
| GET | `/admin/exchange-rates/last-update` | Last update time |

## 🎉 Success Indicators

You know everything is working when:
- ✅ `curl /admin/branches` returns 3 branches
- ✅ Users have `branch_id` and `base_currency` set
- ✅ Wallets have `branch_id` set
- ✅ `exchange_rate` table has 30+ entries
- ✅ Scheduler logs show "Exchange rate update completed"
- ✅ Branch dashboard loads at `/admin-branches.html`

## 📞 Next Steps

After setup, consider:
1. Creating additional branches for other countries
2. Updating exchange rate API key for production
3. Building frontend UI for exchange rate management
4. Adding branch filtering to transaction reports
5. Implementing branch-specific analytics

---

**Need Help?**
- Full documentation: `MULTI_CURRENCY_SETUP.md`
- Implementation details: `IMPLEMENTATION_SUMMARY.md`
- Check application logs for detailed error messages
