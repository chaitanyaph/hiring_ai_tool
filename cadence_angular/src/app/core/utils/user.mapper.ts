import { UserRole, UserModel } from '../models/models';
import { UserResponse, UserType } from '../models/auth.model';

/**
 * The existing AppStateService.currentUser signal (and everything reading it,
 * e.g. the sidebar's isCandidate split) only understands the app's own
 * UserRole.team / UserRole.candidate distinction, not the backend's 4-value
 * UserType. Anything other than CANDIDATE maps to 'team' -- ADMIN and
 * COMPANY_ADMIN both use the recruiter shell, matching recruiterGuard.
 */
export function mapUserResponseToUserModel(user: UserResponse): UserModel {
  return {
    name: user.fullName,
    email: user.email,
    role: user.userType === UserType.CANDIDATE ? UserRole.candidate : UserRole.team,
  };
}
