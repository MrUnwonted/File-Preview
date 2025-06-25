import { Component } from '@angular/core';
import { FileUploadComponent } from "./file-upload/file-upload.component";
import { FilePreviewComponent } from "./file-preview/file-preview.component";
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, // Add this
    FileUploadComponent,
    FilePreviewComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'file-manager-app';
  previewFileName = '';
  previewError = false;

  onFileUploaded(fileName: string) {
    this.previewFileName = fileName;
    this.previewError = false;
  }

  onUploadError() {
    this.previewError = true;
  }
}
