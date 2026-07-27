import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserType } from '../models/auth.model';
import { AppStateService } from '../services/app-state.service';
import { TokenStorageService } from '../services/token-storage.service';

/** /recruiter/** is for anyone who isn't a plain CANDIDATE (ADMIN, COMPANY_ADMIN, RECRUITER all use this shell). */
export const recruiterGuard: CanActivateFn = () => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);
  const appState = inject(AppStateService);

  const user = tokenStorage.getUser();
  if (user && user.userType !== UserType.CANDIDATE) {
    return true;
  }

  appState.showToast('That workspace is for recruiters and admins only.');
  return router.createUrlTree(['/candidate/dashboard']);
};

/** /candidate/** is for CANDIDATE users only. */
export const candidateGuard: CanActivateFn = () => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);
  const appState = inject(AppStateService);

  const user = tokenStorage.getUser();
  if (user && user.userType === UserType.CANDIDATE) {
    return true;
  }

  appState.showToast('That workspace is for candidates only.');
  return router.createUrlTree(['/recruiter/dashboard']);
};
