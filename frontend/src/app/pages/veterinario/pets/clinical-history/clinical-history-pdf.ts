import { formatDate } from '@angular/common';
import { jsPDF } from 'jspdf';
import { evolutionLabel } from '../../../../models/treatment.model';
import type { ClinicalHistory } from './clinical-history.component';

const MARGIN = 15;
const PAGE_WIDTH = 210; // A4 en mm
const PAGE_HEIGHT = 297;
const CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;
const PT_TO_MM = 0.3528;
const PHOTO_SIZE = 35; // mm, arriba a la derecha de la primera página
const PHOTO_PX = 400;

// El patrón es numérico, así que el locale 'en-US' (siempre registrado) no cambia el resultado.
const fmt = (date: string | Date, pattern = 'dd/MM/yyyy HH:mm') => formatDate(date, pattern, 'en-US');
const signed = (n: number) => `${n > 0 ? '+' : ''}${Number(n.toFixed(2))}`;

// Genera la historia clínica como PDF A4 y la descarga. Helvetica cubre tildes y ñ,
// pero no símbolos como "→": por eso el texto usa "->".
export function buildClinicalHistoryPdf(h: ClinicalHistory, photo: string | null = null, generatedAt = new Date()): jsPDF {
  const doc = new jsPDF({ unit: 'mm', format: 'a4' });
  let y = MARGIN;

  // Mientras el texto esté a la altura de la foto, se angosta para no pasarle por encima.
  let photoBottom = 0;
  if (photo) {
    const x = PAGE_WIDTH - MARGIN - PHOTO_SIZE;
    doc.addImage(photo, 'JPEG', x, MARGIN, PHOTO_SIZE, PHOTO_SIZE);
    doc.setDrawColor(220).rect(x, MARGIN, PHOTO_SIZE, PHOTO_SIZE);
    photoBottom = MARGIN + PHOTO_SIZE + 2;
  }
  const lineWidth = () => (y < photoBottom ? CONTENT_WIDTH - PHOTO_SIZE - 5 : CONTENT_WIDTH);

  // Escribe un párrafo con salto de línea automático y salta de página si no cabe.
  const write = (value: string, size = 10, bold = false, gapAfter = 1) => {
    doc.setFont('helvetica', bold ? 'bold' : 'normal').setFontSize(size);
    const lines: string[] = doc.splitTextToSize(value, lineWidth());
    const lineHeight = size * doc.getLineHeightFactor() * PT_TO_MM;
    if (y + lines.length * lineHeight > PAGE_HEIGHT - MARGIN) {
      doc.addPage();
      y = MARGIN;
      photoBottom = 0;
    }
    doc.text(lines, MARGIN, y + lineHeight * 0.8);
    y += lines.length * lineHeight + gapAfter;
  };
  const separator = () => {
    doc.setDrawColor(200).line(MARGIN, y, MARGIN + lineWidth(), y);
    y += 3;
  };
  const section = (title: string) => {
    y += 3;
    write(title, 12, true, 2);
  };

  const { pet, owner, visits } = h;
  write('PetHaven · Historia clínica', 18, true, 1);
  write(`N.º ${pet.id} · Generada el ${fmt(generatedAt)}`, 9, false, 3);
  separator();

  section('Paciente');
  write(`Nombre: ${pet.name} · Especie: ${pet.species === 'DOG' ? 'Perro' : 'Gato'} · Raza: ${pet.breed}`);
  write(`Edad: ${pet.age} años · Peso actual: ${pet.weight} kg · Estado: ${pet.active ? 'Activa' : 'Inactiva'}`);
  write(`Diagnóstico actual: ${pet.disease ?? 'Sin enfermedad registrada'}`);

  section('Dueño');
  write(`Nombre: ${owner.name} · Documento: ${owner.document}`);
  write(`Teléfono: ${owner.phone} · Correo: ${owner.email}`);

  if (visits.length > 0) {
    const first = visits[0];
    const last = visits[visits.length - 1];
    section('Resumen');
    write(
      `Consultas: ${visits.length} · Primera: ${fmt(first.date, 'dd/MM/yyyy')} · Última: ${fmt(last.date, 'dd/MM/yyyy')}`,
    );
    write(`Peso: ${first.weight} kg -> ${last.weight} kg · Última evolución: ${evolutionLabel(last.evolution)}`);
  }

  section('Consultas y tratamientos');
  if (visits.length === 0) {
    write('Esta mascota no tiene consultas registradas.');
  }
  visits.forEach((visit, i) => {
    const delta = visit.weightDelta ? ` (${signed(visit.weightDelta)})` : '';
    const drugs = visit.drugs.map((d) => `${d.drugName} (${d.units} u.)`).join(', ') || 'Ninguno';
    separator();
    write(`${i + 1}. ${fmt(visit.date)} · ${visit.veterinarianName}`, 10, true);
    write(`Peso: ${visit.weight} kg${delta} · Evolución: ${evolutionLabel(visit.evolution)}`);
    write(`Diagnóstico: ${visit.diagnosis}`);
    if (visit.notes) {
      write(`Observaciones: ${visit.notes}`);
    }
    write(`Medicamentos: ${drugs}`, 10, false, 3);
  });

  // Pie con número de página en todas las hojas.
  const pages = doc.getNumberOfPages();
  doc.setFont('helvetica', 'normal').setFontSize(8);
  for (let page = 1; page <= pages; page++) {
    doc.setPage(page);
    doc.text(`${pet.name} · Página ${page} de ${pages}`, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 8, { align: 'right' });
  }
  return doc;
}

// Recorta la foto al centro en un cuadrado y la pasa a JPEG con fondo blanco, así jsPDF
// la acepta sea cual sea el formato original (webp, avif, png con transparencia...).
async function photoToJpeg(blob: Blob | null): Promise<string | null> {
  if (!blob) return null;
  try {
    const bitmap = await createImageBitmap(blob);
    const side = Math.min(bitmap.width, bitmap.height);
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = PHOTO_PX;
    const ctx = canvas.getContext('2d');
    if (!ctx) return null;
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, PHOTO_PX, PHOTO_PX);
    ctx.drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, PHOTO_PX, PHOTO_PX);
    return canvas.toDataURL('image/jpeg', 0.85);
  } catch {
    return null; // imagen que el navegador no puede decodificar: el PDF sale sin foto
  }
}

export async function downloadClinicalHistoryPdf(h: ClinicalHistory, photo: Blob | null): Promise<void> {
  buildClinicalHistoryPdf(h, await photoToJpeg(photo)).save(`historia-clinica-${h.pet.name}-${h.pet.id}.pdf`);
}
