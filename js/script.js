// WIP modal
const wipModal = document.getElementById("wip-modal");
const wipClose = document.getElementById("wip-close");

const openWipModal = () => {
  wipModal.classList.remove("hidden");
  wipModal.classList.add("flex");
  wipModal.setAttribute("aria-hidden", "false");
  document.body.classList.add("overflow-hidden");
  wipClose.focus();
};

const closeWipModal = () => {
  wipModal.classList.add("hidden");
  wipModal.classList.remove("flex");
  wipModal.setAttribute("aria-hidden", "true");
  document.body.classList.remove("overflow-hidden");
};

document.querySelectorAll("button, a").forEach(element => {
  const href = element.getAttribute("href");
  const isSectionLink = href && href.startsWith("#") && href !== "#";

  if (!element.closest("#wip-modal") && !isSectionLink) {
    element.addEventListener("click", event => {
      event.preventDefault();
      openWipModal();
    });
  }
});

wipClose.addEventListener("click", closeWipModal);
