// Si la foto de una mascota no carga, muestra el ícono de respaldo. Se revisa el nombre del
// archivo para no volver a asignar el src cuando el propio respaldo también falla (evita un bucle).
export const onPhotoError = (event: Event): void => {
  const img = event.target as HTMLImageElement;
  if (!img.src.endsWith('paw-print-icon.svg')) {
    img.src = '/images/icons/paw-print-icon.svg';
  }
};
