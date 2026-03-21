-- Data migration script to assign existing users and wallets to default branches
-- Run this AFTER V4__add_branch_and_exchange_rate.sql has been executed

-- Assign all existing users to Zimbabwe branch (ID=1) as default
UPDATE users 
SET branch_id = 1 
WHERE branch_id IS NULL;

-- Set base_currency for users based on their branch's currency
UPDATE users u
SET base_currency = (
    SELECT b.currency 
    FROM branch b 
    WHERE b.id = u.branch_id
)
WHERE base_currency IS NULL;

-- Assign all existing wallets/accounts to the same branch as their owner
UPDATE account a
SET branch_id = (
    SELECT u.branch_id 
    FROM users u 
    WHERE u.id = a.owner_id
)
WHERE branch_id IS NULL;

-- Verify the migration
SELECT 'Users assigned to branches:' as info, COUNT(*) as count, b.name as branch_name
FROM users u
JOIN branch b ON u.branch_id = b.id
GROUP BY b.name;

SELECT 'Wallets assigned to branches:' as info, COUNT(*) as count, b.name as branch_name
FROM account a
JOIN branch b ON a.branch_id = b.id
GROUP BY b.name;

SELECT 'User base currencies:' as info, COUNT(*) as count, u.base_currency
FROM users u
GROUP BY u.base_currency;
