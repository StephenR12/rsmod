ALTER TABLE farming_patches ADD COLUMN protected INTEGER NOT NULL DEFAULT 0;
ALTER TABLE farming_patches ADD COLUMN scarecrow INTEGER NOT NULL DEFAULT 0;
ALTER TABLE farming_patches ADD COLUMN compost_bin_kind TEXT NOT NULL DEFAULT 'Empty';
ALTER TABLE farming_patches ADD COLUMN compost_bin_status TEXT NOT NULL DEFAULT 'Open';
ALTER TABLE farming_patches ADD COLUMN compost_bin_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE farming_patches ADD COLUMN compost_ready_at INTEGER NOT NULL DEFAULT 0;
