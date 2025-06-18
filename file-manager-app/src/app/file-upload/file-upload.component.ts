// Add Output emitter to share the uploaded filename
import { Component, EventEmitter, Output } from '@angular/core';
import { FileService } from '../FileService';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './file-upload.component.html',
  styleUrl: './file-upload.component.css'
})
export class FileUploadComponent {
  @Output() fileUploaded = new EventEmitter<string>();
  selectedFile: File | null = null;
  uploadProgress = 0;
  uploadComplete = false;
  uploadError = false;
  storedFileName: string | null = null;

  constructor(private fileService: FileService) { }

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
    this.uploadComplete = false;
  }

  uploadFile() {
    if (!this.selectedFile) return;

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    console.log('Uploading file to:', this.fileService['apiUrl']); // Check the URL

    this.fileService.uploadFile(formData).subscribe({
      next: (response) => {
        console.log('Upload successful:', response);
        this.storedFileName = response.fileName;
        this.uploadComplete = true;
        this.fileUploaded.emit(response.fileName);
      },
      error: (err) => {
        console.error('Upload failed:', err);
        console.error('Full error:', err.error);
        this.uploadError=true
      }
    });
  }
  
}
