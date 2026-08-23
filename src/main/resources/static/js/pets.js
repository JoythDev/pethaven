document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('filterForm');
    
    // Guard clause: Si no estamos en la página de mascotas, no ejecutamos el script
    if (!form) return; 
  
    const searchInput = document.getElementById('searchInput');
    const filterMenuBtn = document.getElementById('filterMenuBtn');
    const filterDropdown = document.getElementById('filterDropdown');
    const radioButtons = document.querySelectorAll('input[name="status"]');
    const tbody = document.getElementById('tableBody');
  
    // 1. Mostrar/Ocultar el menú de filtros
    filterMenuBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      filterDropdown.classList.toggle('hidden');
    });
  
    // Cerrar menú al hacer clic afuera
    document.addEventListener('click', (e) => {
      if (!filterMenuBtn.contains(e.target) && !filterDropdown.contains(e.target)) {
        filterDropdown.classList.add('hidden');
      }
    });
  
    // 2. Función para actualizar la tabla
    async function fetchUpdatedTable() {
      const url = new URL(form.action, window.location.origin);
      url.search = new URLSearchParams(new FormData(form)).toString();
  
      try {
        const response = await fetch(url);
        const htmlText = await response.text();
  
        const parser = new DOMParser();
        const doc = parser.parseFromString(htmlText, 'text/html');
  
        const newTbody = doc.getElementById('tableBody');
        if (newTbody) {
          tbody.innerHTML = newTbody.innerHTML;
        }
        
        window.history.replaceState({}, '', url);
      } catch (error) {
        console.error("Error al buscar pacientes:", error);
      }
    }
  
    // 3. Debounce
    let timeout = null;
    searchInput.addEventListener('input', () => {
      clearTimeout(timeout); 
      timeout = setTimeout(() => {
        fetchUpdatedTable();
      }, 400);
    });
  
    // 4. Actualizar tabla al elegir una opción del dropdown
    radioButtons.forEach(radio => {
      radio.addEventListener('change', () => {
        fetchUpdatedTable();
        filterDropdown.classList.add('hidden');
      });
    });
  });