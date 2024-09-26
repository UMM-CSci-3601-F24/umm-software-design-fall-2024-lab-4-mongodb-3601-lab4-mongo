// import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from 'src/environments/environment';
// import { Observable } from 'rxjs';
// import { environment } from '../../environments/environment';
// import { Todo } from './todo';
// import { map } from 'rxjs/operators';

@Injectable({
  providedIn: `root`
})
export class TodoService {
  readonly todoUrl: string = `${environment.apiUrl}todos`
}
