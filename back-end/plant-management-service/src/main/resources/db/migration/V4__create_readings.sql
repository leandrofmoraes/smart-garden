-- Leituras de sensores recebidas via MQTT
-- device_id é NULLABLE: a leitura pode chegar antes do device ser registrado
CREATE TABLE readings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     VARCHAR(100),
    device_id       UUID REFERENCES devices(id) ON DELETE SET NULL,
    device_key      VARCHAR(100) NOT NULL,
    humidity        DOUBLE PRECISION,
    regando         BOOLEAN,
    rega_pulsos     INTEGER,
    rega_volume_l   DOUBLE PRECISION,
    volume_total_l  DOUBLE PRECISION,
    rega_duracao_s  INTEGER,
    esp_ip          VARCHAR(50),
    esp_rssi        INTEGER,
    device_ts_ms    BIGINT,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_readings_device_key  ON readings(device_key);
CREATE INDEX idx_readings_device_id   ON readings(device_id);
CREATE INDEX idx_readings_read_at     ON readings(read_at DESC);
