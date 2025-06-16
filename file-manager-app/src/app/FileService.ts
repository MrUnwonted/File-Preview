import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

@Injectable({
    providedIn: 'root'
})
export class FileService {
    private apiUrl = '/api/files';

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
}