import { Controller } from '@nestjs/common';
import { Get, Post, Body, Param, Delete } from '@nestjs/common';
import { HttpCode, HttpStatus } from '@nestjs/common';
import { ReadingService } from './reading.service';
import { CreateReadingDto } from './dto/CreatReading.dto';

@Controller('reading')
export class ReadingController {
  //constructor(private readingService: ReadingService) { }
  constructor(private readonly readingService: ReadingService) { }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  create(@Body() createreadingDto: CreateReadingDto) {
    console.log(createreadingDto);
    return this.readingService.create(createreadingDto);
  }

  @Get()
  @HttpCode(HttpStatus.OK)
  findAll() {
    return this.readingService.findAll();
  }

  @Get(':id')
  @HttpCode(HttpStatus.OK)
  findOne(@Param('id') id: string) {
    return this.readingService.findOne(id);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  delete(@Param('id') id: string) {
    return this.readingService.delete(id);
  }
}
