import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage.service';

/** Blocks /recruiter/** and /candidate/** for anyone without a stored access token. */
export const authGuard: CanActivateFn = (_route, state) => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);

  if (tokenStorage.hasSession()) {
    return true;
  }

  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
