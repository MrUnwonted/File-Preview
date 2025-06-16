// Add Output emitter to share the uploaded filename
import { Component, EventEmitter, Output } from '@angular/core';
import { FileService } from '../FileService';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [],
  templateUrl: './file-upload.component.html',
  styleUrl: './file-upload.component.css'
})
export class FileUploadComponent {
  @Output() fileUploaded = new EventEmitter<string>();
  selectedFile: File | null = null;
  uploadProgress = 0;
  uploadComplete = false;
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

    this.fileService.uploadFile(formData).subscribe({
      next: (response) => {
        this.storedFileName = response.fileName;
        this.uploadComplete = true;
        this.fileUploaded.emit(response.fileName); // Emit the filename
      },
      error: (err) => console.error('Upload failed', err)
    });
  }

}
