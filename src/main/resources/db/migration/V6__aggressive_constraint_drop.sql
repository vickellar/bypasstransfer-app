-- Migration V6: Aggressively drop the transaction_type_check constraint
-- This migration is simplified to ensure it runs consistently across all environments
-- even if the information_schema query in V5 had issues with case-sensitivity or scope.

ALTER TABLE "transaction" DROP CONSTRAINT IF EXISTS transaction_type_check;
