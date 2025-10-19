import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type ReadingDocument = Reading & Document;

@Schema({ timestamps: true })
export class Reading {
  @Prop({ required: true })
  humidity: number;

  @Prop({ required: true, type: Date, index: true })
  timestamp: Date;

  @Prop({ type: Boolean, default: false })
  regando?: boolean;

  @Prop({ type: Number, default: 0 })
  rega_pulsos?: number;

  @Prop({ type: Number, default: 0 })
  rega_volume_l?: number;

  @Prop({ type: Number, default: 0 })
  volume_total_l?: number;

  @Prop({ type: Number, default: 0 })
  rega_duracao_s?: number;

  @Prop({ type: Number })
  device_ts_ms?: number;

  @Prop({ type: String })
  esp_ip?: string;

  @Prop({ type: Number })
  esp_rssi?: number;

  // Campo adicional para timestamp ISO (se necessário)
  @Prop({ type: String })
  timestamp_iso?: string;

  // Campos automáticos do Mongoose
  @Prop()
  createdAt?: Date;

  @Prop()
  updatedAt?: Date;
}

export const ReadingSchema = SchemaFactory.createForClass(Reading);
