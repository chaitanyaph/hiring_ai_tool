import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      [type]="type"
      [disabled]="disabled"
      [ngClass]="[styleClass, fullWidth ? 'full-width' : '']"
      [ngStyle]="buttonStyle"
      (click)="onClick.emit($event)"
    >
      <ng-content></ng-content>
      <span>{{ label }}</span>
    </button>
  `,
  styles: [`
    @use 'variables' as *;

    button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: var(--radius-medium);
      font-size: 13px;
      font-weight: 600;
      font-family: $font-sans;
      cursor: pointer;
      border: 1px solid transparent;
      padding: 0 16px;
      gap: 8px;
      @include transition-base;
      white-space: nowrap;

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      &.primary {
        background-color: var(--indigo);
        color: var(--paper-card);
        &:hover:not(:disabled) {
          background-color: var(--indigo-deep);
        }
      }

      &.ghost {
        background-color: var(--paper-card);
        border-color: var(--line);
        color: var(--ink-soft);
        &:hover:not(:disabled) {
          border-color: var(--ink-soft);
          color: var(--ink);
        }
      }

      &.text {
        background-color: transparent;
        color: var(--ink-soft);
        padding: 0 8px;
        &:hover:not(:disabled) {
          color: var(--indigo);
        }
      }

      &.danger {
        background-color: var(--danger);
        color: var(--paper-card);
        &:hover:not(:disabled) {
          filter: brightness(0.9);
        }
      }

      &.full-width {
        width: 100%;
      }
    }
  `]
})
export class AppButtonComponent {
  @Input() label: string = '';
  @Input() type: 'button' | 'submit' = 'button';
  @Input() styleClass: 'primary' | 'ghost' | 'text' | 'danger' = 'primary';
  @Input() height: number = 40;
  @Input() fullWidth: boolean = false;
  @Input() disabled: boolean = false;

  @Output() onClick = new EventEmitter<MouseEvent>();

  get buttonStyle() {
    return {
      height: `${this.height}px`
    };
  }
}
