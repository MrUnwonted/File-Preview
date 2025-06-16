import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { FileUploadComponent } from "./file-upload/file-upload.component";
import { FilePreviewComponent } from "./file-preview/file-preview.component";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, FileUploadComponent, FilePreviewComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'file-manager-app';
  previewFileName = '';
}
