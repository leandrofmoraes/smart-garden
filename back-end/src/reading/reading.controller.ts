import { Controller } from '@nestjs/common';
import { Get, Post, Body, Param, Delete } from '@nestjs/common';
import { ReadingService } from './reading.service';
import { CreateReadingDto } from './dto/CreatReading.dto';

@Controller('reading')
export class ReadingController {
  constructor(private readingService: ReadingService) { }

  @Post()
  create(@Body() createreadingDto: CreateReadingDto) {
    console.log(createreadingDto);
    return this.readingService.create(createreadingDto);
  }

  @Get()
  findAll() {
    return this.readingService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: number) {
    return this.readingService.findOne(id);
  }

  @Delete(':id')
  delete(@Param('id') id: number) {
    return this.readingService.delete(id);
  }
}
