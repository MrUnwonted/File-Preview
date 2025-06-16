import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../environment";

@Injectable({
    providedIn: 'root'
})
export class FileService {
    private apiUrl = `${environment.apiUrl}/api/files`;

    constructor(private http: HttpClient) { }

    uploadFile(formData: FormData): Observable<{ fileName: string }> {
        return this.http.post<{ fileName: string }>(`${this.apiUrl}/upload`, formData);
    }

    getPreviewUrl(fileName: string): string {
        return `${this.apiUrl}/preview/${fileName}`;
    }

    getThumbnailUrl(fileName: string): string {
        return `${this.apiUrl}/thumbnail/${fileName}`;
    }

    downloadFile(fileName: string): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/download/${fileName}`, {
            responseType: 'blob'
        });
    }

    //     Upload files to /api/files/upload

    // Get previews from /api/files/preview/{filename}

    // Get thumbnails from /api/files/thumbnail/{filename}

    // Download files from /api/files/download/{filename}
}