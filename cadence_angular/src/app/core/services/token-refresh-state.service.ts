import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/**
 * Shared, singleton coordination point for the error interceptor's 401 handling:
 * when several requests 401 at once (e.g. a dashboard firing 5 parallel calls
 * right as the access token expires), only the first should actually call
 * /refresh-token -- the rest wait on refreshedToken$ and retry with whatever
 * it emits (a new token on success, null on failure).
 */
@Injectable({ providedIn: 'root' })
export class TokenRefreshStateService {
  isRefreshing = false;
  readonly refreshedToken$ = new Subject<string | null>();
}
