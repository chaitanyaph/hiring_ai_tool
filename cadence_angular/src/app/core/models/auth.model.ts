/** Mirrors auth-service's UserType enum exactly (constant/authservice/UserType.java). */
export enum UserType {
  ADMIN = 'ADMIN',
  COMPANY_ADMIN = 'COMPANY_ADMIN',
  RECRUITER = 'RECRUITER',
  CANDIDATE = 'CANDIDATE',
}

/** Mirrors auth-service's LoginRequest DTO exactly. */
export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

/** Mirrors auth-service's RegisterRequest DTO exactly. */
export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  phoneNumber?: string;
  userType: UserType;
  companyId?: string;
  companyName?: string;
}

/** Mirrors auth-service's MfaVerifyRequest DTO. */
export interface MfaVerifyRequest {
  mfaSessionToken: string;
  code: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface LogoutRequest {
  refreshToken: string;
  allDevices?: boolean;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ResendVerificationRequest {
  email: string;
}

/** Mirrors auth-service's TokenResponse DTO exactly. */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

/** Mirrors auth-service's UserResponse DTO exactly. */
export interface UserResponse {
  id: string;
  fullName: string;
  email: string;
  phoneNumber: string | null;
  userType: UserType;
  status: string;
  emailVerified: boolean;
  mfaEnabled: boolean;
  authProvider: string;
  companyId: string | null;
  companyName: string | null;
  roles: string[];
  lastLoginAt: string | null;
  createdAt: string;
}

/**
 * Mirrors auth-service's AuthResponse DTO exactly. When mfaRequired is true,
 * tokens/user are null and the caller must complete /auth/mfa/verify-login
 * with mfaSessionToken + the 6-digit code before a usable token exists.
 */
export interface AuthResponse {
  mfaRequired: boolean;
  mfaSessionToken: string | null;
  tokens: TokenResponse | null;
  user: UserResponse | null;
}
