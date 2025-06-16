import { Component, Input } from '@angular/core';
import { FileService } from '../FileService';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-file-preview',
  standalone: true,
  imports: [CommonModule], // Only need CommonModule for ngClass
  templateUrl: './file-preview.component.html',
  styleUrl: './file-preview.component.css'
})
export class FilePreviewComponent {
  @Input() fileName: string = '';
  previewUrl: string = '';
  showFullPreview = false;

  constructor(private fileService: FileService) { }

  ngOnInit() {
    this.previewUrl = this.fileService.getThumbnailUrl(this.fileName);
  }

  togglePreview() {
    this.showFullPreview = !this.showFullPreview;
    if (this.showFullPreview) {
      this.previewUrl = this.fileService.getPreviewUrl(this.fileName);
    } else {
      this.previewUrl = this.fileService.getThumbnailUrl(this.fileName);
    }
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
}
