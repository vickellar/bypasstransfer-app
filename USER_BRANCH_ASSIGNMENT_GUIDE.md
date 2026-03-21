# User & Branch Assignment Guide

## ✅ New Feature: Admin Can Now Assign Staff to Branches!

The system now includes a comprehensive **User Management Dashboard** where admins can assign users/staff to specific branches.

---

## 🎯 What You Can Do Now

### 1. **View All Users with Branch Assignments**
- See which branch each user belongs to
- Filter users by branch, role, or status
- Search users by username or email

### 2. **Assign Users to Branches**
- Edit any user and assign them to a branch
- User's base currency auto-updates to match branch currency
- Can override currency if needed

### 3. **Manage User Details**
- Update email, phone, role
- Change password
- Activate/deactivate users

---

## 📱 How to Use

### Access the Dashboard

**Option 1: Via Browser (Recommended)**
```
http://localhost:8080/admin-users.html
```

**Option 2: Existing User Page**
```
http://localhost:8080/users
```

---

## 🖥️ User Management Dashboard Features

### Dashboard Layout

```
┌─────────────────────────────────────────────────────┐
│  👥 User & Branch Assignment          [+ Add User] │
├─────────────────────────────────────────────────────┤
│  Stats: Total | Branches | Active | Staff          │
├─────────────────────────────────────────────────────┤
│  Filters:                                           │
│  [Branch ▼] [Role ▼] [Status ▼] [Search 🔍]       │
├─────────────────────────────────────────────────────┤
│  User Table:                                        │
│  ID | Username | Email | Role | Branch | Currency  │
│  ... (all users listed)                             │
└─────────────────────────────────────────────────────┘
```

### Step-by-Step: Assign User to Branch

1. **Open Dashboard**
   - Go to `/admin-users.html`
   - See list of all users

2. **Click "Edit" on Any User**
   - Opens edit modal
   - Shows current branch assignment

3. **Select Branch**
   - Dropdown shows all active branches
   - Example: "South Africa Branch (ZAR)"
   - Auto-selects branch currency

4. **Adjust Base Currency (Optional)**
   - Defaults to branch currency
   - Can override if needed
   - Example: User in SA branch but uses USD

5. **Save Changes**
   - Click "Save Changes"
   - User now assigned to branch
   - Updates immediately in table

---

## 📊 Filter & Search

### Filter by Branch
```
[All Branches ▼]
├─ Zimbabwe Headquarters (USD)
├─ South Africa Branch (ZAR)
└─ Russia Branch (RUB)
```

### Filter by Role
```
[All Roles ▼]
├─ Super Admin
├─ Admin
├─ Staff
└─ User
```

### Filter by Status
```
[All Statuses ▼]
├─ Active
└─ Inactive
```

### Search Box
Type to search:
- Username: "john.doe"
- Email: "john@example.com"

---

## 🔧 API Endpoints

### Get All Users (with Branch Info)
```bash
GET /users/api/all

Response:
[
  {
    "id": 1,
    "username": "admin",
    "email": "admin@bypasstransers.com",
    "role": "ADMIN",
    "active": true,
    "baseCurrency": "USD",
    "branch": {
      "id": 1,
      "name": "Zimbabwe Headquarters",
      "country": "Zimbabwe",
      "currency": "USD"
    }
  },
  ...
]
```

### Update User with Branch
```bash
PUT /admin/users/5
Content-Type: application/json

{
  "email": "newemail@example.com",
  "role": "STAFF",
  "baseCurrency": "ZAR",
  "branch": {
    "id": 2  // South Africa Branch
  }
}
```

---

## 💡 Use Cases

### Scenario 1: Onboard New Employee
1. Create new user account
2. Assign to appropriate branch (e.g., South Africa)
3. Set role to STAFF
4. Base currency auto-set to ZAR
5. User can now process transactions for that branch

