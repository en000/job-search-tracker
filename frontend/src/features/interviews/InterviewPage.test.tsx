import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { expect, it, vi } from 'vitest';
import { InterviewForm } from './InterviewPage';

function renderInterview() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<QueryClientProvider client={client}><MemoryRouter>
    <InterviewForm roleId="local-role" timeZone="America/New_York" />
  </MemoryRouter></QueryClientProvider>);
  return userEvent.setup();
}

it('retains participant and note edits after field validation fails', async () => {
  const mock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
    code: 'invalid_input', detail: 'Check the highlighted fields.', fieldErrors: { meetingUrl: 'Enter an absolute HTTP or HTTPS URL.' },
  }), { status: 400 }));
  vi.stubGlobal('fetch', mock);
  const user = renderInterview();
  await user.type(screen.getByLabelText('Interview title'), 'Screen');
  await user.click(screen.getByRole('button', { name: 'Add interviewer' }));
  await user.click(screen.getByRole('button', { name: 'Add interviewer' }));
  await user.type(within(screen.getByRole('group', { name: 'Interviewer 1' })).getByLabelText('Name'), 'Alex');
  await user.type(within(screen.getByRole('group', { name: 'Interviewer 2' })).getByLabelText('Name'), 'Taylor');
  await user.type(screen.getByLabelText('Preparation notes'), 'Study architecture');
  await user.click(screen.getByRole('button', { name: 'Save interview' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Check the highlighted fields.');
  expect(screen.getByLabelText('Preparation notes')).toHaveValue('Study architecture');
  expect(within(screen.getByRole('group', { name: 'Interviewer 2' })).getByLabelText('Name')).toHaveValue('Taylor');
  expect(mock).toHaveBeenCalledTimes(1);
});

it('prompts for an offset and submits the chosen instant', async () => {
  const mock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: 'interview' }), { status: 201 }));
  vi.stubGlobal('fetch', mock);
  const user = renderInterview();
  await user.type(screen.getByLabelText('Interview title'), 'Panel');
  await user.selectOptions(screen.getByLabelText('Status'), 'SCHEDULED');
  const time = screen.getByLabelText('Start time');
  fireEvent.change(time, { target: { value: '2026-03-08T02:30' } });
  expect(screen.getByText(/does not exist/)).toBeVisible();
  fireEvent.change(time, { target: { value: '2026-11-01T01:30' } });
  const offset = screen.getByLabelText('Start time offset');
  expect(offset).toBeRequired();
  await user.selectOptions(offset, '-05:00');
  await user.click(screen.getByRole('button', { name: 'Save interview' }));
  await waitFor(() => expect(mock).toHaveBeenCalledTimes(1));
  expect(JSON.parse(mock.mock.calls[0][1].body).startsAt).toBe('2026-11-01T06:30:00Z');
});
