import { Component } from '@angular/core';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { HeroComponent } from './components/hero/hero.component';
import { TrustBarComponent } from './components/trust-bar/trust-bar.component';
import { MarqueeComponent } from './components/marquee/marquee.component';
import { ServicesComponent } from './components/services/services.component';
import { HowItWorksComponent } from './components/how-it-works/how-it-works.component';
import { PortalsComponent } from './components/portals/portals.component';
import { AboutUsComponent } from './components/about-us/about-us.component';
import { FacilitiesComponent } from './components/facilities/facilities.component';
import { TestimonialsComponent } from './components/testimonials/testimonials.component';
import { FaqComponent } from './components/faq/faq.component';
import { FinalCtaComponent } from './components/final-cta/final-cta.component';
import { RevealDirective } from './directives/reveal/reveal.directive';
import { CountersDirective } from './directives/counters/counters.directive';
import { ParallaxDirective } from './directives/parallax/parallax.directive';
import { PawTrailDirective } from './directives/paw-trail/paw-trail.directive';

/**
 * Landing page container. Port of `templates/index.html`.
 *
 * The original set `class="bg-background text-text font-body"` on <body>; here
 * it lives on this wrapper so the rest of the app is unaffected. The header and
 * the footer belong to the landing rather than to a global layout, which keeps
 * the internal `href="#seccion"` anchors working as plain same-document links.
 *
 * Each `<section>` of the original is a component. The extraction ranges are
 * contiguous and non-overlapping, so concatenating every component template in
 * order reproduces `index.html` lines 10-1810 verbatim, apart from the declared
 * button-target changes.
 */
@Component({
  selector: 'app-landing',
  imports: [
    HeaderComponent,
    FooterComponent,
    HeroComponent,
    TrustBarComponent,
    MarqueeComponent,
    ServicesComponent,
    HowItWorksComponent,
    PortalsComponent,
    AboutUsComponent,
    FacilitiesComponent,
    TestimonialsComponent,
    FaqComponent,
    FinalCtaComponent,
    RevealDirective,
    CountersDirective,
    ParallaxDirective,
    PawTrailDirective
  ],
  templateUrl: './landing.component.html'
})
export class LandingComponent {}
