import { Component, signal, inject, computed, /*computed*/ } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, combineLatest, of, switchMap, tap } from 'rxjs';
import { Todo } from './todo';
import { TodoService } from './todo.service';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { TodoCardComponent } from './todo-card.component';

import { MatRadioModule } from '@angular/material/radio';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCardModule } from '@angular/material/card';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-todo-list-component',
  templateUrl: 'todo-list.component.html',
  styleUrls: ['./todo-list.component.scss'],
  providers: [],
  standalone: true,
  imports: [
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    MatSelectModule,
    MatOptionModule,
    MatRadioModule,
    TodoCardComponent,
    MatListModule,
    RouterLink,
    MatButtonModule,
    MatTooltipModule,
    MatIconModule,
  ],
})
export class TodoListComponent {
  private todoService = inject(TodoService);
  private snackBar = inject(MatSnackBar);

  //will need to update with rest of characteristics
  todoOwner = signal<string | undefined>(undefined);
  todoStatus = signal<number | undefined>(undefined);
  todoCategory = signal<string | undefined>(undefined);
  todoBody = signal<string | undefined>(undefined);

  viewType = signal<'card' | 'list'>('card');

  errMsg = signal<string | undefined>(undefined);

  // We are doing status and owner filtering server side so these are observables
  private todoOwner$ = toObservable(this.todoOwner);
  private todoStatus$ = toObservable(this.todoStatus);

  serverFilteredTodos =
    // This `combineLatest` call takes the most recent values from these two observables (both built from
    // signals as described above) and passes them into the following `.pipe()` call. If either of the
    // `userRole` or `userAge` signals change (because their text fields get updated), then that will trigger
    // the corresponding `userRole$` and/or `userAge$` observables to change, which will cause `combineLatest()`
    // to send a new pair down the pipe.
    toSignal(
      combineLatest([this.todoOwner$, this.todoStatus$]).pipe(

      switchMap(([owner]) =>
        this.todoService.getTodos({
          owner,
          //status,
        })
      ),
        // `catchError` is used to handle errors that might occur in the pipeline. In this case `userService.getUsers()`
        // can return errors if, for example, the server is down or returns an error. This catches those errors, and
        // sets the `errMsg` signal, which allows error messages to be displayed.
        catchError((err) => {
          if (err.error instanceof ErrorEvent) {
            this.errMsg.set(
              `Problem in the client – Error: ${err.error.message}`
            );
          } else {
            this.errMsg.set(
              `Problem contacting the server – Error Code: ${err.status}\nMessage: ${err.message}`
            );
          }
          this.snackBar.open(this.errMsg(), 'OK', { duration: 6000 });
          // `catchError` needs to return the same type. `of` makes an observable of the same type, and makes the array still empty
          return of<Todo[]>([]);
        }),
        // Tap allows you to perform side effects if necessary
        tap(() => {
          // A common side effect is printing to the console.
          // You don't want to leave code like this in the
          // production system, but it can be useful in debugging.
          // console.log('Users were filtered on the server')
        })
      )
    );
  filteredTodos = computed(() => {
    const serverFilteredTodos = this.serverFilteredTodos();
    return this.todoService.filterTodos(serverFilteredTodos, {
      owner: this.todoOwner(),
    });
  });
}
