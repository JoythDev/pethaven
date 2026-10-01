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

  readonly title = signal('Módulo');
  readonly description = signal('Estamos trabajando en este módulo. ¡Muy pronto estará disponible!');

  constructor() {
    const data = this.route.snapshot.data;
    if (data['title']) {
      this.title.set(data['title'] as string);
    }
    if (data['description']) {
      this.description.set(data['description'] as string);
    }
  }
}
