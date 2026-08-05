import { HttpErrorResponse, HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AppStateService } from '../services/app-state.service';
import { AuthService } from '../services/auth.service';
import { TokenRefreshStateService } from '../services/token-refresh-state.service';
import { TokenStorageService } from '../services/token-storage.service';

const AUTH_ENDPOINT_SEGMENT = '/api/v1/auth/';
/**
 * Endpoints under /api/v1/auth/ that DO require (and can benefit from
 * refreshing) a valid access token -- everything else in that segment
 * (login/register/refresh-token/mfa/oauth2/forgot-password/reset-password/
 * resend-verification/verify-email) is anonymous or its own credential
 * exchange, where a 401 means "wrong input," not "your access token
 * expired," so retrying after a refresh would never help. Excluding the
 * whole /auth/ segment (the previous check) meant a stale token on /me,
 * /change-password, or /logout skipped the refresh entirely instead of
 * silently recovering.
 */
const AUTHENTICATED_AUTH_PATHS = ['/me', '/change-password', '/logout'];

/**
 * Global error handling: 401 triggers a single-flight token refresh + retry
 * (see TokenRefreshStateService), 403/network/5xx surface a toast via the
 * existing AppStateService.showToast -- no new toast/snackbar system introduced.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const appState = inject(AppStateService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      if (error.status === 401) {
        const isAnonymousAuthEndpoint = req.url.includes(AUTH_ENDPOINT_SEGMENT)
          && !AUTHENTICATED_AUTH_PATHS.some((path) => req.url.endsWith(path));
        if (isAnonymousAuthEndpoint) {
          // A 401 straight from an anonymous/credential-exchange auth endpoint
          // (bad credentials, expired/invalid refresh token) means refreshing
          // won't help.
          return throwError(() => error);
        }
        return handleUnauthorized(req, next, error);
      }

      if (error.status === 403) {
        appState.showToast("You don't have permission to do that.");
      } else if (error.status === 0) {
        appState.showToast('Network error — check your connection.');
      } else if (error.status >= 500) {
        appState.showToast('Something went wrong on our end. Please try again.');
      }

      return throwError(() => error);
    })
  );
};

function handleUnauthorized(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  originalError: HttpErrorResponse
): Observable<HttpEvent<unknown>> {
  const authService = inject(AuthService);
  const tokenStorage = inject(TokenStorageService);
  const refreshState = inject(TokenRefreshStateService);
  const router = inject(Router);
  const appState = inject(AppStateService);

  const forceLogout = () => {
    authService.clearLocalSession();
    appState.logout();
    router.navigate(['/login']);
  };

  if (refreshState.isRefreshing) {
    return refreshState.refreshedToken$.pipe(
      filter((token) => token !== undefined),
      take(1),
      switchMap((token) => {
        if (!token) {
          return throwError(() => originalError);
        }
        return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
      })
    );
  }

  refreshState.isRefreshing = true;

  return authService.refreshToken().pipe(
    switchMap((res) => {
      refreshState.isRefreshing = false;
      refreshState.refreshedToken$.next(res.data.accessToken);
      return next(req.clone({ setHeaders: { Authorization: `Bearer ${res.data.accessToken}` } }));
    }),
    catchError(() => {
      refreshState.isRefreshing = false;
      refreshState.refreshedToken$.next(null);
      forceLogout();
      return throwError(() => originalError);
    })
  );
}
