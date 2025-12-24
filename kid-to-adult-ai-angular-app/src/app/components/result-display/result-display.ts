import { Component, EventEmitter, Output, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule, TitleCasePipe } from '@angular/common';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-result-display',
    imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatDividerModule, TitleCasePipe],
  templateUrl: './result-display.html',
  styleUrl: './result-display.scss',
})
export class ResultDisplay implements OnInit, OnChanges {
  @Output() regenerate = new EventEmitter<void>();
  @Output() download = new EventEmitter<string>();
  @Output() share = new EventEmitter<string>();
  @Output() clear = new EventEmitter<void>();
  
  @Input() originalImage: string | null = null;
  @Input() generatedImage: string | null = null;
  @Input() profession: string = '';
  @Input() age: number = 30;
  @Input() isLoading: boolean = false;
  @Input() progress: number = 0;
  @Input() status: string = '';
  
  showComparison: boolean = true;
  selectedFilter: string = 'none';
  imageFilters = [
    { id: 'none', name: 'Original', icon: 'filter_none' },
    { id: 'brightness', name: 'Bright', icon: 'wb_sunny' },
    { id: 'contrast', name: 'Contrast', icon: 'tonality' },
    { id: 'sepia', name: 'Sepia', icon: 'invert_colors' },
    { id: 'grayscale', name: 'Grayscale', icon: 'gradient' }
  ];
  
  shareOptions = [
    { platform: 'facebook', icon: 'facebook', color: '#1877F2' },
    { platform: 'twitter', icon: 'twitter', color: '#1DA1F2' },
    { platform: 'instagram', icon: 'instagram', color: '#E4405F' },
    { platform: 'whatsapp', icon: 'whatsapp', color: '#25D366' },
    { platform: 'link', icon: 'link', color: '#666' }
  ];

  constructor() {}

  ngOnInit(): void {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['generatedImage'] && this.generatedImage) {
      // Auto-show comparison when new image is generated
      this.showComparison = true;
    }
  }

  onRegenerate(): void {
    this.regenerate.emit();
  }

  onDownload(): void {
    if (this.generatedImage) {
      this.download.emit(this.generatedImage);
    }
  }

  onShare(platform: string): void {
    if (this.generatedImage) {
      this.share.emit(platform);
    }
  }

  onClear(): void {
    this.clear.emit();
  }

  toggleComparison(): void {
    this.showComparison = !this.showComparison;
  }

  applyFilter(filterId: string): void {
    this.selectedFilter = filterId;
  }

  getFilterClass(): string {
    return `filter-${this.selectedFilter}`;
  }

  getProgressText(): string {
    if (this.progress < 25) return 'Processing image...';
    if (this.progress < 50) return 'Generating face features...';
    if (this.progress < 75) return 'Applying profession attributes...';
    if (this.progress < 100) return 'Finalizing image...';
    return 'Complete!';
  }

  copyImageUrl(): void {
    if (this.generatedImage) {
      navigator.clipboard.writeText(this.generatedImage).then(() => {
        // Show copied notification
        alert('Image URL copied to clipboard!');
      });
    }
  }

  getShareUrl(): string {
    // In production, this would be a permanent URL
    return window.location.href;
  }
}
