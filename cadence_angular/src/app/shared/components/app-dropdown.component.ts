import { Component, Input, Output, EventEmitter, ElementRef, HostListener, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dropdown',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dropdown-container" [style.width]="width" [style.height.px]="height" (click)="toggleDropdown()">
      <div class="dropdown-trigger" [ngClass]="{ 'active': isOpen() }">
        <span class="value-text" [ngClass]="{ 'hint': !value }">
          {{ selectedLabel }}
        </span>
        <svg class="chevron" viewBox="0 0 24 24" [ngClass]="{ 'rotate': isOpen() }">
          <path d="M7 10l5 5 5-5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>

      <!-- Dropdown Menu -->
      <div class="dropdown-menu" *ngIf="isOpen()" (click)="$event.stopPropagation()">
        <div 
          class="dropdown-item" 
          *ngFor="let item of items" 
          [ngClass]="{ 'selected': item === value }"
          (click)="selectItem(item)"
        >
          {{ getLabel(item) }}
        </div>
        <div class="no-items" *ngIf="items.length === 0">
          No options available
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .dropdown-container {
      position: relative;
      display: inline-flex;
      flex-direction: column;
      font-family: $font-sans;
      cursor: pointer;
      user-select: none;
    }

    .dropdown-trigger {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 100%;
      padding: 0 12px;
      background-color: var(--paper-card);
      border: 1px solid var(--line);
      border-radius: var(--radius-medium);
      @include transition-base;

      &:hover, &.active {
        border-color: var(--indigo);
      }

      .value-text {
        font-size: 13px;
        color: var(--ink);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        &.hint {
          color: var(--ink-soft);
        }
      }

      .chevron {
        width: 16px;
        height: 16px;
        color: var(--ink-soft);
        @include transition-base;

        &.rotate {
          transform: rotate(180deg);
        }
      }
    }

    .dropdown-menu {
      position: absolute;
      top: calc(100% + 6px);
      left: 0;
      width: 100%;
      max-height: 240px;
      overflow-y: auto;
      background-color: var(--paper-card);
      border: 1px solid var(--line);
      border-radius: var(--radius-large);
      box-shadow: 0 4px 18px rgba(0, 0, 0, 0.05);
      z-index: 1000;
      padding: 6px 0;
      @include custom-scrollbar;
    }

    .dropdown-item {
      padding: 10px 14px;
      font-size: 13px;
      color: var(--ink);
      @include transition-base;

      &:hover {
        background-color: var(--line-soft);
        color: var(--indigo);
      }

      &.selected {
        background-color: var(--indigo-tint);
        color: var(--indigo);
        font-weight: 600;
      }
    }

    .no-items {
      padding: 12px 14px;
      font-size: 12.5px;
      color: var(--ink-faint);
      text-align: center;
    }
  `]
})
export class AppDropdownComponent {
  @Input() items: any[] = [];
  @Input() value: any = null;
  @Input() hintText: string = 'Select option';
  @Input() itemLabelBuilder?: (item: any) => string;
  @Input() width: string = '100%';
  @Input() height: number = 40;

  @Output() onChanged = new EventEmitter<any>();

  isOpen = signal<boolean>(false);

  constructor(private elementRef: ElementRef) {}

  toggleDropdown() {
    this.isOpen.set(!this.isOpen());
  }

  selectItem(item: any) {
    this.onChanged.emit(item);
    this.isOpen.set(false);
  }

  getLabel(item: any): string {
    if (this.itemLabelBuilder) {
      return this.itemLabelBuilder(item);
    }
    return String(item);
  }

  get selectedLabel(): string {
    if (this.value === null || this.value === undefined) {
      return this.hintText;
    }
    return this.getLabel(this.value);
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent) {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }
}
