import { Component, input } from '@angular/core';
import { LucideCircleAlert, LucideInfo } from '@lucide/angular';

/**
 * Campo de formulario (10.2, 11.4): etiqueta arriba, el control proyectado y, debajo, el error (qué
 * pasa y cómo corregirlo, `role="alert"`) o la ayuda. Mientras hay error, el borde de los campos de
 * PrimeNG pasa a `danger`. El control enlaza el mensaje con `aria-describedby` a `<fieldId>-error` o
 * `<fieldId>-hint`; la etiqueta lleva `id="<fieldId>-label"` para controles sin elemento etiquetable.
 */
@Component({
  selector: 'app-form-field',
  imports: [LucideCircleAlert, LucideInfo],
  template: `
    <label [id]="fieldId() + '-label'" [attr.for]="fieldId()" class="text-[13px] leading-[18px] font-bold text-ink">
      {{ label() }}
      @if (optional()) {
        <span class="font-medium text-ink-muted">(opcional)</span>
      }
    </label>
    <ng-content />
    @if (error(); as message) {
      <p
        [id]="fieldId() + '-error'"
        role="alert"
        class="flex items-start gap-1.5 text-[12.5px] leading-[18px] text-on-danger-soft"
      >
        <svg lucideCircleAlert class="mt-px shrink-0" [size]="14" aria-hidden="true"></svg>
        <span>{{ message }}</span>
      </p>
    } @else if (hint()) {
      <p
        [id]="fieldId() + '-hint'"
        [attr.role]="hintNotice() ? 'status' : null"
        class="flex items-start gap-1.5 text-[12.5px] leading-[18px]"
        [class]="hintNotice() ? 'text-ink' : 'text-ink-muted'"
      >
        @if (hintNotice()) {
          <svg lucideInfo class="mt-px shrink-0" [size]="14" aria-hidden="true"></svg>
        }
        <span>{{ hint() }}</span>
      </p>
    }
  `,
  host: {
    class:
      'flex min-w-0 flex-col gap-1.5 [&[data-invalid=true]_.p-inputtext]:border-danger [&[data-invalid=true]_.p-select]:border-danger',
    '[attr.data-invalid]': 'error() ? "true" : null',
  },
})
export class FormFieldComponent {
  /** Id del control (el mismo de su `id` o `inputId`). */
  readonly fieldId = input.required<string>();
  readonly label = input.required<string>();
  readonly optional = input(false);
  readonly error = input<string | null>(null);
  readonly hint = input<string | null>(null);
  /** La ayuda avisa de algo (por ejemplo, una imagen que no cargó), no solo orienta. */
  readonly hintNotice = input(false);
}
