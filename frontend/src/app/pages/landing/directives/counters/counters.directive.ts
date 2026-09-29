import { AfterViewInit, Directive, ElementRef, OnDestroy, inject } from '@angular/core';

/**
 * Animated counters of the trust bar, port of the `Contadores animados` block of
 * `static/js/script.js`.
 *
 * `data-counter` holds the target, `data-prefix` and `data-suffix` wrap it. The
 * markup already contains the final formatted value as text, so when the
 * animation does not run (reduced motion) the static value stays visible.
 */
@Directive({
  selector: '[appCounters]'
})
export class CountersDirective implements AfterViewInit, OnDestroy {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  private observer?: IntersectionObserver;

  ngAfterViewInit(): void {
    const counters = Array.from(
      this.host.nativeElement.querySelectorAll<HTMLElement>('[data-counter]')
    );

    if (this.reducedMotion || !('IntersectionObserver' in window) || counters.length === 0) {
      return;
    }

    this.observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            this.animate(entry.target as HTMLElement);
            this.observer?.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.4 }
    );

    counters.forEach((element) => this.observer?.observe(element));
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private animate(element: HTMLElement): void {
    const target = parseFloat(element.dataset['counter'] ?? '0');
    const prefix = element.dataset['prefix'] ?? '';
    const suffix = element.dataset['suffix'] ?? '';
    const duration = 1400;
    const start = performance.now();

    const format = (value: number) => value.toLocaleString('en-US', { maximumFractionDigits: 0 });

    const step = (now: number) => {
      const progress = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      element.textContent = prefix + format(Math.round(target * eased)) + suffix;

      if (progress < 1) {
        requestAnimationFrame(step);
      }
    };

    requestAnimationFrame(step);
  }
}
