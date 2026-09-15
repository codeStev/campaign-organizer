import { useState } from 'react';
import { Button } from './ui/button';

interface Props {
  codes: string[];
  continueLabel?: string;
  onContinue: () => void;
}

/**
 * The "here are your one-time recovery codes, acknowledge before continuing" pattern — shared
 * by initial MFA enrollment (MfaSetupPage) and self-service regeneration
 * (RecoveryCodesPanel), since both show a freshly-issued batch exactly once.
 */
export function RecoveryCodesList({ codes, continueLabel = 'Continue', onContinue }: Props) {
  const [acknowledged, setAcknowledged] = useState(false);

  return (
    <>
      <p className="muted hint">
        Each code works once, to sign in if you lose your authenticator or to reset a forgotten
        password. Store them somewhere safe — they won't be shown again.
      </p>
      <ul className="recovery-codes">
        {codes.map((code) => (
          <li key={code}>
            <code>{code}</code>
          </li>
        ))}
      </ul>
      <label htmlFor="ack-recovery-codes" className="checkbox-label">
        <input
          id="ack-recovery-codes"
          type="checkbox"
          checked={acknowledged}
          onChange={(e) => setAcknowledged(e.target.checked)}
        />
        I've saved these recovery codes
      </label>
      <Button type="button" disabled={!acknowledged} onClick={onContinue}>
        {continueLabel}
      </Button>
    </>
  );
}
