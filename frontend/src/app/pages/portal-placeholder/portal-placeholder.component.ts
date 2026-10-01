import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Placeholder for the portal routes (`/login`, `/vet/login`) that the backend
 * still serves as Thymeleaf pages. It reuses the card style of the original
 * `wip-page` fragment, so the landing's portal buttons keep leading somewhere
 * real while the portals are migrated.
 */
@Component({
  selector: 'app-portal-placeholder',
  imports: [RouterLink],
  templateUrl: './portal-placeholder.component.html'
})
export class PortalPlaceholderComponent {}
