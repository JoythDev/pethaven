import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthRole } from '../../models/auth.model';
import { AuthService } from '../../services/auth.service';

// Exige una sesión con el rol del portal. Sin sesión lleva al login de ese portal (con la ruta a la
// que volver); con otro rol, al inicio de su propio portal. Es una ayuda de navegación: la
// autorización real la aplica el API.
export function roleGuard(role: AuthRole): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const user = auth.user();

    if (!user) {
      return router.createUrlTree([auth.loginUrl(role)], { queryParams: { returnUrl: state.url } });
    }
    if (user.role !== role) {
      return router.parseUrl(auth.homeUrl(user));
    }
    return true;
  };
}

// Portal del cliente: además del rol OWNER, el :ownerId de la ruta debe ser el del propio dueño.
export const ownerPortalGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roleCheck = roleGuard('OWNER')(route, state);
  if (roleCheck !== true) {
    return roleCheck;
  }
  const user = auth.user()!;
  return route.paramMap.get('ownerId') === String(user.id) ? true : router.parseUrl(auth.homeUrl(user));
};
