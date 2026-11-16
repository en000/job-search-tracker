export function formatDate(date: string, options: Intl.DateTimeFormatOptions = {}) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', ...options, timeZone: 'UTC' })
    .format(new Date(date + 'T00:00:00Z'));
}

export function formatInstant(instant: string, zone: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium', timeStyle: 'short', timeZone: zone,
  }).format(new Date(instant));
}
