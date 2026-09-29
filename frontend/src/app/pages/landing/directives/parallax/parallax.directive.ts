import { AfterViewInit, Directive, ElementRef, OnDestroy, inject } from '@angular/core';

interface ParallaxItem {
  element: HTMLElement;
  factor: number;
  baseTop: number;
  height: number;
}

/**
 * Subtle blob parallax, port of the `Parallax sutil de blobs` block of
 * `static/js/script.js`.
 *
 * `data-parallax` holds the factor; the offset is clamped to 60px either way and
 * positions are re-measured on load and resize, with scroll work batched in
 * requestAnimationFrame.
 */
@Directive({
  selector: '[appParallax]'
})
export class ParallaxDirective implements AfterViewInit, OnDestroy {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  private items: ParallaxItem[] = [];
  private ticking = false;

  private readonly onScroll = () => this.requestTick();
  private readonly onResize = () => this.measure();

  ngAfterViewInit(): void {
    if (this.reducedMotion) {
      return;
    }

    this.items = Array.from(
      this.host.nativeElement.querySelectorAll<HTMLElement>('[data-parallax]')
    ).map((element) => ({
      element,
      factor: parseFloat(element.dataset['parallax'] ?? '') || 0.1,
      baseTop: 0,
      height: 0
    }));

    if (this.items.length === 0) {
      return;
    }

    window.addEventListener('resize', this.onResize);
    window.addEventListener('load', this.onResize);
    window.addEventListener('scroll', this.onScroll, { passive: true });

    this.measure();
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.onResize);
    window.removeEventListener('load', this.onResize);
    window.removeEventListener('scroll', this.onScroll);
  }

  private measure(): void {
    this.items.forEach((item) => {
      item.element.style.transform = 'none';
      const rect = item.element.getBoundingClientRect();
      item.baseTop = rect.top + window.scrollY;
      item.height = rect.height;
    });

    this.update();
  }

  private requestTick(): void {
    if (this.ticking) {
      return;
    }

    this.ticking = true;
    requestAnimationFrame(() => this.update());
  }

  private update(): void {
    const viewportCenter = window.scrollY + window.innerHeight / 2;

    this.items.forEach(({ element, factor, baseTop, height }) => {
      const delta = viewportCenter - (baseTop + height / 2);
      const offset = Math.max(-60, Math.min(60, delta * factor * -0.5));
      element.style.transform = `translateY(${offset.toFixed(1)}px)`;
    });

    this.ticking = false;
  }
}
