import { AfterViewInit, Directive, OnDestroy } from '@angular/core';

/**
 * Paw-print cursor trail, port of the `Paw-print cursor trail` block of
 * `static/js/script.js`.
 *
 * Desktop pointing devices only, and never under prefers-reduced-motion. The
 * marks are appended to <body> rather than to the landing subtree because
 * `.paw-trail` is position: fixed and must not be clipped by an ancestor.
 */
@Directive({
  selector: '[appPawTrail]'
})
export class PawTrailDirective implements AfterViewInit, OnDestroy {
  private static readonly TRAIL_SVG =
    '<svg viewBox="0 0 96 96" width="100%" height="100%" fill="currentColor" aria-hidden="true">' +
    '<ellipse cx="48" cy="62" rx="17" ry="14"/>' +
    '<ellipse cx="24" cy="38" rx="7" ry="9" transform="rotate(-18 24 38)"/>' +
    '<ellipse cx="42" cy="27" rx="7" ry="9" transform="rotate(-5 42 27)"/>' +
    '<ellipse cx="60" cy="27" rx="7" ry="9" transform="rotate(5 60 27)"/>' +
    '<ellipse cx="77" cy="38" rx="7" ry="9" transform="rotate(18 77 38)"/>' +
    '</svg>';

  private static readonly TRAIL_MAX = 12;
  private static readonly TRAIL_MIN_DISTANCE = 55;
  private static readonly TRAIL_MIN_INTERVAL = 90;

  private lastX = 0;
  private lastY = 0;
  private lastSpawn = 0;
  private listening = false;

  private readonly onPointerMove = (event: PointerEvent): void => {
    const now = performance.now();
    const distance = Math.hypot(event.clientX - this.lastX, event.clientY - this.lastY);

    if (
      now - this.lastSpawn < PawTrailDirective.TRAIL_MIN_INTERVAL ||
      distance < PawTrailDirective.TRAIL_MIN_DISTANCE
    ) {
      return;
    }

    this.lastSpawn = now;
    this.lastX = event.clientX;
    this.lastY = event.clientY;
    this.spawn(event.clientX, event.clientY);
  };

  ngAfterViewInit(): void {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const finePointer = window.matchMedia('(pointer: fine)').matches;

    if (reducedMotion || !finePointer) {
      return;
    }

    this.listening = true;
    document.addEventListener('pointermove', this.onPointerMove);
  }

  ngOnDestroy(): void {
    if (!this.listening) {
      return;
    }

    document.removeEventListener('pointermove', this.onPointerMove);
    document.querySelectorAll('.paw-trail').forEach((paw) => paw.remove());
  }

  private spawn(x: number, y: number): void {
    if (document.querySelectorAll('.paw-trail').length >= PawTrailDirective.TRAIL_MAX) {
      return;
    }

    const paw = document.createElement('span');
    paw.className = 'paw-trail';
    paw.style.left = `${x}px`;
    paw.style.top = `${y}px`;
    paw.style.setProperty('--paw-rot', `${(Math.random() * 50 - 25).toFixed(1)}deg`);
    paw.innerHTML = PawTrailDirective.TRAIL_SVG;
    paw.addEventListener('animationend', () => paw.remove());
    document.body.appendChild(paw);
  }
}
