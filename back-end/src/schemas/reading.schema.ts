import { Schema, Prop, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type ReadingDocument = Reading & Document;

@Schema({ timestamps: false })
export class Reading {
  @Prop({ required: true })
  humidity: number;

  // timestamp armazenado como Date
  @Prop({ required: true, type: Date, index: true })
  timestamp: Date;

  // campos adicionais da rega (opcionais)
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
}

export const ReadingSchema = SchemaFactory.createForClass(Reading);
