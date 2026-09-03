import { ChangeEvent, useRef, useState } from 'react';
import { AiSettingsPanel } from './AiSettingsPanel';
import { Button } from '../components/ui/button';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { downloadBackup, importBackup, ApiError } from '../api/client';

interface Props {
  onAuthExpired: () => void;
}

/**
 * Settings (docs/ui-overhaul-plan.md Phase 5, polished to match the mockup's
 * single-column sectioned layout): a stack of setting-group cards rather
 * than a side-nav, since there's only ever going to be a handful of these.
 * Maintenance reuses the same whole-instance downloadBackup/importBackup
 * flow as WorldsPage.tsx — backup/import is a one-shot action, not
 * per-world configurable state, so it lives here rather than being
 * duplicated per world.
 */
export function NextSettingsPage({ onAuthExpired }: Props) {
  const [error, setError] = useState<string | null>(null);
  const [backingUp, setBackingUp] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importing, setImporting] = useState(false);
  const [overwriteConfirmOpen, setOverwriteConfirmOpen] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  function handleError(err: unknown) {
    if (err instanceof ApiError && err.status === 401) {
      onAuthExpired();
      return;
    }
    setError(err instanceof Error ? err.message : 'Something went wrong');
  }

  async function handleBackup() {
    setBackingUp(true);
    try {
      await downloadBackup();
    } catch (err) {
      handleError(err);
    } finally {
      setBackingUp(false);
    }
  }

  function handleImportFileChosen(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    setImportFile(file);
    event.target.value = '';
  }

  async function runImport(mode: 'ADDITIVE' | 'OVERWRITE') {
    if (!importFile) return;
    setImporting(true);
    setError(null);
    try {
      await importBackup(importFile, mode);
      setImportFile(null);
    } catch (err) {
      handleError(err);
    } finally {
      setImporting(false);
    }
  }

  return (
    <section className="settings-sections">
      {error && <p className="error">{error}</p>}

      <section className="card">
        <p className="eyebrow">AI</p>
        <AiSettingsPanel onAuthExpired={onAuthExpired} />
      </section>

      <section className="card">
        <p className="eyebrow">Maintenance</p>
        <div className="settings-tile-row">
          <div className="settings-tile">
            <strong>Export whole instance</strong>
            <p className="muted">Download a ZIP of every world and its media.</p>
            <Button variant="outline" onClick={() => void handleBackup()} disabled={backingUp}>
              {backingUp ? 'Exporting…' : '⬇ Export'}
            </Button>
          </div>
          <div className="settings-tile">
            <strong>Import backup</strong>
            <p className="muted">Restore worlds from a previously exported ZIP.</p>
            <input
              ref={fileInputRef}
              type="file"
              accept=".zip"
              style={{ display: 'none' }}
              onChange={handleImportFileChosen}
            />
            <Button variant="outline" onClick={() => fileInputRef.current?.click()} disabled={importing}>
              Choose file…
            </Button>
          </div>
        </div>

        {importFile && (
          <div className="settings-import-confirm">
            <p>
              Import <strong>{importFile.name}</strong> as:
            </p>
            <Button onClick={() => void runImport('ADDITIVE')} disabled={importing}>
              {importing ? 'Importing…' : 'Add as new'}
            </Button>
            <Button
              variant="link"
              className="text-destructive hover:text-destructive"
              onClick={() => setOverwriteConfirmOpen(true)}
              disabled={importing}
            >
              Replace everything
            </Button>
            <Button variant="link" onClick={() => setImportFile(null)} disabled={importing}>
              Cancel
            </Button>
          </div>
        )}
      </section>

      <ConfirmDialog
        open={overwriteConfirmOpen}
        onOpenChange={setOverwriteConfirmOpen}
        title="Replace everything with this backup?"
        description="This deletes all existing worlds first. This cannot be undone."
        confirmLabel="Replace everything"
        destructive
        onConfirm={() => {
          setOverwriteConfirmOpen(false);
          void runImport('OVERWRITE');
        }}
      />
    </section>
  );
}
