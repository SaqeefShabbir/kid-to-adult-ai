import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

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
  imageUrl?: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ImageService {
  private apiUrl = 'http://localhost:8080/api/images';

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

    return this.http.post<GenerationResponse>(`${this.apiUrl}/upload`, formData);
  }

  checkStatus(jobId: string): Observable<GenerationResponse> {
    return this.http.get<GenerationResponse>(`${this.apiUrl}/status/${jobId}`);
  }
}