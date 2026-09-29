import { Component } from '@angular/core';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { HeroComponent } from './components/hero/hero.component';
import { TrustBarComponent } from './components/trust-bar/trust-bar.component';

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
 * order reproduces `index.html` lines 10-1810 verbatim.
 */
@Component({
  selector: 'app-landing',
  imports: [HeaderComponent, FooterComponent, HeroComponent, TrustBarComponent],
  templateUrl: './landing.component.html'
})
export class LandingComponent {}
