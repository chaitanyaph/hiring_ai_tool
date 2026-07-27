import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenStorageService } from '../services/token-storage.service';

/** Attaches the access token to every outgoing request. Public auth endpoints simply ignore it. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(TokenStorageService).getAccessToken();
  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
