# Multi-Currency & Branch Management Setup Guide

## Overview
The system now supports international branches with location-specific currencies:
- **South Africa**: ZAR (Rand)
- **Russia**: RUB (Ruble)
- **Zimbabwe**: USD (Dollar)
- **Future branches**: EUR, GBP, ZWL, NGN, KES supported

## Key Features

### 1. Branch Management
- Each branch has its own base currency
- Users and wallets are assigned to branches
- Branch admins can manage their own branch data

### 2. Currency Conversion
- Automatic exchange rate updates via API (exchangerate-api.com)
- Support for all major currency pairs
- Triangular arbitrage via USD when direct rates unavailable

### 3. User Base Currency
- Users inherit their branch's currency by default
- Can override with personal base currency preference
- All transactions display in user's base currency

## Database Migration

### Step 1: Run the Migration Script
The migration script `V4__add_branch_and_exchange_rate.sql` will automatically run on application startup via Flyway.

**What it does:**
- Creates `branch` table with 3 default branches
- Creates `exchange_rate` table with initial rates
- Adds `branch_id` and `base_currency` columns to users
- Adds `branch_id` column to accounts/wallets

### Step 2: Assign Existing Data
Run `data-migration.sql` manually after the application starts:

```sql
-- Execute this script once after first startup
-- This assigns existing users/wallets to Zimbabwe branch (default)
```

You can run this via:
- pgAdmin
- psql command line: `psql -d bypass_records -f src/main/resources/data-migration.sql`
- Your database management tool

## Configuration

### Exchange Rate API (application.properties)
```properties
# Use 'demo' for free tier or get API key from exchangerate-api.com
exchange.rate.api.key=demo
exchange.rate.api.url=https://api.exchangerate-api.com/v4/latest/

# Daily update at 9 AM UTC (cron expression)
exchange.rate.update.cron=0 0 9 * * *
```

### For Production (Recommended)
Get a free API key from https://www.exchangerate-api.com/ and update:
```properties
exchange.rate.api.key=YOUR_API_KEY_HERE
```

## API Endpoints

### Branch Management
```
GET    /admin/branches              - List all active branches
GET    /admin/branches/all          - List all branches (including inactive)
GET    /admin/branches/{id}         - Get specific branch
POST   /admin/branches              - Create new branch (SUPER_ADMIN only)
PUT    /admin/branches/{id}         - Update branch (SUPER_ADMIN only)
DELETE /admin/branches/{id}         - Deactivate branch (SUPER_ADMIN only)
POST   /admin/branches/{id}/activate - Activate branch (SUPER_ADMIN only)
```

### Exchange Rates
```
GET    /admin/exchange-rates           - Get all rates
GET    /admin/exchange-rates/{from}/{to} - Get specific rate
POST   /admin/exchange-rates/update     - Trigger API update (SUPER_ADMIN only)
PUT    /admin/exchange-rates/{from}/{to} - Manual override (SUPER_ADMIN only)
GET    /admin/exchange-rates/last-update - Last update timestamp
```

## Usage Examples

### Creating a New Branch (via API)
```bash
curl -X POST http://localhost:8080/admin/branches \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "Kenya Branch",
    "country": "Kenya",
    "currency": "KES",
    "address": "Nairobi Office",
    "contactEmail": "kenya@bypasstransers.com",
    "contactPhone": "+254-700-123456"
  }'
```

### Manually Updating Exchange Rate
```bash
curl -X PUT http://localhost:8080/admin/exchange-rates/USD/ZAR \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"rate": 18.50}'
```

### Converting Currency (in code)
```java
@Autowired
private CurrencyConversionService conversionService;

// Convert 100 USD to ZAR
Double amountInZar = conversionService.convert(100.0, "USD", "ZAR");

// Get exchange rate
Double rate = conversionService.getExchangeRate("USD", "ZAR");
```

## Scheduled Tasks

### Daily Exchange Rate Update
- **Time**: 9:00 AM UTC daily
- **Currencies Updated**: USD, ZAR, RUB, EUR, GBP (as base currencies)
- **Fallback**: If API fails, uses last known rates
- **Logs**: Check application logs for "Exchange rate update completed"

To change the schedule, update `exchange.rate.update.cron` in application.properties.

## Testing

### Verify Branch Creation
```sql
SELECT * FROM branch;
-- Should show 3 branches: Zimbabwe, South Africa, Russia
```

### Verify Exchange Rates
```sql
SELECT * FROM exchange_rate 
WHERE from_currency = 'USD' 
ORDER BY to_currency;
-- Should show rates for all supported currencies
```

### Verify User Assignment
```sql
SELECT u.username, u.base_currency, b.name as branch_name, b.currency as branch_currency
FROM users u
JOIN branch b ON u.branch_id = b.id;
-- All users should have branch assignments
```

## Troubleshooting

### Issue: Exchange rates not updating
**Solution:**
1. Check API key is valid
2. Verify internet connectivity
3. Check logs: `logging.level.com.bypass=DEBUG`
4. Test API manually: `https://api.exchangerate-api.com/v4/latest/USD`

### Issue: Users without branch assignment
**Solution:**
Run the data migration script again:
```sql
UPDATE users SET branch_id = 1 WHERE branch_id IS NULL;
```

### Issue: Currency conversion fails
**Solution:**
Ensure exchange rates exist for the currency pair:
```sql
INSERT INTO exchange_rate (from_currency, to_currency, rate, source)
VALUES ('USD', 'ZAR', 18.45, 'MANUAL');
```

## Next Steps

### Frontend Integration (Not Included)
You'll need to create UI pages for:
1. **Admin Branches Dashboard** (`admin-branches.html`)
   - List branches with edit/delete actions
   - Add new branch form
   - Branch statistics

2. **Exchange Rates Dashboard** (`admin-exchange-rates.html`)
   - Live rate table
   - Manual override form
   - Last update indicator

3. **User/Wallet Forms Update**
   - Branch selection dropdown
   - Auto-populate currency based on branch
   - Allow base currency override

### Backend Enhancements
- Filter transactions/reports by branch
- Branch-specific performance analytics
- Multi-currency profit/loss reporting
- Cross-branch transfer fees

## Support

For issues or questions:
- Check application logs
- Review API documentation
- Test endpoints with Postman/curl
- Verify database state with SQL queries
