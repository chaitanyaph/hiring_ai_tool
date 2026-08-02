import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AppStateService } from './core/services/app-state.service';
import { TokenStorageService } from './core/services/token-storage.service';
import { mapUserResponseToUserModel } from './core/utils/user.mapper';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class App {
  protected readonly title = signal('cadence-angular');

  constructor(public state: AppStateService, tokenStorage: TokenStorageService) {
    // Rehydrate the signed-in user on a hard refresh/direct navigation -- without
    // this, AppStateService.currentUser stays null even with a valid stored
    // session until the next real /login call, breaking name/role-driven UI
    // (topbar greeting, sidebar workspace switcher) until then.
    const storedUser = tokenStorage.getUser();
    if (tokenStorage.hasSession() && storedUser) {
      state.currentUser.set(mapUserResponseToUserModel(storedUser));
    }
  }
}
