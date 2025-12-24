import { Component, signal, OnInit, ChangeDetectorRef } from '@angular/core';
import { ImageService, GenerationRequest } from './services/image.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatSliderModule } from '@angular/material/slider';

import { ImageUpload } from './components/image-upload/image-upload';
import { ProfessionSelector } from './components/profession-selector/profession-selector';
import { ResultDisplay } from './components/result-display/result-display';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [
    ImageUpload,
    ProfessionSelector,
    ResultDisplay,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatToolbarModule,
    MatIconModule,
    MatSliderModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  title = 'Kid to Adult AI Transformation';

  // State variables
  selectedImage: File | null = null;
  selectedProfession: string = 'doctor';
  targetAge: number = 30;
  originalImageUrl: string | null = null;
  generatedImageUrl: string | null = null;
  isLoading: boolean = false;
  generationProgress: number = 0;
  generationStatus: string = '';
  currentJobId: string | null = null;
  resultImage: string | null = null;

  constructor(private imageService: ImageService, private snackBar: MatSnackBar, private changeDetectorRef: ChangeDetectorRef) {}

  // Event handlers for components
  onImageSelected(file: File) {
    this.selectedImage = file;
  }

  onImagePreview(url: string) {
    this.originalImageUrl = url;
  }

  onProfessionSelected(profession: string) {
    this.selectedProfession = profession;
  }

  onAgeChanged(age: number) {
    this.targetAge = age;
  }

  startGeneration() {
    if (!this.selectedImage) {
      this.showError('Please select an image first');
      return;
    }

    if (!this.selectedProfession) {
      this.showError('Please select a profession');
      return;
    }

    const request: GenerationRequest = {
      image: this.selectedImage,
      profession: this.selectedProfession,
      age: this.targetAge
    };

    this.isLoading = true;
    this.generationStatus = 'Starting generation...';

    this.imageService.uploadImage(request).subscribe({
      next: (response) => {
        this.currentJobId = response.jobId;
        this.generationStatus = response.message;
        this.pollStatus();
      },
      error: (error) => {
        this.isLoading = false;
        this.showError('Failed to start generation');
      }
    });
  }

  pollStatus() {
    if (!this.currentJobId) return;

    const interval = setInterval(() => {
      this.imageService.checkStatus(this.currentJobId!).subscribe({
        next: (response) => {
          this.generationStatus = response.message;
          
          if (response.status === 'COMPLETED' && response.imageUrl) {
            this.isLoading = false;
            this.resultImage = response.imageUrl;
            this.generationStatus = 'Generation completed!';
            clearInterval(interval);
          } else if (response.status === 'FAILED') {
            this.isLoading = false;
            this.showError('Generation failed');
            clearInterval(interval);
          }
        },
        error: (error) => {
          clearInterval(interval);
          this.isLoading = false;
          this.showError('Error checking status');
        }
      });
    }, 3000); 
  }

  onRegenerate() {
    this.startGeneration();
  }

  onDownload(imageUrl: string) {
    if (!imageUrl) {
      this.showError('No image available to download');
      return;
    }
  
    // If it's a base64 image
    if (imageUrl.startsWith('data:image')) {
      this.downloadBase64Image(imageUrl);
    } 
    // If it's a URL
    else if (imageUrl.startsWith('http')) {
      this.downloadUrlImage(imageUrl);
    } 
    // If it's a blob URL
    else if (imageUrl.startsWith('blob:')) {
      this.downloadBlobImage(imageUrl);
    }
  }
  
  private downloadBase64Image(base64Data: string) {
    try {
      // Extract the base64 content
      const link = document.createElement('a');
      link.href = base64Data;
      link.download = this.generateFileName();
      link.style.display = 'none';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      this.showSuccess('Image downloaded successfully!');
    } catch (error) {
      console.error('Download failed:', error);
      this.showError('Failed to download image');
    }
  }
  
  private downloadUrlImage(imageUrl: string) {
    // For demo/production with actual URLs
    const link = document.createElement('a');
    link.href = imageUrl;
    link.download = this.generateFileName();
    link.target = '_blank'; // Open in new tab for external URLs
    link.rel = 'noopener noreferrer';
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    this.showSuccess('Download started!');
  }
  
  private downloadBlobImage(blobUrl: string) {
    fetch(blobUrl)
      .then(response => response.blob())
      .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = this.generateFileName();
        document.body.appendChild(link);
        link.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(link);
        
        this.showSuccess('Image downloaded successfully!');
      })
      .catch(error => {
        console.error('Download failed:', error);
        this.showError('Failed to download image');
      });
  }
  
  private generateFileName(): string {
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[:]/g, '-');
    const profession = this.selectedProfession || 'adult';
    return `kid-to-${profession}-${timestamp}.jpg`;
  }

  onShare(platform: string) {
    if (!this.generatedImageUrl) {
      this.showError('No image to share');
      return;
    }
  
    const shareData = this.prepareShareData();
    
    switch (platform.toLowerCase()) {
      case 'facebook':
        this.shareOnFacebook(shareData);
        break;
      case 'twitter':
        this.shareOnTwitter(shareData);
        break;
      case 'whatsapp':
        this.shareOnWhatsApp(shareData);
        break;
      case 'instagram':
        this.shareOnInstagram(shareData);
        break;
      case 'pinterest':
        this.shareOnPinterest(shareData);
        break;
      case 'linkedin':
        this.shareOnLinkedIn(shareData);
        break;
      case 'email':
        this.shareViaEmail(shareData);
        break;
      case 'link':
        this.copyToClipboard(shareData.url);
        break;
      default:
        this.showError('Unknown platform');
    }
  }

  getProfessionName(): string {
    // Fallback: Capitalize the profession ID
    return this.capitalizeFirstLetter(this.selectedProfession || '');
  }
  
  // Helper method to capitalize first letter
  private capitalizeFirstLetter(text: string): string {
    if (!text) return '';
    return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
  }
  
  private prepareShareData() {
    const title = `See this amazing AI transformation!`;
    const text = `I transformed into a ${this.getProfessionName()} at age ${this.targetAge} using AI!`;
    const hashtags = 'AI #KidToAdult #FutureProfession #AIArt';
    const url = window.location.href;
    
    return {
      title,
      text,
      hashtags,
      url,
      profession: this.getProfessionName(),
      age: this.targetAge
    };
  }
  
  private shareOnFacebook(shareData: any) {
    const shareUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(shareData.url)}`;
    this.openShareWindow(shareUrl, 600, 400);
  }
  
  private shareOnTwitter(shareData: any) {
    const tweetText = `${shareData.text} ${shareData.hashtags}`;
    const shareUrl = `https://twitter.com/intent/tweet?text=${encodeURIComponent(tweetText)}&url=${encodeURIComponent(shareData.url)}`;
    this.openShareWindow(shareUrl, 600, 400);
  }
  
  private shareOnWhatsApp(shareData: any) {
    const message = `${shareData.text} ${shareData.url}`;
    const shareUrl = `https://wa.me/?text=${encodeURIComponent(message)}`;
    this.openShareWindow(shareUrl, 600, 700);
  }
  
  private shareOnInstagram(shareData: any) {
    // Instagram doesn't support direct web sharing
    // Guide user to download and share
    this.showInfo('To share on Instagram:\n1. Download the image\n2. Open Instagram\n3. Create a new post\n4. Upload the downloaded image');
    
    // Optional: Trigger download first
    this.onDownload(this.generatedImageUrl!);
  }
  
  private shareOnPinterest(shareData: any) {
    const shareUrl = `https://pinterest.com/pin/create/button/?url=${encodeURIComponent(shareData.url)}&description=${encodeURIComponent(shareData.text)}`;
    this.openShareWindow(shareUrl, 735, 600);
  }
  
  private shareOnLinkedIn(shareData: any) {
    const shareUrl = `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(shareData.url)}`;
    this.openShareWindow(shareUrl, 600, 400);
  }
  
  private shareViaEmail(shareData: any) {
    const subject = `Check out my AI-generated ${shareData.profession} transformation!`;
    const body = `${shareData.text}\n\nSee the result here: ${shareData.url}\n\nGenerated using AI technology.`;
    const mailtoLink = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
    window.location.href = mailtoLink;
  }
  
  private copyToClipboard(text: string) {
    navigator.clipboard.writeText(text).then(
      () => {
        this.showSuccess('Link copied to clipboard!');
      },
      (err) => {
        console.error('Failed to copy: ', err);
        this.showError('Failed to copy to clipboard');
      }
    );
  }
  
  private openShareWindow(url: string, width: number, height: number) {
    const left = (window.innerWidth - width) / 2;
    const top = (window.innerHeight - height) / 2;
    
    window.open(
      url,
      'Share',
      `width=${width},height=${height},left=${left},top=${top},toolbar=0,location=0,menubar=0,resizable=0,scrollbars=0`
    );
  }

  onClear() {
    // Reset all state variables
    this.selectedImage = null;
    this.selectedProfession = 'doctor';
    this.targetAge = 30;
    this.originalImageUrl = null;
    this.generatedImageUrl = null;
    this.isLoading = false;
    this.generationProgress = 0;
    this.generationStatus = '';
    
    // Reset any file input elements
    const fileInputs = document.querySelectorAll('input[type="file"]') as NodeListOf<HTMLInputElement>;
    fileInputs.forEach(input => {
      input.value = '';
    });
    
    // Clear any localStorage or sessionStorage if used
    localStorage.removeItem('kidToAdult_lastImage');
    localStorage.removeItem('kidToAdult_lastProfession');

    this.changeDetectorRef.detectChanges();
    
    // Show confirmation message
    this.showSuccess('All data cleared! Ready to start fresh.');
    
    // Optional: Scroll to top
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }

  // Success notification
  showSuccess(message: string, duration: number = 3000): void {
    this.snackBar.open(message, 'Close', {
      duration: duration,
      panelClass: ['success-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'bottom'  // Changed to bottom for better UX
    });
  }

  // Error notification
  showError(message: string, duration: number = 5000): void {
    this.snackBar.open(message, 'Close', {
      duration: duration,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    });
  }

  // Info notification
  showInfo(message: string, duration: number = 4000): void {
    this.snackBar.open(message, 'OK', {
      duration: duration,
      panelClass: ['info-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    });
  }

  // Warning notification
  showWarning(message: string, duration: number = 4000): void {
    this.snackBar.open(message, 'Got it', {
      duration: duration,
      panelClass: ['warning-snackbar'],
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    });
  }
}
