import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, Observable, of } from "rxjs";
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
        return `${this.apiUrl}/preview/${encodeURIComponent(fileName)}`;
    }

    // In your FileService
    getMultiPagePreview(fileName: string, page: number = 1): Observable<Blob> {
        return this.http.get(
            `${this.apiUrl}/multipage-preview/${encodeURIComponent(fileName)}?page=${page}`,
            { responseType: 'blob' }
        );
    }

    downloadFile(fileName: string): Observable<Blob> {
        return this.http.get(`${this.apiUrl}/download/${fileName}`, {
            responseType: 'blob'
        });
    }

    getPdfPageCount(fileName: string): Observable<number> {
        return this.http.get<number>(
            `${this.apiUrl}/page-count/${encodeURIComponent(fileName)}`
        ).pipe(
            catchError((error: any) => {
                console.error('Failed to get page count', error);
                return of(1); // Default to single page on error
            })
        );
    }

}