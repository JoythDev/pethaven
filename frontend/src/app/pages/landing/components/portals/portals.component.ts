import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Sección de portales de la landing: cada tarjeta lleva al login único.
@Component({
  selector: 'app-portals',
  imports: [RouterLink],
  templateUrl: './portals.component.html'
})
export class PortalsComponent {}
