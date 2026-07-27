import { Injectable } from '@angular/core';
import { UserResponse } from '../models/auth.model';

const ACCESS_TOKEN_KEY = 'cadence_access_token';
const REFRESH_TOKEN_KEY = 'cadence_refresh_token';
const USER_KEY = 'cadence_user';

/**
 * localStorage, not an httpOnly cookie -- auth-service issues tokens in the
 * JSON response body (not a Set-Cookie header), so this is the option that
 * works without backend changes. That means the access token is readable by
 * any script on the page (XSS risk) -- acceptable for this build since the
 * backend already deliberately keeps access tokens short-lived (15 min, see
 * auth-service's application.yml) specifically to bound that exposure window.
 * A same-site httpOnly cookie would remove the risk entirely but requires
 * auth-service to set cookies itself, which is out of scope here.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  getUser(): UserResponse | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as UserResponse) : null;
  }

  setSession(accessToken: string, refreshToken: string, user: UserResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  setAccessToken(accessToken: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  }

  /** Used after /refresh-token, which rotates both tokens but doesn't return an updated user profile. */
  setTokenPair(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  hasSession(): boolean {
    return !!this.getAccessToken();
  }
}
