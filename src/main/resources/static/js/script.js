// ===========================
// ======= Mobile Menu =======
// ===========================

const menuButton = document.getElementById("menu-btn");
const mobileMenu = document.getElementById("mobile-menu");

// Función para abrir el menú de móvil
const openMobileMenu = () => {
  mobileMenu.classList.remove("hidden");
  menuButton.setAttribute("aria-expanded", "true");
  menuButton.setAttribute("aria-label", "Cerrar menú de navegación");
  mobileMenu.setAttribute("aria-hidden", "false");
};

// Función para cerrar el menú de móvil
const closeMobileMenu = () => {
  mobileMenu.classList.add("hidden");
  menuButton.setAttribute("aria-expanded", "false");
  menuButton.setAttribute("aria-label", "Abrir menú de navegación");
  mobileMenu.setAttribute("aria-hidden", "true");
};

// Función para alternar abrir o cerrar menú de móvil
const toggleMobileMenu = () => {
  const isOpen = menuButton.getAttribute("aria-expanded") === "true";

  if (isOpen) {
    closeMobileMenu();
    return;
  }

  openMobileMenu();
};

menuButton.addEventListener("click", toggleMobileMenu);

// Cerrar el dropdown después de presionar algún enlace de navegación
mobileMenu.querySelectorAll("a").forEach((link) => {
  link.addEventListener("click", closeMobileMenu);
});

// Cerrar el dropdown después de presionar en cualquier parte de la página (fuera del menú)
document.addEventListener("click", (event) => {
  // Vertificar si se hizo click fuera del menú
  const clickedOutsideMenu =
    !mobileMenu.contains(event.target) && !menuButton.contains(event.target);

  if (
    menuButton.getAttribute("aria-expanded") === "true" &&
    clickedOutsideMenu
  ) {
    closeMobileMenu();
  }
});

// Cerrar el dropdown al presionar la tecla "Esc"
document.addEventListener("keydown", (event) => {
  if (
    event.key === "Escape" &&
    menuButton.getAttribute("aria-expanded") === "true"
  ) {
    closeMobileMenu();
    menuButton.focus();
  }
});

// ===========================
// ======== WIP Modal ========
// ===========================
const wipModal = document.getElementById("wip-modal");
const wipClose = document.getElementById("wip-close");

// Función para abrir el modal
const openWipModal = () => {
  wipModal.classList.remove("hidden");
  wipModal.classList.add("flex");
  wipModal.setAttribute("aria-hidden", "false");
  document.body.classList.add("overflow-hidden");
  wipClose.focus();
};

// Función para cerrar el modal
const closeWipModal = () => {
  wipModal.classList.add("hidden");
  wipModal.classList.remove("flex");
  wipModal.setAttribute("aria-hidden", "true");
  document.body.classList.remove("overflow-hidden");
};

// Agregar evento de abrir modal a todos los botones no funcionales
document.querySelectorAll("button, a").forEach((element) => {
  const href = element.getAttribute("href");

  // Verificar si es un enlace de sección
  const isSectionLink = href && href.startsWith("#") && href !== "#";

  if (
    element !== menuButton &&
    !element.closest("#wip-modal") &&
    !isSectionLink
  ) {
    element.addEventListener("click", (event) => {
      event.preventDefault();
      openWipModal();
    });
  }
});

// Agregar evento de cerrar modal a la X del mismo
wipClose.addEventListener("click", closeWipModal);

// ========================
// ======== Motion ========
// ========================
document.documentElement.classList.add("js");

const prefersReducedMotion = window.matchMedia(
  "(prefers-reduced-motion: reduce)",
).matches;

// Reveals al scroll
const revealElements = document.querySelectorAll(".reveal");

// Escalonado: los .reveal hijos de un contenedor [data-reveal-stagger]
// reciben delays incrementales vía --reveal-delay
document.querySelectorAll("[data-reveal-stagger]").forEach((group) => {
  group.querySelectorAll(".reveal").forEach((el, index) => {
    el.style.setProperty("--reveal-delay", `${index * 90}ms`);
  });
});

if (
  !prefersReducedMotion &&
  "IntersectionObserver" in window &&
  revealElements.length > 0
) {
  const revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          revealObserver.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.15, rootMargin: "0px 0px -40px 0px" },
  );

  revealElements.forEach((el) => revealObserver.observe(el));
} else {
  revealElements.forEach((el) => el.classList.add("is-visible"));
}

// --- Contadores animados de la trust bar ---
const counterElements = document.querySelectorAll("[data-counter]");

