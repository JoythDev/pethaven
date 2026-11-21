import { DestroyRef, Signal, effect, inject, signal, untracked } from '@angular/core';

// El esqueleto solo aparece si la carga supera 150 ms y, una vez visible, dura al menos 300 ms.
export const SKELETON_DELAY_MS = 150;
export const SKELETON_MIN_MS = 300;

export interface SkeletonVisibility {
  visible: boolean;
  // Milisegundos hasta el próximo cambio de estado; null si no hay ninguno programado.
  recheckIn: number | null;
}

/**
 * Visibilidad del esqueleto en el instante `now`. `finish` es null mientras la carga sigue.
 * Una carga que termina en 150 ms o antes nunca lo muestra; si no, aparece en `start + 150` y se
 * oculta al terminar la carga o a los 300 ms de haber aparecido, lo que ocurra más tarde.
 */
export function skeletonVisibility(start: number, finish: number | null, now: number): SkeletonVisibility {
  const shownAt = start + SKELETON_DELAY_MS;
  if (finish !== null && finish <= shownAt) {
    return { visible: false, recheckIn: null };
  }
  if (now < shownAt) {
    return { visible: false, recheckIn: shownAt - now };
  }
  if (finish === null) {
    return { visible: true, recheckIn: null };
  }
  const hideAt = Math.max(finish, shownAt + SKELETON_MIN_MS);
  return now < hideAt ? { visible: true, recheckIn: hideAt - now } : { visible: false, recheckIn: null };
}

/**
 * Convierte el estado de carga de una página en «mostrar esqueleto» con la regla de 150/300 ms.
 * Se llama en un contexto de inyección; los temporizadores se limpian al destruirse el contexto.
 */
export function skeletonVisible(loading: Signal<boolean>): Signal<boolean> {
  const visible = signal(false);
  let start: number | null = null;
  let finish: number | null = null;
  let timer: ReturnType<typeof setTimeout> | undefined;

  const evaluate = (): void => {
    clearTimeout(timer);
    timer = undefined;
    if (start === null) {
      visible.set(false);
      return;
    }
    const state = skeletonVisibility(start, finish, Date.now());
    visible.set(state.visible);
    if (state.recheckIn !== null) {
      timer = setTimeout(evaluate, state.recheckIn);
    }
  };

  effect(() => {
    const isLoading = loading();
    untracked(() => {
      if (isLoading) {
        // Si una recarga llega con el esqueleto aún visible, se conserva en vez de parpadear.
        if (start === null || !visible()) {
          start = Date.now();
        }
        finish = null;
      } else if (start !== null && finish === null) {
        finish = Date.now();
      }
      evaluate();
    });
  });

  inject(DestroyRef).onDestroy(() => clearTimeout(timer));
  return visible.asReadonly();
}
