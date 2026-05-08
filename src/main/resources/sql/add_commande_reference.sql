-- Migration: add 'reference' column to 'commande' table
-- Run this on your MySQL database if the column does not exist:

ALTER TABLE commande
  ADD COLUMN reference VARCHAR(64) DEFAULT NULL;

-- Optional: create an index to speed lookups by reference
-- CREATE UNIQUE INDEX idx_commande_reference ON commande(reference);

-- Backfill existing rows: set reference to 'REF-' + HEX(id) for rows without reference
UPDATE commande SET reference = CONCAT('REF-', HEX(id)) WHERE reference IS NULL;

-- Notes:
-- - MySQL's HEX() returns uppercase hex digits (e.g. 15 -> 'F').
-- - If you prefer a different prefix or formatting, adjust the CONCAT above.
