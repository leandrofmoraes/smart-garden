-- Parâmetros de cuidado associados a cada planta
CREATE TABLE plant_care (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plant_id        UUID NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
    light_lux_min   DOUBLE PRECISION,
    light_lux_max   DOUBLE PRECISION,
    temp_min        DOUBLE PRECISION,
    temp_max        DOUBLE PRECISION,
    env_humidity_min DOUBLE PRECISION,
    env_humidity_max DOUBLE PRECISION,
    soil_moisture_min DOUBLE PRECISION,
    soil_moisture_max DOUBLE PRECISION,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_plant_care_plant UNIQUE (plant_id)
);
