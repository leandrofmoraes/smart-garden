import { Module } from '@nestjs/common';
import { ReadingService } from './reading.service';
import { ReadingController } from './reading.controller';
import { MongooseModule } from '@nestjs/mongoose';
import { ReadingSchema } from 'src/schemas/reading.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      {
        name: 'Reading',
        schema: ReadingSchema,
      },
    ]),
  ],
  controllers: [ReadingController],
  providers: [ReadingService],
})
export class ReadingModule {}
