// Mapeado para o contrato de Dispositivo do integration-service
export interface IotDevice {
  id: string;
  name: string;
  ip?: string;          // opcional no contrato
  description?: string;
}
