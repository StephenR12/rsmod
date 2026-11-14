-- Preserve server storage when native revision 238 claims inventory IDs 936 through 952.
UPDATE inventories
SET inv_type = CASE inv_type
    WHEN 936 THEN 65000 -- hunter_fur_pouch_storage
    WHEN 937 THEN 65001 -- hunter_meat_pouch_storage
    WHEN 938 THEN 65002 -- fish_barrel_storage
    WHEN 939 THEN 65003 -- gem_bag_storage
    WHEN 940 THEN 65004 -- clue_stash_storage
    WHEN 941 THEN 65005 -- fairy_fixit_enchantments
    WHEN 942 THEN 65006 -- warrens_general_store
    WHEN 943 THEN 65007 -- motherlode_pending
    WHEN 944 THEN 65008 -- motherlode_sack_storage
    WHEN 945 THEN 65009 -- graceful_clothing
    WHEN 946 THEN 65010 -- skill_pet_menagerie_forms
    WHEN 947 THEN 65011 -- drift_net_harvest
    WHEN 948 THEN 65012 -- poh_menagerie_arena_pets
    WHEN 949 THEN 65013 -- dusuri_star_stock
    WHEN 950 THEN 65014 -- forestry_log_basket_storage
    WHEN 951 THEN 65015 -- mixology_reagent_pouch
    WHEN 952 THEN 65016 -- volcanic_mine_retrieval
    ELSE inv_type
END
WHERE inv_type BETWEEN 936 AND 952;
