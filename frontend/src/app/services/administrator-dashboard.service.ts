import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api/api-base-url';
import { AdministratorDashboardResponse } from '../models/administrator-dashboard.model';

@Injectable({ providedIn: 'root' })
export class AdministratorDashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_BASE_URL);

  getSummary(): Observable<AdministratorDashboardResponse> {
    return this.http.get<AdministratorDashboardResponse>(`${this.apiUrl}/administrators/dashboard`);
  }
}
