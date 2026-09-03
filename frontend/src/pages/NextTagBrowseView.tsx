import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError, TagBrowseResult, tagBrowseApi, worldTagsApi } from '../api/client';
import { Button } from '../components/ui/button';
import { Spinner } from '../components/ui/spinner';

interface Props {
  worldId: string;
  onOpenArticle: (id: string) => void;
  onOpenStatblock: (id: string) => void;
  onAuthExpired: () => void;
}

/**
 * Cross-entity "browse by tag" view (ADR-0083, FR-47): a world-level tag
 * index, and, when a tag is selected, every article and statblock carrying
 * it, both entity types together.
 */
export function NextTagBrowseView({ worldId, onOpenArticle, onOpenStatblock, onAuthExpired }: Props) {
  const navigate = useNavigate();
  const { tagName } = useParams<{ tagName: string }>();
  const [worldTags, setWorldTags] = useState<string[]>([]);
  const [result, setResult] = useState<TagBrowseResult | null>(null);
  const [loading, setLoading] = useState(true);

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
    },
    [onAuthExpired],
  );

  useEffect(() => {
    worldTagsApi(worldId).list().then(setWorldTags).catch(handleError);
  }, [worldId, handleError]);

  useEffect(() => {
    if (!tagName) {
      setResult(null);
      return;
    }
    setLoading(true);
    tagBrowseApi(worldId)
      .entities(tagName)
      .then(setResult)
      .catch(handleError)
      .finally(() => setLoading(false));
  }, [worldId, tagName, handleError]);

  if (!tagName) {
    return (
      <div className="wiki-main">
        <div className="card">
          <h3>Tags</h3>
          {worldTags.length === 0 && <p className="muted">No tags yet.</p>}
          <div className="beat-article-chips">
            {worldTags.map((t) => (
              <button
                key={t}
                type="button"
                className="beat-chip tag-chip-link"
                onClick={() => navigate(encodeURIComponent(t))}
              >
                {t}
              </button>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="wiki-main">
      <div className="card">
        <div className="form-actions">
          <h3 style={{ margin: 0 }}>Tag: {tagName}</h3>
          <span className="print-toolbar-spacer" />
          <Button variant="link" onClick={() => navigate('..', { relative: 'path' })}>
            ← All tags
          </Button>
        </div>
        {loading && (
          <p className="muted loading-row">
            <Spinner /> Loading…
          </p>
        )}
        {!loading && result && result.articles.length === 0 && result.statblocks.length === 0 && (
          <p className="muted">Nothing tagged "{tagName}" yet.</p>
        )}
        {!loading && result && result.articles.length > 0 && (
          <>
            <h4>Articles ({result.articles.length})</h4>
            <ul className="article-list">
              {result.articles.map((a) => (
                <li key={a.id}>
                  <Button variant="link" className="consistency-link" onClick={() => onOpenArticle(a.id)}>
                    {a.title}
                  </Button>
                </li>
              ))}
            </ul>
          </>
        )}
        {!loading && result && result.statblocks.length > 0 && (
          <>
            <h4>Statblocks ({result.statblocks.length})</h4>
            <ul className="article-list">
              {result.statblocks.map((s) => (
                <li key={s.id}>
                  <Button variant="link" className="consistency-link" onClick={() => onOpenStatblock(s.id)}>
                    {s.name}
                  </Button>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
    </div>
  );
}
