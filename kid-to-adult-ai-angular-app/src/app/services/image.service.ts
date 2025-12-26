import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, interval, Observable, of } from 'rxjs';
import { catchError, map, switchMap, takeWhile } from 'rxjs/operators';

export interface Profession {
  id: string;
  name: string;
  icon: string;
  description: string;
}

export interface GenerationRequest {
  image: File;
  profession: string;
  age: number;
}

export interface GenerationResponse {
  jobId: string;
  status: string;
  message?: string;
  imageUrl?: string;
  profession?: string;
  age?: number;
  progress?: number;
  createdAt?: Date;
  completedAt?: Date;
  errorCode?: string;
  errorDetails?: string;
}

export interface JobStatus {
  jobId: string;
  status: string;
  imageUrl?: string;
  profession: string;
  targetAge: number;
  originalFilename: string;
  generatedFilename?: string;
  errorMessage?: string;
  createdAt: Date;
  startedAt?: Date;
  completedAt?: Date;
  processingTime?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ImageService {
  private apiUrl = 'http://localhost:8080/api/stable-diffusion';

  constructor(private http: HttpClient) { }

  getProfessions(): Observable<Profession[]> {
    return this.http.get<string[]>(`${this.apiUrl}/professions`).pipe(
      map(professions => professions.map(p => this.mapProfession(p)))
    );
  }

  private mapProfession(profession: string): Profession {
    const icons: { [key: string]: string } = {
      'doctor': 'medical_services',
      'engineer': 'engineering',
      'teacher': 'school',
      'astronaut': 'rocket_launch',
      'scientist': 'science',
      'artist': 'palette',
      'pilot': 'flight',
      'firefighter': 'fire_truck',
      'chef': 'restaurant',
      'athlete': 'sports'
    };

    const descriptions: { [key: string]: string } = {
      'doctor': 'Medical professional helping people',
      'engineer': 'Building innovative solutions',
      'teacher': 'Educating future generations',
      'astronaut': 'Exploring space and beyond',
      'scientist': 'Discovering new knowledge',
      'artist': 'Creating beautiful art',
      'pilot': 'Flying aircraft worldwide',
      'firefighter': 'Protecting and saving lives',
      'chef': 'Creating culinary masterpieces',
      'athlete': 'Competing at professional level'
    };

    return {
      id: profession,
      name: profession.charAt(0).toUpperCase() + profession.slice(1),
      icon: icons[profession] || 'work',
      description: descriptions[profession] || 'Professional career'
    };
  }

  uploadImage(request: GenerationRequest): Observable<GenerationResponse> {
    const formData = new FormData();
    formData.append('image', request.image);
    formData.append('profession', request.profession);
    formData.append('age', request.age.toString());
    
    return this.http.post<GenerationResponse>(
      `${this.apiUrl}/generate`, 
      formData
    ).pipe(
      map(response => {
        return response;
      })
    );
  }

  checkStatus(jobId: string): Observable<GenerationResponse> {
    return this.http.get<GenerationResponse>(
      `${this.apiUrl}/status/${jobId}`
    );
  }

  pollStatus(jobId: string, intervalMs: number = 2000): Observable<GenerationResponse> {
    return interval(intervalMs).pipe(
      switchMap(() => this.checkStatus(jobId)),
      takeWhile(response => 
        response.status === 'PROCESSING' || 
        (response.status !== 'COMPLETED' && response.status !== 'FAILED'), 
        true
      ),
      catchError(error => {
        const errorResponse: GenerationResponse = {
          jobId,
          status: 'ERROR',
          message: 'Failed to check status'
        };
        return [errorResponse];
      })
    );
  }
  
  getAllJobs(): Observable<JobStatus[]> {
    return this.http.get<JobStatus[]>(`${this.apiUrl}/jobs`);
  }
  
  getJobsByStatus(status: string): Observable<JobStatus[]> {
    return this.http.get<JobStatus[]>(`${this.apiUrl}/jobs`, {
      params: new HttpParams().set('status', status)
    });
  }
  
  getJobsByProfession(profession: string): Observable<JobStatus[]> {
    return this.http.get<JobStatus[]>(`${this.apiUrl}/jobs`, {
      params: new HttpParams().set('profession', profession)
    });
  }
  
  deleteJob(jobId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/jobs/${jobId}`);
  }
  
  getJobUsage(): Observable<any> {
    return this.http.get(`${this.apiUrl}/statistics`);
  }
  
  getAvailableModels(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/stable-diffusion/models`);
  }
  
  setModel(modelName: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/stable-diffusion/models/${modelName}`, {});
  }
  
  getProgress(): Observable<any> {
    return this.http.get(`${this.apiUrl}/stable-diffusion/progress`);
  }
}