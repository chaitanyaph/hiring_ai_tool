import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'user-avatar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="avatar" [ngStyle]="avatarStyle">
      {{ initials | uppercase }}
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .avatar {
      border-radius: 50%;
      background: linear-gradient(135deg, var(--indigo) 0%, var(--indigo-deep) 100%);
      color: var(--paper-card);
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      user-select: none;
      flex-shrink: 0;
    }
  `]
})
export class UserAvatarComponent {
  @Input() initials: string = 'AR';
  @Input() size: number = 34;

  get avatarStyle() {
    return {
      width: `${this.size}px`,
      height: `${this.size}px`,
      fontSize: `${this.size * 0.38}px`
    };
  }
}
