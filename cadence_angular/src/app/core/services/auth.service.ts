import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  AuthResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  LogoutRequest,
  MfaVerifyRequest,
  RefreshTokenRequest,
  RegisterRequest,
  ResendVerificationRequest,
  ResetPasswordRequest,
  TokenResponse,
  UserResponse,
} from '../models/auth.model';
import { TokenStorageService } from './token-storage.service';

/** Every call here goes through the API Gateway (environment.apiBaseUrl), never directly to auth-service. */
const BASE_URL = `${environment.apiBaseUrl}/auth/api/v1/auth`;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokenStorage = inject(TokenStorageService);

  register(request: RegisterRequest): Observable<ApiResponse<UserResponse>> {
    return this.http.post<ApiResponse<UserResponse>>(`${BASE_URL}/register`, request);
  }

  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${BASE_URL}/login`, request).pipe(
      tap((res) => this.persistIfComplete(res.data))
    );
  }

  verifyMfaLogin(request: MfaVerifyRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${BASE_URL}/mfa/verify-login`, request).pipe(
      tap((res) => this.persistIfComplete(res.data))
    );
  }

  /** Redeems the one-time code from the /oauth2/callback redirect for the actual session. */
  exchangeOAuthCode(code: string): Observable<ApiResponse<AuthResponse>> {
    return this.http.get<ApiResponse<AuthResponse>>(`${BASE_URL}/oauth2/exchange`, { params: { code } }).pipe(
      tap((res) => this.persistIfComplete(res.data))
    );
  }

  /**
   * Full-page navigation (not an XHR) -- Google's consent screen can only redirect the whole
   * browser. Deliberately NOT built from BASE_URL: Spring Security's OAuth2 authorization-redirect
   * filter listens at the servlet root ("/oauth2/authorization/google"), not under auth-service's
   * own "/api/v1/auth" controller prefix, so only the Gateway's "/auth" route-prefix applies here.
   */
  startGoogleLogin(): void {
    window.location.href = `${environment.apiBaseUrl}/auth/oauth2/authorization/google`;
  }

  /** Rotation: backend issues a brand new access+refresh pair on every call, the old refresh token is invalidated. */
  refreshToken(): Observable<ApiResponse<TokenResponse>> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    const request: RefreshTokenRequest = { refreshToken: refreshToken ?? '' };
    return this.http.post<ApiResponse<TokenResponse>>(`${BASE_URL}/refresh-token`, request).pipe(
      tap((res) => this.tokenStorage.setTokenPair(res.data.accessToken, res.data.refreshToken))
    );
  }

  logout(allDevices = false): Observable<ApiResponse<void>> {
    const request: LogoutRequest = { refreshToken: this.tokenStorage.getRefreshToken() ?? '', allDevices };
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/logout`, request).pipe(
      tap(() => this.tokenStorage.clear())
    );
  }

  /** Clears the local session without waiting on the network -- used when the server call itself is what's failing (e.g. expired refresh token). */
  clearLocalSession(): void {
    this.tokenStorage.clear();
  }

  forgotPassword(email: string): Observable<ApiResponse<void>> {
    const request: ForgotPasswordRequest = { email };
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/forgot-password`, request);
  }

  resetPassword(token: string, newPassword: string): Observable<ApiResponse<void>> {
    const request: ResetPasswordRequest = { token, newPassword };
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/reset-password`, request);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<ApiResponse<void>> {
    const request: ChangePasswordRequest = { currentPassword, newPassword };
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/change-password`, request);
  }

  resendVerification(email: string): Observable<ApiResponse<void>> {
    const request: ResendVerificationRequest = { email };
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/resend-verification`, request);
  }

  verifyEmail(token: string): Observable<ApiResponse<void>> {
    return this.http.get<ApiResponse<void>>(`${BASE_URL}/verify-email`, { params: { token } });
  }

  getCurrentUser(): Observable<ApiResponse<UserResponse>> {
    return this.http.get<ApiResponse<UserResponse>>(`${BASE_URL}/me`);
  }

  private persistIfComplete(auth: AuthResponse): void {
    if (!auth.mfaRequired && auth.tokens && auth.user) {
      this.tokenStorage.setSession(auth.tokens.accessToken, auth.tokens.refreshToken, auth.user);
    }
  }
}
