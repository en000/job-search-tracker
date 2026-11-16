import { Temporal } from '@js-temporal/polyfill';

export function toLocalInput(instant: string | null, zone: string) {
  if (!instant) return '';
  return Temporal.Instant.from(instant).toZonedDateTimeISO(zone).toPlainDateTime()
    .toString({ smallestUnit: 'second' });
}

type TimeResolution =
  | { kind: 'empty' }
  | { kind: 'invalid'; message: string }
  | { kind: 'valid'; instant: string }
  | { kind: 'ambiguous'; choices: { offset: string; instant: string }[] };

export function resolveLocalTime(value: string, zone: string): TimeResolution {
  if (!value) return { kind: 'empty' };
  try {
    const local = Temporal.PlainDateTime.from(value);
    const earlier = local.toZonedDateTime(zone, { disambiguation: 'earlier' });
    const later = local.toZonedDateTime(zone, { disambiguation: 'later' });
    if (!earlier.toPlainDateTime().equals(local) || !later.toPlainDateTime().equals(local)) {
      return { kind: 'invalid', message: 'This local time does not exist in ' + zone + '. Choose a different time.' };
    }
    if (earlier.equals(later)) return { kind: 'valid', instant: earlier.toInstant().toString() };
    return {
      kind: 'ambiguous',
      choices: [earlier, later].map((time) => ({ offset: time.offset, instant: time.toInstant().toString() })),
    };
  } catch {
    return { kind: 'invalid', message: 'Enter a valid local date and time.' };
  }
}
