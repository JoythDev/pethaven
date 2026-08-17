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

  if (!element.closest("#wip-modal") && !isSectionLink) {
    element.addEventListener("click", event => {
      event.preventDefault();
      openWipModal();
    });
  }
});

// Agregar evento de cerrar modal a la X del mismo
wipClose.addEventListener("click", closeWipModal);
