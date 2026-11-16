import { describe, expect, it } from 'vitest';
import { resolveLocalTime, toLocalInput } from './zonedTime';

describe('tracker-zone entry', () => {
  it('rejects a time in the spring gap', () => {
    expect(resolveLocalTime('2026-03-08T02:30', 'America/New_York').kind).toBe('invalid');
  });

  it('resolves both offsets in the autumn overlap', () => {
    expect(resolveLocalTime('2026-11-01T01:30', 'America/New_York')).toEqual({
      kind: 'ambiguous',
      choices: [
        { offset: '-04:00', instant: '2026-11-01T05:30:00Z' },
        { offset: '-05:00', instant: '2026-11-01T06:30:00Z' },
      ],
    });
  });

  it('shows a stored instant in the selected tracker zone', () => {
    expect(toLocalInput('2026-11-16T00:30:00Z', 'America/Los_Angeles')).toBe('2026-11-15T16:30:00');
  });
});
