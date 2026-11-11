// Saludo según la hora del día; añade el nombre del usuario cuando lo hay.
export function greetingFor(name?: string | null, now: Date = new Date()): string {
  const hour = now.getHours();
  const salutation = hour < 12 ? 'Buenos días' : hour < 19 ? 'Buenas tardes' : 'Buenas noches';
  return name ? `${salutation}, ${name}` : salutation;
}
