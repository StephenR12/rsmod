CREATE TABLE farming_patches (
    character_id INTEGER NOT NULL,
    patch_key TEXT NOT NULL,
    patch_type TEXT NOT NULL,
    weeds INTEGER NOT NULL,
    crop_key TEXT,
    stage INTEGER NOT NULL,
    status TEXT NOT NULL,
    compost TEXT NOT NULL,
    watered INTEGER NOT NULL,
    harvest_lives INTEGER NOT NULL,
    next_growth_at INTEGER NOT NULL,
    resurrection_attempted INTEGER NOT NULL,
    health_checked INTEGER NOT NULL,
    PRIMARY KEY (character_id, patch_key),
    FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE
);

CREATE INDEX idx_farming_patches_character_id ON farming_patches(character_id);
