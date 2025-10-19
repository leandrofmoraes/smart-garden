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
  findOne(@Param('id') id: number) {
    return this.readingService.findOne(id);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  delete(@Param('id') id: number) {
    return this.readingService.delete(id);
  }

  @Get('test')
  @HttpCode(HttpStatus.OK)
  testEndpoint() {
    return {
      message: 'API is working',
      timestamp: new Date(),
      expectedFields: [
        'humidity', 'timestamp', 'regando', 'rega_pulsos',
        'rega_volume_l', 'volume_total_l', 'rega_duracao_s',
        'device_ts_ms', 'esp_ip', 'esp_rssi'
      ]
    };
  }
}
