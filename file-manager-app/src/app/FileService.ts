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

    // getThumbnailUrl(fileName: string): string {
    //     const url = `${this.apiUrl}/thumbnail/${fileName}`;
    //     console.log('Generated thumbnail URL:', url);
    //     return url;
    // }
    getPreviewUrl(fileName: string): string {
        return `${this.apiUrl}/preview/${encodeURIComponent(fileName)}`;
    }

    getMultiPagePreview(fileName: string) {
        return this.http.get(
            `${this.apiUrl}/multipage-preview/${encodeURIComponent(fileName)}`,
            { responseType: 'blob' }
        );
    }

    downloadFile(fileName: string): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/download/${fileName}`, {
            responseType: 'blob'
        });
    }

}