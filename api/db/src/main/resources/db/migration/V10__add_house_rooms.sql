CREATE TABLE house_rooms (
    character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    cell INTEGER NOT NULL,
    room_type TEXT NOT NULL,
    rotation INTEGER NOT NULL,
    furniture BIGINT NOT NULL,
    PRIMARY KEY (character_id, cell)
);
