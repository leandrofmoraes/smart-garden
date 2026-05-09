-- Dispositivos IoT conhecidos
-- plant_id: vínculo explícito device → planta monitorada
CREATE TABLE devices (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_key  VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(255),
    ip          VARCHAR(50),
    description TEXT,
    plant_id    UUID REFERENCES plants(id) ON DELETE SET NULL,
    last_seen   TIMESTAMPTZ,
    online      BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_devices_device_key ON devices(device_key);
CREATE INDEX idx_devices_plant_id   ON devices(plant_id);
