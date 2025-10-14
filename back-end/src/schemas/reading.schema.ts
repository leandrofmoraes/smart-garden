import { Schema, Prop, SchemaFactory } from '@nestjs/mongoose';

@Schema()
export class Reading {
  @Prop({ required: true })
  humidity: number;

  @Prop({ required: true })
  timestamp: Date;
}

export const ReadingSchema = SchemaFactory.createForClass(Reading);
