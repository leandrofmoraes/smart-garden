export interface Reading {
  _id: string; // O ObjectId é geralmente representado como uma string
  temperature: number;
  humidity: number;
  pressure: number;
  timestamp: string; // A data e hora podem ser representadas como string
  __v: number;
}
