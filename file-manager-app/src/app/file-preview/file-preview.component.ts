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
  currentPage = 1;
  totalPages = 1;
  private loadedPageCount = false;


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

    // First get page count if we haven't already
    if (this.totalPages === 1 && this.isMultiPage) {
      this.fileService.getPdfPageCount(this.fileName).subscribe({
        next: (count) => {
          this.totalPages = count;
          this.loadPagePreview();
        },
        error: (err) => this.handleImageError()
      });
    } else {
      this.loadPagePreview();
    }
  }

  private loadPagePreview() {
    this.fileService.getMultiPagePreview(this.fileName, this.currentPage).subscribe({
      next: (blob) => {
        const reader = new FileReader();
        reader.onload = (e) => {
          const result = e.target?.result as string;
          if (result.startsWith('data:image/')) {
            this.previewUrl = result;
            this.isLoading = false;
          } else {
            this.handleImageError();
          }
        };
        reader.readAsDataURL(blob);
      },
      error: (err) => this.handleImageError()
    });
  }

  changePage(delta: number) {
    const newPage = this.currentPage + delta;
    if (newPage >= 1 && newPage <= this.totalPages) {
      this.currentPage = newPage;
      this.loadPagePreview();
    }
  }


  // Add this to handle PDF previews better
  private loadRegularPreview() {
    this.resetPreview();
    this.previewUrl = this.fileService.getPreviewUrl(this.fileName);

    // Add cache busting to prevent browser caching issues
    this.previewUrl += `?t=${new Date().getTime()}`;

    // Test if the URL returns an image
    const testImage = new Image();
    testImage.onload = () => {
      if (testImage.width > 0 && testImage.height > 0) {
        this.handleImageLoad();
      } else {
        this.handleImageError(); // Not a valid image
      }
    };
    testImage.onerror = () => {
      console.error('Preview load failed, trying fallback...');
      this.handleImageError();
      // Optional: Try a different preview method here
    };
    testImage.src = this.previewUrl;
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
    const img = new Image();
    img.onload = () => {
      this.previewHeight = img.height;
      this.isLoading = false;
    };
    img.src = this.previewUrl;
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