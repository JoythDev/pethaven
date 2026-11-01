import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TagModule } from 'primeng/tag';

/**
 * Placeholder reutilizable para módulos que aún no existen.
 * Se configura desde los `data` de la ruta: { title, description }.
 */
@Component({
  selector: 'app-coming-soon',
  imports: [TagModule],
  templateUrl: './coming-soon.component.html',
})
export class ComingSoonComponent {
  private readonly route = inject(ActivatedRoute);

  // Lectura única de los datos de la ruta; "||" conserva el respaldo cuando el valor falta o está vacío.
  readonly title = signal((this.route.snapshot.data['title'] as string) || 'Módulo');
  readonly description = signal(
    (this.route.snapshot.data['description'] as string) ||
      'Estamos trabajando en este módulo. ¡Muy pronto estará disponible!',
  );
}
