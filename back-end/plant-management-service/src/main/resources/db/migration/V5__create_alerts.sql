-- Alertas gerados pelo domínio (ex: solo muito seco, irrigação necessária)
CREATE TABLE alerts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plant_id    UUID REFERENCES plants(id),
    device_key  VARCHAR(100),
    type        VARCHAR(50) NOT NULL,
    message     TEXT NOT NULL,
    severity    VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    resolved    BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_alerts_plant_id   ON alerts(plant_id);
CREATE INDEX idx_alerts_created_at ON alerts(created_at DESC);
CREATE INDEX idx_alerts_resolved   ON alerts(resolved);
