import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FileService } from '../FileService';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-file-preview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './file-preview.component.html',
  styleUrls: ['./file-preview.component.css']
})
export class FilePreviewComponent implements OnChanges {
  @Input() fileName: string = '';
  previewUrl: string = '';
  isLoading = true;
  showFullPreview = false;
  isMultiPage = false;
  previewHeight = 0;
  errorOccurred = false;

  constructor(private fileService: FileService) { }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['fileName'] && this.fileName) {
      this.isMultiPage = this.isMultiPageDocument(this.fileName);
      this.resetPreview();
      this.loadPreview();
    }
  }

  resetPreview() {
    this.previewUrl = '';
    this.isLoading = true;
    this.errorOccurred = false;
  }

  private loadPreview() {
    if (this.showFullPreview) {
      this.loadFullPreview();
    }
  }

  togglePreview() {
    console.log('Toggle preview', this.showFullPreview, this.fileName);
    this.showFullPreview = !this.showFullPreview;
    this.loadPreview();
  }

  // loadThumbnail() {
  //   this.resetPreview();
  //   this.previewUrl = this.fileService.getThumbnailUrl(this.fileName);
  //   // Add proper image validation
  //   const testImage = new Image();
  //   testImage.onload = () => {
  //     console.log('Thumbnail loaded successfully', this.previewUrl);
  //     this.handleImageLoad();
  //   };
  //   testImage.onerror = () => {
  //     console.error('Thumbnail failed to load', this.previewUrl);
  //     this.handleImageError();
  //   };
  //   testImage.src = this.previewUrl;
  // }

  loadFullPreview() {
    this.resetPreview();
    if (this.isMultiPage) {
      this.fileService.getMultiPagePreview(this.fileName).subscribe({
        next: (blob) => {
          this.createImageFromBlob(blob);
        },
        error: (err) => {
          console.error('Error loading multi-page preview:', err);
          this.loadRegularPreview();
        }
      });
    } else {
      this.loadRegularPreview();
    }
  }

  loadRegularPreview() {
    this.resetPreview();
    this.previewUrl = this.fileService.getPreviewUrl(this.fileName);
    // Calculate height after load
    const img = new Image();
    img.onload = () => {
      this.handleImageLoad();
      this.previewHeight = img.height;
    };
    img.onerror = () => this.handleImageError();
    img.src = this.previewUrl;
  }

  handleImageError() {
    console.error('Failed to load preview image', {
      url: this.previewUrl,
      fileName: this.fileName,
      time: new Date().toISOString()
    });
    this.errorOccurred = true;
    this.isLoading = false;
  }

  handleImageLoad() {
    this.isLoading = false;
    this.errorOccurred = false;
  }

  createImageFromBlob(blob: Blob) {
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.previewUrl = e.target.result;
      this.isLoading = false;
    };
    reader.onerror = () => {
      this.handleImageError();
    };
    reader.readAsDataURL(blob);
  }

  download() {
    this.fileService.downloadFile(this.fileName).subscribe(blob => {
      const a = document.createElement('a');
      const objectUrl = URL.createObjectURL(blob);
      a.href = objectUrl;
      a.download = this.fileName;
      a.click();
      URL.revokeObjectURL(objectUrl);
    });
  }

  isMultiPageDocument(filename: string): boolean {
    const ext = filename.split('.').pop()?.toLowerCase();
    return ['pdf', 'docx', 'doc', 'pptx', 'ppt'].includes(ext || '');
  }
}