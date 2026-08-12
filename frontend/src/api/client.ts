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

export function articlesApi(worldId: string) {
  const base = `/worlds/${worldId}/articles`;
  return {
    list: (params?: { categoryId?: string; q?: string }) => {
      const search = new URLSearchParams();
      if (params?.categoryId) search.set('categoryId', params.categoryId);
      if (params?.q) search.set('q', params.q);
      const qs = search.toString();
      return request<ArticleSummary[]>(qs ? `${base}?${qs}` : base);
    },
    get: (id: string) => request<Article>(`${base}/${id}`),
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
