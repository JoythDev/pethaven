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
mobileMenu.querySelectorAll("a").forEach(link => {
  link.addEventListener("click", closeMobileMenu);
});

// Cerrar el dropdown después de presionar en cualquier parte de la página (fuera del menú)
document.addEventListener("click", event => {
  // Vertificar si se hizo click fuera del menú
  const clickedOutsideMenu = !mobileMenu.contains(event.target) && !menuButton.contains(event.target);

  if (menuButton.getAttribute("aria-expanded") === "true" && clickedOutsideMenu) {
    closeMobileMenu();
  }
});

// Cerrar el dropdown al presionar la tecla "Esc"
document.addEventListener("keydown", event => {
  if (event.key === "Escape" && menuButton.getAttribute("aria-expanded") === "true") {
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
document.querySelectorAll("button, a").forEach(element => {
  const href = element.getAttribute("href");

  // Verificar si es un enlace de sección
  const isSectionLink = href && href.startsWith("#") && href !== "#";

  if (element !== menuButton && !element.closest("#wip-modal") && !isSectionLink) {
    element.addEventListener("click", event => {
      event.preventDefault();
      openWipModal();
    });
  }
});

// Agregar evento de cerrar modal a la X del mismo
wipClose.addEventListener("click", closeWipModal);
