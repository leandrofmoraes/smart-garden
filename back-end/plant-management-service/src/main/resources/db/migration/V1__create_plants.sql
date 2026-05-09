-- Tabela principal de plantas
CREATE TABLE plants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    scientific_name VARCHAR(255),
    image_url   TEXT,
    plantbook_pid VARCHAR(100),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_plants_scientific_name ON plants(scientific_name);