### Scenario 2: Transfer Employee Between Branches
1. Find user in dashboard
2. Click "Edit"
3. Change branch from "Russia" to "South Africa"
4. Base currency updates from RUB to ZAR
5. Save - user now reports to SA branch

### Scenario 3: Promote Staff to Admin
1. Find staff member in dashboard
2. Click "Edit"
3. Change role from STAFF to ADMIN
4. Optionally change branch assignment
5. Save - user now has admin privileges

### Scenario 4: Deactivate Departing Employee
1. Find user in dashboard
2. Click "Deactivate"
3. User marked as inactive
4. Preserves all historical data
5. Can reactivate later if needed

---

## 📋 Database Fields

### User Table Updates
```sql
-- New columns added
ALTER TABLE users ADD COLUMN branch_id BIGINT;
ALTER TABLE users ADD COLUMN base_currency VARCHAR(10);

-- Example query: Get all users in South Africa branch
SELECT * FROM users WHERE branch_id = 2;

-- Example query: Count users per branch
SELECT b.name, COUNT(u.id) 
FROM branch b 
LEFT JOIN users u ON b.id = u.branch_id 
GROUP BY b.name;
```

---

## 🎨 Dashboard UI Features

### Color-Coded Badges
- **Purple**: Super Admin
- **Blue**: Admin
- **Cyan**: Staff
- **Gray**: User
- **Green**: Active
- **Red**: Inactive

### Branch Badge
Shows assigned branch with gradient background

### Currency Badge
Shows user's base currency in green

---

## ⚙️ Technical Details

### Backend Changes
- Added `BranchRepository` to `UserController`
- New REST endpoint: `GET /users/api/all`
- New update endpoint: `PUT /admin/users/{id}`
- Returns DTO with nested branch information

### Frontend Features
- Dynamic filtering without page reload
- Real-time search
- Modal-based editing
- Auto-populate currency based on branch
- Responsive design

---

## 🔒 Security

### Access Control
- Requires ADMIN or SUPER_ADMIN role
- All endpoints protected by Spring Security
- Audit logging for all changes

### Audit Trail
Every user update is logged:
```
[timestamp] admin updated user john.doe with branch assignment
```

---

## 📊 Statistics Bar

The dashboard shows real-time stats:
- **Total Users**: All users in system
- **Branches**: Number of active branches
- **Active Users**: Currently active users
- **Staff Members**: Users with STAFF role

---

## 🚀 Quick Start

### For Admins:

1. **First Time Setup**
   ```bash
   # Ensure migration ran successfully
   psql -d bypass_records -f data-migration.sql
   
   # Start application
   ./mvnw spring-boot:run
   ```

2. **Access Dashboard**
   ```
   http://localhost:8080/admin-users.html
   ```

3. **Assign First User**
   - Click "Edit" on any user
   - Select branch from dropdown
   - Click "Save Changes"
   - Done! ✅

---

## 🛠️ Troubleshooting

### Issue: Dashboard doesn't load
**Solution:**
- Check you're logged in as ADMIN
- Verify endpoint: `curl http://localhost:8080/users/api/all`
- Check browser console for errors

### Issue: Branch dropdown is empty
**Solution:**
- Ensure branches exist: `SELECT * FROM branch;`
- Should have at least 3 default branches
- Run migration if missing

### Issue: Can't save changes
**Solution:**
- Check all required fields filled
- Verify branch selection
- Check browser network tab for API errors

---

## 📈 Future Enhancements

Planned features:
- [ ] Bulk assign multiple users to same branch
- [ ] Export user list with branch assignments
- [ ] Branch transfer history
- [ ] Email notification on branch assignment
- [ ] Permission matrix per branch

---

## 📞 Support

For issues or questions:
- Check this guide first
- Review API endpoints in Swagger (if enabled)
- Check application logs
- Contact system administrator

---

**Last Updated:** March 20, 2026  
**Version:** 1.0.0  
**Status:** ✅ Production Ready
