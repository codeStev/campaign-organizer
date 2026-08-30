import { useCallback, useEffect, useState } from 'react';
import {
  BrokenLink,
  consistencyApi,
  ConsistencyArticle,
  ConsistencyReport,
  ApiError,
} from '../api/client';
import { Button } from '../components/ui/button';
import { NewWindowPortal, PrintButton } from '../components/NewWindowPortal';
import { PrintOptionsMenu, usePrintOptions } from '../components/PrintOptionsMenu';

interface Props {
  worldId: string;
  worldName: string;
  onOpenArticle: (id: string) => void;
  onAuthExpired: () => void;
}

const SOURCE_LABELS: Record<BrokenLink['sourceType'], string> = {
  ARTICLE: 'Article',
  BEAT: 'Beat',
  ROLL_TABLE: 'Roll table',
  CARD_DECK: 'Card deck',
};

/**
 * FR-43: the per-world consistency report — broken [[wiki-links]] across
 * every rendered body, articles nothing links to, and articles no campaign
 * reaches. Read-only lint; each finding links to where it can be fixed.
 */
export function ConsistencyView({ worldId, worldName, onOpenArticle, onAuthExpired }: Props) {
  const api = consistencyApi(worldId);
  const [report, setReport] = useState<ConsistencyReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [printing, setPrinting] = useState(false);
  const { opts: printOpts, setOpts: setPrintOpts, docProps: printDocProps } = usePrintOptions();

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setReport(await api.report());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [api, handleError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  if (loading && !report) {
    return <div className="wiki-main">
      <p className="muted">Checking the world…</p>
    </div>;
  }

  const clean =
    report != null &&
    report.brokenLinks.length === 0 &&
    report.orphanedArticles.length === 0 &&
    report.unreferencedByCampaigns.length === 0;

  return (
    <div className="wiki-main">
      {error && <p className="error">{error}</p>}
      <div className="card">
        <div className="form-actions">
          <h3 style={{ margin: 0 }}>Consistency report</h3>
          <span className="print-toolbar-spacer" />
          <Button variant="outline" onClick={() => void refresh()} disabled={loading}>
            {loading ? 'Checking…' : '↻ Re-check'}
          </Button>
          {report && !clean && (
            <Button variant="outline" onClick={() => setPrinting(true)}>
              🖨 Print
            </Button>
          )}
        </div>
        <p className="muted">
          Broken wiki-links across articles, beats, roll tables and card decks;
          articles nothing points at; articles no campaign uses.
        </p>

        {report && clean && (
          <p className="muted">✓ No issues found. Every link resolves and nothing is stranded.</p>
        )}

        {report && report.brokenLinks.length > 0 && (
          <>
            <h4>Broken links ({report.brokenLinks.length})</h4>
            <ul className="article-list consistency-list">
              {report.brokenLinks.map((l, i) => (
                <BrokenLinkRow key={i} link={l} onOpenArticle={onOpenArticle} />
              ))}
            </ul>
          </>
        )}

        {report && report.orphanedArticles.length > 0 && (
          <>
            <h4>Orphaned articles ({report.orphanedArticles.length})</h4>
            <ul className="article-list consistency-list">
              {report.orphanedArticles.map((a) => (
                <li key={a.articleId}>
                  <Button variant="link" className="consistency-link" onClick={() => onOpenArticle(a.articleId)}>
                    {a.title}
                  </Button>
                  <span className="muted"> — nothing links here</span>
                </li>
              ))}
            </ul>
          </>
        )}

        {report && report.unreferencedByCampaigns.length > 0 && (
          <>
            <h4>Not used by any campaign ({report.unreferencedByCampaigns.length})</h4>
            <ul className="article-list consistency-list">
              {report.unreferencedByCampaigns.map((a) => (
                <li key={a.articleId}>
                  <Button variant="link" className="consistency-link" onClick={() => onOpenArticle(a.articleId)}>
                    {a.title}
                  </Button>
                  <span className="muted"> — no beat or sheet references it</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>

      {report && !clean && printing && (
        <NewWindowPortal title={`Print — Consistency report`} onClose={() => setPrinting(false)}>
          <div className="print-toolbar">
            <strong>Consistency report</strong>
            <PrintOptionsMenu opts={printOpts} onChange={setPrintOpts} />
            <span className="print-toolbar-spacer" />
            <PrintButton />
            <Button variant="link" onClick={() => setPrinting(false)}>
              Close
            </Button>
          </div>
          <div className="print-doc" {...printDocProps}>
            <section className="print-cover">
              <h1>{worldName}</h1>
              <p className="print-subtitle">Consistency report</p>
            </section>

            {report.brokenLinks.length > 0 && (
              <section className="print-map-section">
                <h2>Broken links</h2>
                <table className="print-table-grid">
                  <thead>
                    <tr>
                      <th>Source</th>
                      <th>Missing target</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.brokenLinks.map((l, i) => (
                      <tr key={i}>
                        <td>
                          {SOURCE_LABELS[l.sourceType]} — {l.sourceLabel}
                        </td>
                        <td>{l.target}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
            )}

            <ArticleIssuePrintSection
              title="Orphaned articles"
              hint="nothing links to these"
              articles={report.orphanedArticles}
            />
            <ArticleIssuePrintSection
              title="Not used by any campaign"
              hint="no beat or sheet references these"
              articles={report.unreferencedByCampaigns}
            />
          </div>
        </NewWindowPortal>
      )}
    </div>
  );
}

function BrokenLinkRow({ link, onOpenArticle }: { link: BrokenLink; onOpenArticle: (id: string) => void }) {
  return (
    <li>
      <span className="consistency-source">{SOURCE_LABELS[link.sourceType]}</span>{' '}
      {/* Only article sources have somewhere to navigate to. */}
      {link.sourceType === 'ARTICLE' ? (
        <Button variant="link" className="consistency-link" onClick={() => onOpenArticle(link.sourceId)}>
          {link.sourceLabel}
        </Button>
      ) : (
        <span>{link.sourceLabel}</span>
      )}
      <span className="muted"> → </span>
      <code>{link.target}</code>
    </li>
  );
}

function ArticleIssuePrintSection({
  title,
  hint,
  articles,
}: {
  title: string;
  hint: string;
  articles: ConsistencyArticle[];
}) {
  if (articles.length === 0) return null;
  return (
    <section className="print-map-section">
      <h2>
        {title} <small className="print-kicker">({hint})</small>
      </h2>
      <ul>
        {articles.map((a) => (
          <li key={a.articleId}>{a.title}</li>
        ))}
      </ul>
    </section>
  );
}
