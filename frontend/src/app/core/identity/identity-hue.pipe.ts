import { Pipe, PipeTransform } from '@angular/core';

import { IdentityHue, identityHue } from './identity';

// Uso: [class]="'hue-' + (pet.id | identityHue)"
@Pipe({ name: 'identityHue' })
export class IdentityHuePipe implements PipeTransform {
  transform(id: number | string): IdentityHue {
    return identityHue(id);
  }
}
