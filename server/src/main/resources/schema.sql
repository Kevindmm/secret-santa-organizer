CREATE TABLE IF NOT EXISTS game (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    max_price DECIMAL(19, 2),
    exchange_date DATE,
    assignments_done BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS participant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255),
    wish_list TEXT,
    assigned_to_email VARCHAR(255),
    email_sent BOOLEAN DEFAULT false,
    game_id VARCHAR(255),
    CONSTRAINT fk_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_participant_game ON participant(game_id);
CREATE INDEX IF NOT EXISTS idx_participant_email ON participant(email);