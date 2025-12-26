import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, Input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-image-upload',
    imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  templateUrl: './image-upload.html',
  styleUrl: './image-upload.scss',
})
export class ImageUpload {
  @Output() imageSelected = new EventEmitter<File>();
  @Output() imagePreview = new EventEmitter<string>();
  @Input() previewUrl: string | null = null;
  
  selectedFile: File | null = null;
  isDragging = false;
  maxFileSize = 5 * 1024 * 1024; // 5MB
  acceptedFormats = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

  constructor(private snackBar: MatSnackBar) {}

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.handleFile(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    
    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      this.handleFile(file);
    }
  }

  getFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    // Format with 2 decimal places for MB and GB, 0 decimals for KB
    const formattedSize = i === 0 ? bytes : parseFloat((bytes / Math.pow(k, i)).toFixed(2));
    
    return `${formattedSize} ${sizes[i]}`;
  }

  private handleFile(file: File): void {
    // Validate file type
    if (!this.acceptedFormats.includes(file.type)) {
      this.showError('Invalid file format. Please upload JPEG, PNG, or WebP images.');
      return;
    }

    // Validate file size
    if (file.size > this.maxFileSize) {
      this.showError(`File is too large. Maximum size is ${this.getFileSize(this.maxFileSize)}.`);
      return;
    }

    this.selectedFile = file;
    
    // Create preview
    const reader = new FileReader();
    reader.onload = (e: ProgressEvent<FileReader>) => {
      const result = e.target?.result as string;
      this.previewUrl = result;
      this.imagePreview.emit(result);
    };
    reader.readAsDataURL(file);
    
    // Emit the file to parent
    this.imageSelected.emit(file);
    
    this.showSuccess('Image uploaded successfully!');
  }

  removeImage(): void {
    this.selectedFile = null;
    this.previewUrl = null;
    this.imagePreview.emit('');
    this.imageSelected.emit(null!);
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: ['success-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  triggerFileInput(): void {
    const fileInput = document.getElementById('fileInput') as HTMLElement;
    if (fileInput) {
      fileInput.click();
    }
  }
}