const animateCounter = (element) => {
  const target = parseFloat(element.dataset.counter);
  const prefix = element.dataset.prefix ?? "";
  const suffix = element.dataset.suffix ?? "";
  const duration = 1400;
  const start = performance.now();

  const format = (value) =>
    value.toLocaleString("en-US", { maximumFractionDigits: 0 });

  const step = (now) => {
    const progress = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    element.textContent = prefix + format(Math.round(target * eased)) + suffix;

    if (progress < 1) {
      requestAnimationFrame(step);
    }
  };

  requestAnimationFrame(step);
};

if (
  !prefersReducedMotion &&
  "IntersectionObserver" in window &&
  counterElements.length > 0
) {
  const counterObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          animateCounter(entry.target);
          counterObserver.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.4 },
  );

  counterElements.forEach((el) => counterObserver.observe(el));
}

// Parallax sutil de blobs
if (!prefersReducedMotion) {
  const parallaxItems = Array.from(
    document.querySelectorAll("[data-parallax]"),
  ).map((el) => ({
    el,
    factor: parseFloat(el.dataset.parallax) || 0.1,
    baseTop: 0,
    height: 0,
  }));

  if (parallaxItems.length > 0) {
    let ticking = false;

    const updateParallax = () => {
      const viewportCenter = window.scrollY + window.innerHeight / 2;

      parallaxItems.forEach(({ el, factor, baseTop, height }) => {
        const delta = viewportCenter - (baseTop + height / 2);
        const offset = Math.max(-60, Math.min(60, delta * factor * -0.5));
        el.style.transform = `translateY(${offset.toFixed(1)}px)`;
      });

      ticking = false;
    };

    const requestTick = () => {
      if (!ticking) {
        ticking = true;
        requestAnimationFrame(updateParallax);
      }
    };

    // Mide posiciones base y recalcula al redimensionar
    const measureParallax = () => {
      parallaxItems.forEach((item) => {
        item.el.style.transform = "none";
        const rect = item.el.getBoundingClientRect();
        item.baseTop = rect.top + window.scrollY;
        item.height = rect.height;
      });
      updateParallax();
    };

    window.addEventListener("resize", measureParallax);
    window.addEventListener("load", measureParallax);
    measureParallax();
    window.addEventListener("scroll", requestTick, { passive: true });
  }
}

// Video del hero
if (prefersReducedMotion) {
  document.querySelectorAll("video[autoplay]").forEach((video) => {
    video.removeAttribute("autoplay");
    video.pause();
  });
}

// Paw-print cursor trail (solo desktop)
if (!prefersReducedMotion && window.matchMedia("(pointer: fine)").matches) {
  const TRAIL_SVG =
    '<svg viewBox="0 0 96 96" width="100%" height="100%" fill="currentColor" aria-hidden="true">' +
    '<ellipse cx="48" cy="62" rx="17" ry="14"/>' +
    '<ellipse cx="24" cy="38" rx="7" ry="9" transform="rotate(-18 24 38)"/>' +
    '<ellipse cx="42" cy="27" rx="7" ry="9" transform="rotate(-5 42 27)"/>' +
    '<ellipse cx="60" cy="27" rx="7" ry="9" transform="rotate(5 60 27)"/>' +
    '<ellipse cx="77" cy="38" rx="7" ry="9" transform="rotate(18 77 38)"/>' +
    "</svg>";

  const TRAIL_MAX = 12;
  const TRAIL_MIN_DISTANCE = 55;
  const TRAIL_MIN_INTERVAL = 90;

  let lastX = 0;
  let lastY = 0;
  let lastSpawn = 0;

  const spawnPaw = (x, y) => {
    if (document.querySelectorAll(".paw-trail").length >= TRAIL_MAX) return;

    const paw = document.createElement("span");
    paw.className = "paw-trail";
    paw.style.left = `${x}px`;
    paw.style.top = `${y}px`;
    paw.style.setProperty(
      "--paw-rot",
      `${(Math.random() * 50 - 25).toFixed(1)}deg`,
    );
    paw.innerHTML = TRAIL_SVG;
    paw.addEventListener("animationend", () => paw.remove());
    document.body.appendChild(paw);
  };

  document.addEventListener("pointermove", (event) => {
    const now = performance.now();
    const distance = Math.hypot(event.clientX - lastX, event.clientY - lastY);

    if (now - lastSpawn < TRAIL_MIN_INTERVAL || distance < TRAIL_MIN_DISTANCE)
      return;

    lastSpawn = now;
    lastX = event.clientX;
    lastY = event.clientY;
    spawnPaw(event.clientX, event.clientY);
  });
}
