import { AfterViewInit, Directive, ElementRef, OnDestroy, inject } from '@angular/core';

/**
 * Scroll reveal, port of the Motion block of `static/js/script.js`.
 *
 * `html.js .reveal { opacity: 0 }` is what hides the elements until they are
 * revealed, so the `js` class has to reach <html> before the first paint. It is
 * added in the constructor, which runs while the landing view is being created
 * and therefore well before the browser paints.
 *
 * The original queried `document`; querying the host subtree instead keeps the
 * behaviour identical while scoping the effect to the landing, which is why
 * none of the 30 `.reveal` elements in the markup had to be touched.
 *
 * The same `prefers-reduced-motion` gate also disables the autoplaying hero
 * videos, exactly as the original did.
 */
@Directive({
  selector: '[appReveal]'
})
export class RevealDirective implements AfterViewInit, OnDestroy {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  private observer?: IntersectionObserver;

  constructor() {
    document.documentElement.classList.add('js');
  }

  ngAfterViewInit(): void {
    const host = this.host.nativeElement;

    // Stagger: the `.reveal` children of a [data-reveal-stagger] group receive
    // an incremental --reveal-delay.
    host.querySelectorAll<HTMLElement>('[data-reveal-stagger]').forEach((group) => {
      group.querySelectorAll<HTMLElement>('.reveal').forEach((element, index) => {
        element.style.setProperty('--reveal-delay', `${index * 90}ms`);
      });
    });

    const reveals = Array.from(host.querySelectorAll<HTMLElement>('.reveal'));

    if (!this.reducedMotion && 'IntersectionObserver' in window && reveals.length > 0) {
      this.observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              entry.target.classList.add('is-visible');
              this.observer?.unobserve(entry.target);
            }
          });
        },
        { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
      );

      reveals.forEach((element) => this.observer?.observe(element));
    } else {
      reveals.forEach((element) => element.classList.add('is-visible'));
    }

    if (this.reducedMotion) {
      host.querySelectorAll<HTMLVideoElement>('video[autoplay]').forEach((video) => {
        video.removeAttribute('autoplay');
        video.pause();
      });
    }
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
