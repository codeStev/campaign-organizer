// Thin API client. Types mirror docs/api/openapi.yaml; run `npm run gen:api`
// to regenerate the full typed schema (src/api/schema.ts) from the contract.

export interface World {
  id: string;
  name: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WorldRequest {
  name: string;
  description?: string;
}

interface TokenResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
}

const TOKEN_KEY = 'co_token';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(init.headers);
  if (init.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`/api${path}`, { ...init, headers });

  if (response.status === 401) {
    clearToken();
    throw new ApiError(401, 'Not authenticated');
  }
  if (!response.ok) {
    const detail = await safeProblemDetail(response);
    throw new ApiError(response.status, detail);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function safeProblemDetail(response: Response): Promise<string> {
  try {
    const body = await response.json();
    return body.detail ?? body.title ?? response.statusText;
  } catch {
    return response.statusText;
  }
}

export async function login(password: string): Promise<void> {
  const result = await request<TokenResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ password }),
  });
  setToken(result.token);
}

export const worldsApi = {
  list: () => request<World[]>('/worlds'),
  get: (id: string) => request<World>(`/worlds/${id}`),
  create: (body: WorldRequest) =>
    request<World>('/worlds', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: string, body: WorldRequest) =>
    request<World>(`/worlds/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  remove: (id: string) => request<void>(`/worlds/${id}`, { method: 'DELETE' }),
};

// ---- Wiki: articles & categories (mirrors docs/api/openapi.yaml) ----

export type ArticleTemplate =
  | 'GENERIC'
  | 'CHARACTER'
  | 'LOCATION'
  | 'ORGANIZATION'
  | 'SPECIES'
  | 'ITEM'
  | 'EVENT';

export const ARTICLE_TEMPLATES: ArticleTemplate[] = [
  'GENERIC',
  'CHARACTER',
  'LOCATION',
  'ORGANIZATION',
  'SPECIES',
  'ITEM',
  'EVENT',
];

export interface ArticleSummary {
  id: string;
  worldId: string;
  categoryId?: string | null;
  title: string;
  slug: string;
  template: ArticleTemplate;
  createdAt: string;
  updatedAt: string;
}

export interface Article extends ArticleSummary {
  body?: string | null;
  /** Server-rendered body with `[[wiki-links]]` resolved (read-only, ADR-0014). */
  bodyHtml?: string | null;
}

export interface ArticleRequest {
  title: string;
  slug?: string;
  template?: ArticleTemplate;
  categoryId?: string | null;
  body?: string;
}

export interface Category {
  id: string;
  worldId: string;
  parentId?: string | null;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface Usage {
  type: 'BEAT' | 'MAP_PIN' | 'TIMELINE_EVENT' | 'RELATIONSHIP' | 'CHARACTER_SHEET' | 'STATBLOCK' | 'ARTICLE_LINK';
  label: string;
  targetId?: string | null;
  campaignId?: string | null;
  campaignName?: string | null;
}

export function articlesApi(worldId: string) {
  const base = `/worlds/${worldId}/articles`;
  return {
    list: (params?: { categoryId?: string; q?: string; campaignId?: string }) => {
      const search = new URLSearchParams();
      if (params?.categoryId) search.set('categoryId', params.categoryId);
      if (params?.q) search.set('q', params.q);
      if (params?.campaignId) search.set('campaignId', params.campaignId);
      const qs = search.toString();
      return request<ArticleSummary[]>(qs ? `${base}?${qs}` : base);
    },
    get: (id: string) => request<Article>(`${base}/${id}`),
    usages: (id: string) => request<{ usages: Usage[] }>(`${base}/${id}/usages`),
    create: (body: ArticleRequest) =>
      request<Article>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: ArticleRequest) =>
      request<Article>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function categoriesApi(worldId: string) {
  const base = `/worlds/${worldId}/categories`;
  return {
    list: () => request<Category[]>(base),
  };
}

export interface TemplateSection {
  heading: string;
  hint: string;
}

export interface ArticleTemplateInfo {
  template: ArticleTemplate;
  label: string;
  sections: TemplateSection[];
}

export const templatesApi = {
  list: () => request<ArticleTemplateInfo[]>('/article-templates'),
};

export interface MediaAsset {
  id: string;
  worldId: string;
  filename: string;
  contentType: string;
  size: number;
  url: string;
  createdAt: string;
}

export interface WorldMap {
  id: string;
  worldId: string;
  name: string;
  mediaId?: string | null;
  imageUrl?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MapRequest {
  name: string;
  mediaId: string;
}

export interface MapPin {
  id: string;
  mapId: string;
  articleId?: string | null;
  label?: string | null;
  layer?: string | null;
  x: number;
  y: number;
  createdAt: string;
  updatedAt: string;
}

export interface MapPinRequest {
  articleId?: string | null;
  label?: string | null;
  layer?: string | null;
  x: number;
  y: number;
}

export function mapsApi(worldId: string) {
  const base = `/worlds/${worldId}/maps`;
  return {
    list: () => request<WorldMap[]>(base),
    get: (id: string) => request<WorldMap>(`${base}/${id}`),
    create: (body: MapRequest) =>
      request<WorldMap>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: MapRequest) =>
      request<WorldMap>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function pinsApi(worldId: string, mapId: string) {
  const base = `/worlds/${worldId}/maps/${mapId}/pins`;
  return {
    list: () => request<MapPin[]>(base),
    create: (body: MapPinRequest) =>
      request<MapPin>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: MapPinRequest) =>
      request<MapPin>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export interface Relationship {
  id: string;
  worldId: string;
  fromArticleId: string;
  toArticleId: string;
  label?: string | null;
  directed: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RelationshipRequest {
  fromArticleId: string;
  toArticleId: string;
  label?: string | null;
  directed?: boolean;
}

export function relationshipsApi(worldId: string) {
  const base = `/worlds/${worldId}/relationships`;
  return {
    list: () => request<Relationship[]>(base),
    create: (body: RelationshipRequest) =>
      request<Relationship>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: RelationshipRequest) =>
      request<Relationship>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export interface CalendarMonthInput {
  name: string;
  days: number;
}

export interface Calendar {
  id: string;
  worldId: string;
  name: string;
  daysPerWeek?: number | null;
  months: CalendarMonthInput[];
  createdAt: string;
  updatedAt: string;
}

export interface CalendarRequest {
  name: string;
  daysPerWeek?: number | null;
  months: CalendarMonthInput[];
}

export function calendarsApi(worldId: string) {
  const base = `/worlds/${worldId}/calendars`;
  return {
    list: () => request<Calendar[]>(base),
    create: (body: CalendarRequest) =>
      request<Calendar>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: CalendarRequest) =>
      request<Calendar>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export interface Timeline {
  id: string;
  worldId: string;
  name: string;
  description?: string | null;
  calendarId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TimelineRequest {
  name: string;
  description?: string;
  calendarId?: string | null;
}

export interface TimelineEvent {
  id: string;
  timelineId: string;
  articleId?: string | null;
  title: string;
  description?: string | null;
  year: number;
  month?: number | null;
  day?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface TimelineEventRequest {
  title: string;
  description?: string;
  articleId?: string | null;
  year: number;
  month?: number | null;
  day?: number | null;
}

export function timelinesApi(worldId: string) {
  const base = `/worlds/${worldId}/timelines`;
  return {
    list: () => request<Timeline[]>(base),
    create: (body: TimelineRequest) =>
      request<Timeline>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: TimelineRequest) =>
      request<Timeline>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function eventsApi(worldId: string, timelineId: string) {
  const base = `/worlds/${worldId}/timelines/${timelineId}/events`;
  return {
    list: () => request<TimelineEvent[]>(base),
    create: (body: TimelineEventRequest) =>
      request<TimelineEvent>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: TimelineEventRequest) =>
      request<TimelineEvent>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function mediaApi(worldId: string) {
  const base = `/worlds/${worldId}/media`;
  return {
    list: () => request<MediaAsset[]>(base),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
    // Multipart upload: let the browser set the Content-Type boundary itself.
    upload: async (file: File): Promise<MediaAsset> => {
      const form = new FormData();
      form.append('file', file);
      const token = getToken();
      const response = await fetch(`/api${base}`, {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        body: form,
      });
      if (response.status === 401) {
        clearToken();
        throw new ApiError(401, 'Not authenticated');
      }
      if (!response.ok) {
        throw new ApiError(response.status, await safeProblemDetail(response));
      }
      return (await response.json()) as MediaAsset;
    },
  };
}

// ---- GM campaign manager (mirrors docs/api/openapi.yaml) ----

export interface Campaign {
  id: string;
  worldId: string;
  name: string;
  description?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CampaignRequest {
  name: string;
  description?: string | null;
  notes?: string | null;
}

export interface Session {
  id: string;
  campaignId: string;
  title: string;
  sessionNumber?: number | null;
  date?: string | null;
  summary?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SessionRequest {
  title: string;
  sessionNumber?: number | null;
  date?: string | null;
  summary?: string | null;
  notes?: string | null;
}

export type ArcStatus = 'PLANNED' | 'ACTIVE' | 'COMPLETED' | 'ABANDONED';
export const ARC_STATUSES: ArcStatus[] = ['PLANNED', 'ACTIVE', 'COMPLETED', 'ABANDONED'];

export interface Arc {
  id: string;
  campaignId: string;
  title: string;
  description?: string | null;
  status: ArcStatus;
  position: number;
  createdAt: string;
  updatedAt: string;
}

export interface ArcRequest {
  title: string;
  description?: string | null;
  status?: ArcStatus;
  position?: number | null;
}

export interface Beat {
  id: string;
  arcId: string;
  title: string;
  body?: string | null;
  done: boolean;
  articleIds: string[];
  sessionId?: string | null;
  position: number;
  createdAt: string;
  updatedAt: string;
}

export interface BeatRequest {
  title: string;
  body?: string | null;
  done?: boolean;
  articleIds?: string[];
  sessionId?: string | null;
  position?: number | null;
}

export function campaignsApi(worldId: string) {
  const base = `/worlds/${worldId}/campaigns`;
  return {
    list: () => request<Campaign[]>(base),
    get: (id: string) => request<Campaign>(`${base}/${id}`),
    create: (body: CampaignRequest) =>
      request<Campaign>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: CampaignRequest) =>
      request<Campaign>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function sessionsApi(worldId: string, campaignId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/sessions`;
  return {
    list: () => request<Session[]>(base),
    create: (body: SessionRequest) =>
      request<Session>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: SessionRequest) =>
      request<Session>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function arcsApi(worldId: string, campaignId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/arcs`;
  return {
    list: () => request<Arc[]>(base),
    create: (body: ArcRequest) =>
      request<Arc>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: ArcRequest) =>
      request<Arc>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function beatsApi(worldId: string, campaignId: string, arcId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/arcs/${arcId}/beats`;
  return {
    list: () => request<Beat[]>(base),
    create: (body: BeatRequest) =>
      request<Beat>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: BeatRequest) =>
      request<Beat>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

// ---- Character sheets, statblocks, dice (mirrors docs/api/openapi.yaml) ----

export type SheetFieldType = 'TEXT' | 'TEXTAREA' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'CIRCLES';

export type FieldWidth = 'FULL' | 'HALF' | 'THIRD' | 'QUARTER';

export interface SheetField {
  key: string;
  label: string;
  type: SheetFieldType;
  options?: string[] | null;
  width?: FieldWidth | null;
  count?: number | null;
}

export interface SheetSection {
  title: string;
  fields: SheetField[];
}

export interface SheetTemplate {
  id: string;
  worldId: string;
  name: string;
  system?: string | null;
  sections: SheetSection[];
  createdAt: string;
  updatedAt: string;
}

export interface SheetTemplateRequest {
  name: string;
  system?: string | null;
  sections: SheetSection[];
}

export interface BuiltinSheetTemplate {
  name: string;
  system?: string | null;
  sections: SheetSection[];
}

export interface CharacterSheet {
  id: string;
  worldId: string;
  templateId: string;
  articleId?: string | null;
  campaignId?: string | null;
  name: string;
  values: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface CharacterSheetRequest {
  name: string;
  templateId: string;
  articleId?: string | null;
  campaignId?: string | null;
  values?: Record<string, unknown>;
}

export interface Statblock {
  id: string;
  worldId: string;
  articleId?: string | null;
  campaignId?: string | null;
  name: string;
  stats: Record<string, unknown>;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface StatblockRequest {
  name: string;
  articleId?: string | null;
  campaignId?: string | null;
  stats?: Record<string, unknown>;
  notes?: string | null;
}

export interface DieRoll {
  sides: number;
  value: number;
  kept: boolean;
}

export interface DiceRollResult {
  expression: string;
  total: number;
  rolls: DieRoll[];
  breakdown: string;
}

export function sheetTemplatesApi(worldId: string) {
  const base = `/worlds/${worldId}/sheet-templates`;
  return {
    list: () => request<SheetTemplate[]>(base),
    get: (id: string) => request<SheetTemplate>(`${base}/${id}`),
    create: (body: SheetTemplateRequest) =>
      request<SheetTemplate>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: SheetTemplateRequest) =>
      request<SheetTemplate>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export const builtinSheetTemplatesApi = {
  list: () => request<BuiltinSheetTemplate[]>('/sheet-templates/builtin'),
};

export function characterSheetsApi(worldId: string) {
  const base = `/worlds/${worldId}/character-sheets`;
  return {
    list: (campaignId?: string) =>
      request<CharacterSheet[]>(campaignId ? `${base}?campaignId=${campaignId}` : base),
    get: (id: string) => request<CharacterSheet>(`${base}/${id}`),
    create: (body: CharacterSheetRequest) =>
      request<CharacterSheet>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: CharacterSheetRequest) =>
      request<CharacterSheet>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function statblocksApi(worldId: string) {
  const base = `/worlds/${worldId}/statblocks`;
  return {
    list: (campaignId?: string) =>
      request<Statblock[]>(campaignId ? `${base}?campaignId=${campaignId}` : base),
    create: (body: StatblockRequest) =>
      request<Statblock>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: StatblockRequest) =>
      request<Statblock>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export const diceApi = {
  roll: (expression: string) =>
    request<DiceRollResult>('/dice/roll', { method: 'POST', body: JSON.stringify({ expression }) }),
};

export interface ArticleRevision {
  id: string;
  articleId: string;
  title: string;
  slug: string;
  template: ArticleTemplate;
  body?: string | null;
  createdAt: string;
}

export function articleRevisionsApi(worldId: string, articleId: string) {
  const base = `/worlds/${worldId}/articles/${articleId}/revisions`;
  return {
    list: () => request<ArticleRevision[]>(base),
    restore: (revisionId: string) =>
      request<Article>(`${base}/${revisionId}/restore`, { method: 'POST' }),
  };
}

/** Downloads the world's JSON export bundle via the browser. */
export async function exportWorld(worldId: string): Promise<void> {
  const token = getToken();
  const response = await fetch(`/api/worlds/${worldId}/export`, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (response.status === 401) {
    clearToken();
    throw new ApiError(401, 'Not authenticated');
  }
  if (!response.ok) {
    throw new ApiError(response.status, await safeProblemDetail(response));
  }
  const disposition = response.headers.get('Content-Disposition') ?? '';
  const match = disposition.match(/filename="?([^"]+)"?/);
  const filename = match ? match[1] : `world-${worldId}.json`;
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

// ---- Whiteboards (mirrors docs/api/openapi.yaml) ----

export interface WhiteboardNode {
  id: string;
  text: string;
  x: number;
  y: number;
  color?: string | null;
}

export interface WhiteboardEdge {
  id: string;
  fromNodeId: string;
  toNodeId: string;
  label?: string | null;
}

export interface Whiteboard {
  id: string;
  worldId: string;
  name: string;
  nodes: WhiteboardNode[];
  edges: WhiteboardEdge[];
  createdAt: string;
  updatedAt: string;
}

export interface WhiteboardRequest {
  name: string;
  nodes: WhiteboardNode[];
  edges: WhiteboardEdge[];
}

export function whiteboardsApi(worldId: string) {
  const base = `/worlds/${worldId}/whiteboards`;
  return {
    list: () => request<Whiteboard[]>(base),
    get: (id: string) => request<Whiteboard>(`${base}/${id}`),
    create: (body: WhiteboardRequest) =>
      request<Whiteboard>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: WhiteboardRequest) =>
      request<Whiteboard>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

/** Downloads a character sheet's filled PDF (D&D 5e). */
export async function exportCharacterSheetPdf(worldId: string, sheetId: string): Promise<void> {
  const token = getToken();
  const response = await fetch(`/api/worlds/${worldId}/character-sheets/${sheetId}/pdf`, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (response.status === 401) {
    clearToken();
    throw new ApiError(401, 'Not authenticated');
  }
  if (!response.ok) {
    throw new ApiError(response.status, await safeProblemDetail(response));
  }
  const disposition = response.headers.get('Content-Disposition') ?? '';
  const match = disposition.match(/filename="?([^"]+)"?/);
  const filename = match ? match[1] : `character-${sheetId}.pdf`;
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
