-- World 1 is the standard members world. Assigning the exact value clears every
-- variant-world bit (PvP, Deadman, Seasonal, Tournament, Beta, and so on).
UPDATE worlds
SET flags = 1
WHERE id = 1;

-- Membership entitlement is stored separately from the selected world's flags.
-- Keep existing local accounts eligible to enter and use members-only content.
UPDATE accounts
SET members = 1;
