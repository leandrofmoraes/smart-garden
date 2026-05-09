-- Remove a constraint gerada pelo Hibernate (sem ON DELETE SET NULL)
ALTER TABLE devices DROP CONSTRAINT IF EXISTS fkeb7dfbxl1eg3ibpu59k5ly1db;

-- Recria com ON DELETE SET NULL para garantir consistência no nível do banco
ALTER TABLE devices
    ADD CONSTRAINT fk_devices_plant_id
    FOREIGN KEY (plant_id) REFERENCES plants(id) ON DELETE SET NULL;
