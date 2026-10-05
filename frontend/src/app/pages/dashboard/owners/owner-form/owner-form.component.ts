import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { Owner } from '../../../../models/owner.model';
import { OwnerService } from '../../../../services/owner.service';

@Component({
  selector: 'app-owners-form',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, MessageModule],
  templateUrl: './owner-form.component.html',
})
export class OwnersFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ownerService = inject(OwnerService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly messageService = inject(MessageService);

  // Si tiene valor, estamos editando ese dueño; si es null, creamos uno nuevo.
  editandoId: number | null = null;

  readonly form = this.formBuilder.group({
    name: ['', Validators.required],
    document: ['', Validators.required],
    phone: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editandoId = Number(idParam);
      const existente = this.ownerService.getById(this.editandoId);
      if (existente) {
        const { id, ...resto } = existente;
        this.form.patchValue(resto);
      } else {
        this.messageService.add({
          severity: 'error',
          summary: 'Dueño no encontrado',
          detail: 'El dueño que intentas editar no existe.',
        });
        this.router.navigate(['/dashboard/owners']);
      }
    }
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const owner = this.form.getRawValue() as Omit<Owner, 'id'>;

    if (this.editandoId !== null) {
      this.ownerService.update(this.editandoId, owner);
      this.messageService.add({
        severity: 'success',
        summary: 'Dueño actualizado',
        detail: `${owner.name} se actualizó correctamente.`,
      });
      this.router.navigate(['/dashboard/owners', this.editandoId]);
    } else {
      const nuevo = this.ownerService.add(owner);
      this.messageService.add({
        severity: 'success',
        summary: 'Dueño registrado',
        detail: `${nuevo.name} se registró correctamente.`,
      });
      this.router.navigate(['/dashboard/owners']);
    }
  }

  cancelar(): void {
    this.router.navigate(['/dashboard/owners']);
  }
}
