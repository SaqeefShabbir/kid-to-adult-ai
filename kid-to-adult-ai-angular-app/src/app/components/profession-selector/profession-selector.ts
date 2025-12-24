import { Component, EventEmitter, Output, Input, OnInit } from '@angular/core';
import { ImageService, Profession } from '../../services/image.service';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatFormField, MatSelectModule } from '@angular/material/select';
import { MatTooltip } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { MatDividerModule } from '@angular/material/divider';
import { MatInputModule } from '@angular/material/input';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  selector: 'app-profession-selector',
    imports: [CommonModule, MatCardModule, MatIconModule, MatSelectModule, MatTooltip, MatDividerModule, FormsModule, MatFormFieldModule, MatInputModule, MatFormField],
  templateUrl: './profession-selector.html',
  styleUrl: './profession-selector.scss',
})
export class ProfessionSelector implements OnInit {
  @Output() professionSelected = new EventEmitter<string>();
  @Output() ageChanged = new EventEmitter<number>();
  @Input() selectedProfession: string = '';
  @Input() targetAge: number = 30;
  
  professions: Profession[] = [];
  filteredProfessions: Profession[] = [];
  searchQuery: string = '';
  selectedProfessionData: Profession | null = null;
  ageOptions = [
    { value: 25, label: '25 years' },
    { value: 30, label: '30 years' },
    { value: 35, label: '35 years' },
    { value: 40, label: '40 years' },
    { value: 45, label: '45 years' },
    { value: 50, label: '50 years' }
  ];

  constructor(private imageService: ImageService) {}

  ngOnInit(): void {
    this.loadProfessions();
  }

  loadProfessions(): void {
    this.imageService.getProfessions().subscribe({
      next: (professions) => {
        this.professions = professions;
        this.filteredProfessions = professions;
        
        // Set initial selection
        if (this.selectedProfession) {
          this.selectedProfessionData = professions.find(p => p.id === this.selectedProfession) || null;
        } else if (professions.length > 0) {
          this.selectedProfession = professions[0].id;
          this.selectedProfessionData = professions[0];
          this.professionSelected.emit(this.selectedProfession);
        }
      },
      error: (error) => {
        console.error('Failed to load professions:', error);
      }
    });
  }

  selectProfession(professionId: string): void {
    this.selectedProfession = professionId;
    this.selectedProfessionData = this.professions.find(p => p.id === professionId) || null;
    this.professionSelected.emit(professionId);
  }

  onAgeChange(age: number): void {
    this.targetAge = age;
    this.ageChanged.emit(age);
  }

  onSearchChange(query: string): void {
    this.searchQuery = query;
    if (!query.trim()) {
      this.filteredProfessions = this.professions;
      return;
    }
    
    const searchTerm = query.toLowerCase();
    this.filteredProfessions = this.professions.filter(profession =>
      profession.name.toLowerCase().includes(searchTerm) ||
      profession.description.toLowerCase().includes(searchTerm)
    );
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.filteredProfessions = this.professions;
  }

  getProfessionIcon(professionId: string): string {
    const profession = this.professions.find(p => p.id === professionId);
    return profession ? profession.icon : 'work';
  }

  getProfessionDescription(professionId: string): string {
    const profession = this.professions.find(p => p.id === professionId);
    return profession ? profession.description : '';
  }
}
