import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PawTrailDirective } from './core/directives/paw-trail.directive';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, PawTrailDirective],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'pethaven-frontend';
}
