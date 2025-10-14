/* eslint-disable @typescript-eslint/no-unsafe-call */
import { IsNotEmpty, IsNumber } from 'class-validator';

export class CreateReadingDto {
  @IsNotEmpty()
  @IsNumber()
  humidity: number;

  @IsNotEmpty()
  timestamp: Date;
}
