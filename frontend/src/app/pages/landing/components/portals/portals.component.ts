import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Portals section. Port of `templates/index.html` lines 1024-1151.
 *
 * The three buttons were Thymeleaf server navigations (`th:href="@{/login}"`
 * and `@{/vet/login}"`); here they point to the placeholder routes instead.
 */
@Component({
  selector: 'app-portals',
  imports: [RouterLink],
  templateUrl: './portals.component.html'
})
export class PortalsComponent {}
