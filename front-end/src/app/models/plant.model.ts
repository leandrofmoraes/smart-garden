export interface PlantCareRange {
  min: number;
  max: number;
}

export interface PlantCare {
  lightLux?: PlantCareRange;
  temperature?: PlantCareRange;
  envHumidity?: PlantCareRange;
  soilMoisture?: PlantCareRange;
}

export interface Plant {
  id?: string;
  name: string;
  scientificName?: string;
  imageUrl?: string;
  care?: PlantCare;
}
