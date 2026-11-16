import { DailyTip } from '../../models/demo/daily-tip.model';
import { dayNumber } from './demo-date';

// Colección fija; cada consejo nombra el sticker que coincide con su tema.
export const DAILY_TIPS: readonly DailyTip[] = [
  {
    id: 'water',
    headline: 'Agua fresca, todos los días',
    text: 'Cambia el agua del bebedero una o dos veces al día y lávalo cada semana.',
    sticker: 'water-bowl',
  },
  {
    id: 'teeth',
    headline: 'Cepillado dental, una rutina corta',
    text: 'Cepilla sus dientes tres veces por semana con una pasta hecha para mascotas.',
    sticker: 'toothbrush',
  },
  {
    id: 'walk',
    headline: 'Un paseo diario hace la diferencia',
    text: 'Salir a caminar le ayuda a gastar energía, mantener el peso y despejar la mente.',
    sticker: 'leash',
  },
  {
    id: 'bath',
    headline: 'Baño cada cuatro a seis semanas',
    text: 'Usa champú para mascotas y seca muy bien entre los pliegues para cuidar su piel.',
    sticker: 'shampoo',
  },
  {
    id: 'vaccines',
    headline: 'Las vacunas, siempre al día',
    text: 'Anota la fecha del próximo refuerzo y agéndalo antes de que venza.',
    sticker: 'syringe',
  },
  {
    id: 'checkup',
    headline: 'Un control al año, aunque se vea sano',
    text: 'La revisión anual detecta a tiempo lo que todavía no da señales.',
    sticker: 'calendar',
  },
  {
    id: 'fever',
    headline: 'Fiebre: más de 39,5 °C',
    text: 'Su temperatura normal ronda los 38 a 39 °C; si pasa de 39,5, llama al veterinario.',
    sticker: 'thermometer',
  },
  {
    id: 'portions',
    headline: 'Porciones medidas, peso estable',
    text: 'Mide la comida con una taza y ajusta la ración según su edad y su actividad.',
    sticker: 'food-bowl',
  },
  {
    id: 'wounds',
    headline: 'Una herida pequeña también se limpia',
    text: 'Lávala con solución salina y consulta si enrojece, supura o no mejora en dos días.',
    sticker: 'bandage',
  },
  {
    id: 'carrier',
    headline: 'La transportadora, un lugar tranquilo',
    text: 'Déjala abierta en casa con una manta para que la asocie con algo agradable.',
    sticker: 'carrier',
  },
  {
    id: 'rest',
    headline: 'Descanso en un lugar tranquilo',
    text: 'Un perro adulto duerme entre doce y catorce horas; dale una cama lejos del ruido.',
    sticker: 'pet-bed',
  },
  {
    id: 'treats',
    headline: 'Premios, máximo el 10 % del día',
    text: 'Úsalos para reforzar buenas conductas y descuéntalos de su ración diaria.',
    sticker: 'treat',
  },
  {
    id: 'parasites',
    headline: 'Antiparasitario al día',
    text: 'Pulgas y garrapatas se previenen todo el año, no solo en temporada de calor.',
    sticker: 'shield',
  },
  {
    id: 'medication',
    headline: 'Termina el tratamiento completo',
    text: 'Aunque se vea mejor, da todas las dosis indicadas para que la mejoría se mantenga.',
    sticker: 'pill',
  },
  {
    id: 'warning-signs',
    headline: 'Señales para llamar al veterinario',
    text: 'Falta de apetito por más de un día, vómito repetido o decaimiento merecen una consulta.',
    sticker: 'stethoscope',
  },
  {
    id: 'play',
    headline: 'El cariño también cuenta',
    text: 'Dedícale diez minutos de juego tranquilo al día: baja el estrés de los dos.',
    sticker: 'heart',
  },
  {
    id: 'paws',
    headline: 'Revisa las almohadillas',
    text: 'Después de caminar, busca cortes o piedritas y mantén las uñas cortas.',
    sticker: 'paw',
  },
];

// El consejo del día: el mismo durante toda la fecha y distinto al día siguiente.
export function tipForDate(date: Date): DailyTip {
  return DAILY_TIPS[dayNumber(date) % DAILY_TIPS.length];
}
