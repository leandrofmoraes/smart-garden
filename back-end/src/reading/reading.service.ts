import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Reading, ReadingDocument } from 'src/schemas/reading.schema';
import { CreateReadingDto } from './dto/CreatReading.dto';

@Injectable()
export class ReadingService {
  constructor(
    @InjectModel(Reading.name) private readingModel: Model<ReadingDocument>,
  ) { }

  async create(createreadingDto: CreateReadingDto): Promise<Reading> {
    const createdReading = new this.readingModel(createreadingDto);
    return createdReading.save();
  }
  async findAll(): Promise<Reading[]> {
    return this.readingModel.find().exec();
  }

  async findOne(id: number): Promise<Reading | null> {
    return this.readingModel.findById(id).exec();
  }

  async findByDateRange(start: Date, end: Date): Promise<Reading[]> {
    return this.readingModel
      .find({
        timestamp: {
          $gte: start,
          $lte: end,
        },
      })
      .exec();
  }

  async delete(id: number): Promise<Reading | null> {
    return this.readingModel.findByIdAndDelete(id).exec();
  }
}
