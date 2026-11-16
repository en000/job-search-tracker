import { Link } from 'react-router-dom';
import { send, useWrite } from '../../shared/api';
import type { CompletionSuggestion, Role } from '../../shared/types';
import { WriteError } from '../../shared/ui';

function SuggestionRow({ suggestion }: { suggestion: CompletionSuggestion }) {
  const rolePath = '/roles/' + suggestion.roleId;
  const write = useWrite((action: 'DISMISS' | 'SNOOZE') => send<Role>(
    rolePath + '/completion-suggestion', 'POST', { action, expectedVersion: suggestion.roleVersion },
  ));

  return <li>
    <div className="work-main">
      <Link to={rolePath} className="work-label">{suggestion.roleTitle}</Link>
      <div className="muted">{suggestion.company}</div>
    </div>
    <div className="form-actions">
      <Link className="button secondary small" to={rolePath + '?review=no-response'}>Review</Link>
      <button type="button" className="button secondary small" disabled={write.isPending}
        onClick={() => write.mutate('SNOOZE')}>Remind me in 2 weeks</button>
      <button type="button" className="link-button" disabled={write.isPending}
        onClick={() => write.mutate('DISMISS')}>Dismiss</button>
    </div>
    {write.error && <div className="work-error"><WriteError error={write.error} /></div>}
  </li>;
}

export function CompletionSuggestions({ suggestions }: { suggestions: CompletionSuggestion[] }) {
  if (!suggestions.length) return null;
  return <section className="surface work-group completion-suggestions" aria-labelledby="completion-suggestions-heading">
    <div className="section-heading">
      <div>
        <h2 id="completion-suggestions-heading">Review inactive applications</h2>
        <p className="muted">These applications have been at the same stage for at least 2 weeks. If you’re still waiting for a reply, consider closing them as No response.</p>
      </div>
    </div>
    <ul className="work-list">
      {suggestions.map((suggestion) => <SuggestionRow key={suggestion.roleId} suggestion={suggestion} />)}
    </ul>
  </section>;
}
