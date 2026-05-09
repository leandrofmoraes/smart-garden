import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Plant } from '../../models/plant.model';
import { IotDevice } from '../../models/iot-device.model';
import { PlantService, CreatePlantPayload } from '../../services/plant.service';
import { IotDeviceService } from '../../services/iot-device.service';

@Component({
  selector: 'app-plant-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './plant-list.component.html',
  styleUrls: ['./plant-list.component.scss']
})
export class PlantListComponent implements OnInit {
  plants: Plant[] = [];
  loading = true;
  error = false;

  viewMode: 'grid' | 'list' = 'grid';

  // controle do formulário
  showForm = false;
  submitting = false;
  submitError = false;
  submitSuccess = false;

  // campos do formulário
  formScientificName = '';
  formName = '';
  formDeviceKey = '';

  // sensores IoT
  iotDevices: IotDevice[] = [];
  iotLoading = false;
  useManualIp = false;

  // controle de delete
  confirmDeleteId: string | null = null;  // id do card aguardando confirmação
  deleting = false;

  constructor(
    private plantService: PlantService,
    private iotDeviceService: IotDeviceService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadPlants();
  }

  loadPlants(): void {
    this.loading = true;
    this.plantService.getAll().subscribe({
      next: (data) => {
        this.plants = data || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Erro ao buscar plantas:', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  goToDashboard(plant: Plant): void {
    this.router.navigate(['/plants', plant.id]);
  }

  openForm(): void {
    this.showForm = true;
    this.submitError = false;
    this.submitSuccess = false;
    this.formScientificName = '';
    this.formName = '';
    this.formDeviceKey = '';
    this.useManualIp = false;
    this.loadIotDevices();
  }

  closeForm(): void {
    this.showForm = false;
  }

  loadIotDevices(): void {
    this.iotLoading = true;
    this.iotDeviceService.getAll().subscribe({
      next: (data) => {
        this.iotDevices = data || [];
        this.iotLoading = false;
      },
      error: () => {
        this.iotDevices = [];
        this.iotLoading = false;
      }
    });
  }

  submitForm(): void {
    // if (!this.formScientificName.trim()) return;
    // if (!this.formName.trim()) return;

    this.submitting = true;
    this.submitError = false;

    const payload: CreatePlantPayload = {
      name: this.formName.trim() || this.formScientificName.trim(),
      scientificName: this.formScientificName.trim() || undefined,
      deviceKey: this.formDeviceKey || undefined,
    };

    this.plantService.create(payload).subscribe({
      next: (newPlant) => {
        this.submitting = false;
        this.submitSuccess = true;
        this.plants.push(newPlant);
        setTimeout(() => this.closeForm(), 1500);
      },
      error: (err) => {
        console.error('Erro ao cadastrar planta:', err);
        this.submitting = false;
        this.submitError = true;
      }
    });
  }

  requestDelete(event: Event, plant: Plant): void {
    event.stopPropagation(); // evita navegar para o dashboard
    this.confirmDeleteId = plant.id ?? null;
  }

  cancelDelete(event: Event): void {
    event.stopPropagation();
    this.confirmDeleteId = null;
  }

  confirmDelete(event: Event, plant: Plant): void {
    event.stopPropagation();
    if (!plant.id) return;

    this.deleting = true;
    this.plantService.delete(plant.id).subscribe({
      next: () => {
        this.plants = this.plants.filter(p => p.id !== plant.id);
        this.confirmDeleteId = null;
        this.deleting = false;
      },
      error: (err) => {
        console.error('Erro ao deletar planta:', err);
        this.confirmDeleteId = null;
        this.deleting = false;
      }
    });
  }
}
