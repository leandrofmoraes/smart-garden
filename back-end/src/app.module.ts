import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ReadingModule } from './reading/reading.module';

@Module({
  imports: [
    MongooseModule.forRoot(process.env.MONGO_URI ?? 'mongodb://127.0.0.1:27017/jardim-inteligente-db'),
    ReadingModule,
  ],
  controllers: [],
  providers: [],
})
export class AppModule { }
