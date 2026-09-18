// Thin API client. Types mirror docs/api/openapi.yaml; run `npm run gen:api`
// to regenerate the full typed schema (src/api/schema.ts) from the contract.

export interface World {
  id: string;
  name: string;
  description?: string | null;
  /** Cosmetic-only sandbox/brainstorming flag (FR-60) — no functional effect. */
  scratch: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface WorldRequest {
  name: string;
  description?: string;
  scratch?: boolean;
}

export interface TokenResponse {
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

/**
 * `tokenOverride` lets the MFA setup/challenge flow (ADR-0111) send its short-lived,
 * PASSWORD-only pending token explicitly — that token deliberately isn't stored via
 * setToken()/getToken() (the "real" token used everywhere else), since it isn't usable for
 * general API access and storing it there would make the app briefly look logged-in before
 * MFA is actually complete.
 */
async function request<T>(path: string, init: RequestInit = {}, tokenOverride?: string): Promise<T> {
  const token = tokenOverride ?? getToken();
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

// ---- Login and MFA (ADR-0111): a correct password alone never grants full access ----

export type LoginStatus = 'MFA_SETUP_REQUIRED' | 'MFA_CHALLENGE_REQUIRED';

export interface LoginResponse {
  status: LoginStatus;
  /** PASSWORD-only pending token — pass to the MFA functions below, don't store via setToken(). */
  token: string;
  tokenType: string;
  expiresAt: string;
  /** Present only when status is MFA_CHALLENGE_REQUIRED. */
  method: MfaMethod | null;
}

/**
 * Never resolves to a directly-usable token — the caller must inspect `status` and continue
 * through either /auth/mfa/setup/** (MFA_SETUP_REQUIRED) or /auth/mfa/verify* (MFA_CHALLENGE_REQUIRED).
 */
export function login(email: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

/**
 * Google sign-in (ADR-0113) — "Sign in with Google" is a plain `<a href="/oauth2/authorization/google">`
 * link (a full-page browser navigation, outside this file entirely), not a fetch call. This
 * status check lets the caller hide that link on a deployment with no Google credentials
 * configured, so it never links to a 404 instead of failing loudly and confusingly at click time.
 */
export function getOidcStatus(): Promise<{ googleEnabled: boolean }> {
  return request<{ googleEnabled: boolean }>('/auth/oidc/status');
}

/**
 * Redeems the single-use code the backend redirected the browser back with, after a completed
 * Google sign-in — same {@link LoginResponse} shape `login()` returns, feed it into the same
 * MFA setup/challenge routing.
 */
export function exchangeOidcCode(code: string): Promise<LoginResponse> {
  return request<LoginResponse>('/auth/oidc/exchange', {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}

export interface TotpSetupStart {
  secret: string;
  provisioningUri: string;
  qrCodeDataUri: string;
}

export interface MfaEnrollmentResult {
  token: string;
  tokenType: string;
  expiresAt: string;
  /** Shown only in this response — the caller must display them once and move on. */
  recoveryCodes: string[];
}

export function startTotpSetup(pendingToken: string): Promise<TotpSetupStart> {
  return request<TotpSetupStart>('/auth/mfa/setup/totp/start', { method: 'POST' }, pendingToken);
}

export function confirmTotpSetup(pendingToken: string, code: string): Promise<MfaEnrollmentResult> {
  return request<MfaEnrollmentResult>(
    '/auth/mfa/setup/totp/confirm',
    { method: 'POST', body: JSON.stringify({ code }) },
    pendingToken,
  );
}

export function verifyTotpChallenge(pendingToken: string, code: string): Promise<TokenResponse> {
  return request<TokenResponse>(
    '/auth/mfa/verify',
    { method: 'POST', body: JSON.stringify({ code }) },
    pendingToken,
  );
}

/** Consumes one recovery code and forces re-enrollment (the old device may be gone for good). */
export function verifyRecoveryCode(pendingToken: string, recoveryCode: string): Promise<LoginResponse> {
  return request<LoginResponse>(
    '/auth/mfa/verify-recovery-code',
    { method: 'POST', body: JSON.stringify({ recoveryCode }) },
    pendingToken,
  );
}

/** No token involved — identity is proven by the recovery code itself. */
export function recoverPassword(email: string, recoveryCode: string, newPassword: string): Promise<void> {
  return request<void>('/auth/recover-password', {
    method: 'POST',
    body: JSON.stringify({ email, recoveryCode, newPassword }),
  });
}

// ---- WebAuthn/passkeys (ADR-0111 follow-up): Spring Security's own ceremony endpoints ----
//
// Unlike everything else in this file, /webauthn/** and /login/webauthn are Spring Security's
// own endpoints, not this app's — they live at the root, not under /api, and (since this app is
// otherwise a pure bearer-token API with no cookies) are the one place a CSRF cookie is actually
// enforced. The cookie itself is set automatically by SecurityConfig's `.spa()` CSRF mode on
// every response, so by the time a user reaches MFA setup/challenge (always after /auth/login)
// it's already there.

function base64UrlToBuffer(value: string): ArrayBuffer {
  const padded = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=');
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

function bufferToBase64Url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function getCsrfCookie(): string | null {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/** Calls one of Spring Security's own WebAuthn endpoints directly — not under /api. */
async function webauthnRequest<T>(path: string, pendingToken: string, body?: unknown): Promise<T> {
  const headers = new Headers({ Authorization: `Bearer ${pendingToken}` });
  if (body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  const csrf = getCsrfCookie();
  if (csrf) {
    headers.set('X-XSRF-TOKEN', csrf);
  }
  const response = await fetch(path, {
    method: 'POST',
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    throw new ApiError(response.status, await safeProblemDetail(response));
  }
  return (await response.json()) as T;
}

interface WebauthnCreationOptionsJson {
  rp: { name: string; id: string };
  user: { name: string; id: string; displayName: string };
  challenge: string;
  pubKeyCredParams: PublicKeyCredentialParameters[];
  timeout?: number;
  excludeCredentials?: Array<{ id: string; type: 'public-key'; transports?: AuthenticatorTransport[] }>;
  authenticatorSelection?: AuthenticatorSelectionCriteria;
  attestation?: AttestationConveyancePreference;
  extensions?: Record<string, unknown>;
}

interface WebauthnRequestOptionsJson {
  challenge: string;
  timeout?: number;
  rpId?: string;
  allowCredentials?: Array<{ id: string; type: 'public-key'; transports?: AuthenticatorTransport[] }>;
  userVerification?: UserVerificationRequirement;
  extensions?: Record<string, unknown>;
}

async function fetchCreationOptions(pendingToken: string): Promise<CredentialCreationOptions> {
  const json = await webauthnRequest<WebauthnCreationOptionsJson>('/webauthn/register/options', pendingToken);
  return {
    publicKey: {
      ...json,
      challenge: base64UrlToBuffer(json.challenge),
      user: { ...json.user, id: base64UrlToBuffer(json.user.id) },
      excludeCredentials: json.excludeCredentials?.map((c) => ({ ...c, id: base64UrlToBuffer(c.id) })),
    },
  };
}

async function fetchRequestOptions(pendingToken: string): Promise<CredentialRequestOptions> {
  const json = await webauthnRequest<WebauthnRequestOptionsJson>('/webauthn/authenticate/options', pendingToken);
  return {
    publicKey: {
      ...json,
      challenge: base64UrlToBuffer(json.challenge),
      allowCredentials: json.allowCredentials?.map((c) => ({ ...c, id: base64UrlToBuffer(c.id) })),
    },
  };
}

/** Shared by both enrollment and self-service "add a backup passkey" — see callers below. */
async function registerCredential(token: string, label: string): Promise<void> {
  const options = await fetchCreationOptions(token);
  const credential = (await navigator.credentials.create(options)) as PublicKeyCredential | null;
  if (!credential) {
    throw new Error('The browser did not return a passkey credential.');
  }
  const response = credential.response as AuthenticatorAttestationResponse;
  await webauthnRequest<{ success: boolean }>('/webauthn/register', token, {
    publicKey: {
      credential: {
        id: credential.id,
        rawId: bufferToBase64Url(credential.rawId),
        type: credential.type,
        response: {
          attestationObject: bufferToBase64Url(response.attestationObject),
          clientDataJSON: bufferToBase64Url(response.clientDataJSON),
          transports: response.getTransports ? response.getTransports() : [],
        },
        clientExtensionResults: credential.getClientExtensionResults(),
        authenticatorAttachment: credential.authenticatorAttachment ?? undefined,
      },
      label,
    },
  });
}

/**
 * Runs the full passkey enrollment ceremony (navigator.credentials.create() against Spring's
 * options/register endpoints), then confirms it with this app's own endpoint to activate WebAuthn
 * as the account's MFA method and issue the fresh recovery-code batch — the WebAuthn counterpart
 * to confirmTotpSetup, just with no code parameter since the ceremony itself is the proof.
 */
export async function enrollWebauthn(pendingToken: string): Promise<MfaEnrollmentResult> {
  await registerCredential(pendingToken, 'Passkey');
  return request<MfaEnrollmentResult>('/auth/mfa/setup/webauthn/confirm', { method: 'POST' }, pendingToken);
}

/**
 * Self-service "add a backup passkey" (ADR-0111 follow-up) — same ceremony as enrollment, but
 * driven by the ambient session token instead of a pending one, and with no confirm call
 * afterward: mfaMethod is already WEBAUTHN, so there's nothing left to activate.
 * WebAuthnCredentialRepositoryAdapter on the backend requires this session to already carry the
 * MFA factor before it'll actually persist a second credential.
 */
export async function addWebauthnCredential(label = 'Passkey'): Promise<void> {
  const token = getToken();
  if (!token) {
    throw new ApiError(401, 'Not authenticated');
  }
  await registerCredential(token, label);
}

/** Runs the passkey login-challenge ceremony and returns a fully authenticated token on success. */
export async function challengeWebauthn(pendingToken: string): Promise<TokenResponse> {
  const options = await fetchRequestOptions(pendingToken);
  const credential = (await navigator.credentials.get(options)) as PublicKeyCredential | null;
  if (!credential) {
    throw new Error('The browser did not return a passkey credential.');
  }
  const response = credential.response as AuthenticatorAssertionResponse;
  return webauthnRequest<TokenResponse>('/login/webauthn', pendingToken, {
    id: credential.id,
    rawId: bufferToBase64Url(credential.rawId),
    response: {
      authenticatorData: bufferToBase64Url(response.authenticatorData),
      clientDataJSON: bufferToBase64Url(response.clientDataJSON),
      signature: bufferToBase64Url(response.signature),
      userHandle: response.userHandle ? bufferToBase64Url(response.userHandle) : undefined,
    },
    clientExtensionResults: credential.getClientExtensionResults(),
    authenticatorAttachment: credential.authenticatorAttachment ?? undefined,
  });
}

// ---- Accounts (ADR-0109/ADR-0110): self-registration, roles, roster management ----

export type Role = 'ADMIN' | 'USER';

export type MfaMethod = 'NONE' | 'TOTP' | 'WEBAUTHN';

export interface Account {
  id: string;
  email: string;
  role: Role;
  enabled: boolean;
  mfaMethod: MfaMethod;
  createdAt: string;
}

interface RegistrationAccepted {
  message: string;
}

/**
 * Always resolves the same way whether or not the email was already taken
 * (anti-enumeration, ADR-0110) — the returned message never confirms which.
 */
export async function registerAccount(email: string, password: string): Promise<string> {
  const result = await request<RegistrationAccepted>('/accounts/register', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  return result.message;
}

export function getCurrentAccount(): Promise<Account> {
  return request<Account>('/accounts/me');
}

export function changeOwnPassword(currentPassword: string, newPassword: string): Promise<void> {
  return request<void>('/accounts/me/password', {
    method: 'PATCH',
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

export function logoutAllSessions(): Promise<void> {
  return request<void>('/accounts/me/logout-all', { method: 'POST' });
}

export interface WebAuthnCredentialSummary {
  id: string;
  label?: string | null;
  createdAt: string;
  lastUsedAt: string;
  transports: string[];
}

/** Self-service passkey management (ADR-0111 follow-up) — adding one is `addWebauthnCredential` above. */
export const webauthnCredentialsApi = {
  list: () => request<WebAuthnCredentialSummary[]>('/accounts/me/webauthn-credentials'),
  remove: (id: string) => request<void>(`/accounts/me/webauthn-credentials/${id}`, { method: 'DELETE' }),
};

export interface RecoveryCodeStatus {
  remaining: number;
}

/** Self-service recovery-code visibility/regeneration (ADR-0111 follow-up) — works for either MFA method. */
export const recoveryCodesApi = {
  status: () => request<RecoveryCodeStatus>('/accounts/me/recovery-codes'),
  /** Shown only in this response — the caller must display them once and move on. */
  regenerate: () => request<string[]>('/accounts/me/recovery-codes/regenerate', { method: 'POST' }),
};

/**
 * Self-service TOTP secret replacement for an account already on TOTP (ADR-0111 follow-up,
 * e.g. a new phone) — mirrors startTotpSetup/confirmTotpSetup's shape but uses the ambient
 * session token (not a pending one). Confirm reissues no recovery codes (the standing batch is
 * unaffected) but DOES reissue the session token: the swap bumps tokenVersion to invalidate any
 * other outstanding token for this account (e.g. a lost/stolen device's still-live session — the
 * whole point of this flow), so the caller must call setToken() with the response to keep their
 * own in-flight session working past this call.
 */
export function startTotpReEnrollment(): Promise<TotpSetupStart> {
  return request<TotpSetupStart>('/accounts/me/totp/start', { method: 'POST' });
}

export function confirmTotpReEnrollment(code: string): Promise<TokenResponse> {
  return request<TokenResponse>('/accounts/me/totp/confirm', { method: 'POST', body: JSON.stringify({ code }) });
}

export interface AccountSessionSummary {
  id: string;
  createdAt: string;
  userAgent?: string | null;
  ipAddress?: string | null;
  expiresAt: string;
  current: boolean;
}

/** Self-service session/device tracking (ADR-0112) — a second, finer-grained layer alongside logout-all. */
export const accountSessionsApi = {
  list: () => request<AccountSessionSummary[]>('/accounts/me/sessions'),
  revoke: (id: string) => request<void>(`/accounts/me/sessions/${id}`, { method: 'DELETE' }),
};

/** Admin-only account roster management. */
export const accountsApi = {
  list: () => request<Account[]>('/accounts'),
  updateRole: (id: string, role: Role) =>
    request<Account>(`/accounts/${id}/role`, { method: 'PATCH', body: JSON.stringify({ role }) }),
  disable: (id: string) => request<Account>(`/accounts/${id}/disable`, { method: 'POST' }),
  enable: (id: string) => request<Account>(`/accounts/${id}/enable`, { method: 'POST' }),
  resetPassword: (id: string, newPassword: string) =>
    request<void>(`/accounts/${id}/reset-password`, {
      method: 'POST',
      body: JSON.stringify({ newPassword }),
    }),
  /** Forces re-enrollment (ADR-0111) — the recovery path for a lost device or a suspicious enrollment. */
  resetMfa: (id: string) => request<Account>(`/accounts/${id}/reset-mfa`, { method: 'POST' }),
  remove: (id: string) => request<void>(`/accounts/${id}`, { method: 'DELETE' }),
};

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
  /** Structural parent for sidebar nesting; independent of categoryId (ADR-0080). */
  parentArticleId?: string | null;
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
  parentArticleId?: string | null;
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
  type: 'BEAT' | 'MAP_PIN' | 'TIMELINE_EVENT' | 'RELATIONSHIP' | 'CHARACTER_SHEET' | 'STATBLOCK' | 'ARTICLE_LINK' | 'CHILD_ARTICLE';
  label: string;
  targetId?: string | null;
  campaignId?: string | null;
  campaignName?: string | null;
}

export function articlesApi(worldId: string) {
  const base = `/worlds/${worldId}/articles`;
  return {
    list: (params?: { categoryId?: string; q?: string; campaignId?: string; tag?: string }) => {
      const search = new URLSearchParams();
      if (params?.categoryId) search.set('categoryId', params.categoryId);
      if (params?.q) search.set('q', params.q);
      if (params?.campaignId) search.set('campaignId', params.campaignId);
      if (params?.tag) search.set('tag', params.tag);
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

/** Manual auto-link scan/apply (ADR-0116) - article bodies only. */
export function autolinkApi(worldId: string) {
  const base = `/worlds/${worldId}/articles`;
  return {
    scan: () => request<AutolinkCandidateGroup[]>(`${base}/autolink-candidates`),
    apply: (articleId: string, selections: AutolinkSelection[]) =>
      request<Article>(`${base}/${articleId}/autolink`, {
        method: 'POST',
        body: JSON.stringify({ selections }),
      }),
  };
}

export interface CategoryRequest {
  name: string;
  parentId?: string | null;
}

export function categoriesApi(worldId: string) {
  const base = `/worlds/${worldId}/categories`;
  return {
    list: () => request<Category[]>(base),
    create: (body: CategoryRequest) =>
      request<Category>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: CategoryRequest) =>
      request<Category>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export interface BrokenLink {
  sourceType: 'ARTICLE' | 'BEAT' | 'ROLL_TABLE' | 'CARD_DECK';
  sourceId: string;
  sourceLabel: string;
  target: string;
}

export interface ConsistencyArticle {
  articleId: string;
  title: string;
}

export interface ConsistencyReport {
  brokenLinks: BrokenLink[];
  orphanedArticles: ConsistencyArticle[];
  unreferencedByCampaigns: ConsistencyArticle[];
}

export interface AutolinkMatch {
  targetArticleId: string;
  /** Every name-form the target article is known by - canonical title first,
   * then aliases - offered as choices for which one to write as the link's
   * target, regardless of which one textually matched (ADR-0116). */
  candidateNames: string[];
  matchedText: string;
  occurrenceIndex: number;
  snippet: string;
}

export interface AutolinkCandidateGroup {
  articleId: string;
  articleTitle: string;
  matches: AutolinkMatch[];
}

export interface AutolinkSelection {
  targetArticleId: string;
  occurrenceIndex: number;
  chosenName: string;
}

/** FR-43: read-only world lint over the same machinery as the usage panel. */
export function consistencyApi(worldId: string) {
  return {
    report: () => request<ConsistencyReport>(`/worlds/${worldId}/consistency-report`),
  };
}

export type HandoutPreset = 'PARCHMENT' | 'NEWSPAPER' | 'POSTER' | 'LETTER';

export interface Handout {
  id: string;
  worldId: string;
  categoryId?: string | null;
  title: string;
  preset: HandoutPreset;
  body?: string | null;
  sessionId?: string | null;
  revealed: boolean;
  createdAt: string;
  updatedAt: string;
}

interface HandoutRequestBody {
  categoryId?: string | null;
  title: string;
  preset: HandoutPreset;
  body?: string | null;
  sessionId?: string | null;
  revealed?: boolean;
}

export interface HandoutCategory {
  id: string;
  worldId: string;
  parentId?: string | null;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface HandoutCategoryRequest {
  name: string;
  parentId?: string | null;
}

/** FR-46: player-facing styled one-page printables. */
export function handoutsApi(worldId: string) {
  const base = `/worlds/${worldId}/handouts`;
  return {
    list: () => request<Handout[]>(base),
    get: (id: string) => request<Handout>(`${base}/${id}`),
    create: (body: HandoutRequestBody) =>
      request<Handout>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: HandoutRequestBody) =>
      request<Handout>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
    reorder: (orderedIds: string[]) =>
      request<Handout[]>(`${base}/order`, { method: 'PUT', body: JSON.stringify({ orderedIds }) }),
    duplicate: (id: string) => request<Handout>(`${base}/${id}/duplicate`, { method: 'POST' }),
  };
}

/** ADR-0105: a handout-only taxonomy, separate from Wiki's. */
export function handoutCategoriesApi(worldId: string) {
  const base = `/worlds/${worldId}/handout-categories`;
  return {
    list: () => request<HandoutCategory[]>(base),
    create: (body: HandoutCategoryRequest) =>
      request<HandoutCategory>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: HandoutCategoryRequest) =>
      request<HandoutCategory>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export interface ArticleTemplateSection {
  heading: string;
  hint: string;
}

export interface ArticleTemplateInfo {
  template: ArticleTemplate;
  label: string;
  sections: ArticleTemplateSection[];
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
  categoryId?: string | null;
  name: string;
  mediaId?: string | null;
  imageUrl?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MapRequest {
  name: string;
  mediaId: string;
  categoryId?: string | null;
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

/** A world's map category taxonomy (ADR-0105) — separate from Wiki's `categoriesApi`. */
export interface MapCategory {
  id: string;
  worldId: string;
  parentId?: string | null;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface MapCategoryRequest {
  name: string;
  parentId?: string | null;
}

export function mapCategoriesApi(worldId: string) {
  const base = `/worlds/${worldId}/map-categories`;
  return {
    list: () => request<MapCategory[]>(base),
    create: (body: MapCategoryRequest) =>
      request<MapCategory>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: MapCategoryRequest) =>
      request<MapCategory>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export interface LayerStyle {
  color?: string | null;
  icon?: string | null;
}

export function layerStylesApi(worldId: string) {
  const base = `/worlds/${worldId}/layer-styles`;
  return {
    get: () => request<Record<string, LayerStyle>>(base),
    put: (styles: Record<string, LayerStyle>) =>
      request<Record<string, LayerStyle>>(base, { method: 'PUT', body: JSON.stringify(styles) }),
  };
}

export interface FoundryConnection {
  relayBaseUrl: string;
  clientId: string;
  configured: boolean;
}

export interface FoundryConnectionRequest {
  relayBaseUrl: string;
  clientId: string;
  /** Blank keeps the currently stored key — never re-send a key you didn't just type. */
  apiKey: string;
}

export interface FoundryConnectionTestResult {
  ok: boolean;
  connectedClientIds: string[];
  error: string | null;
}

/** Per-world Foundry relay connection — always user-supplied, never a shared default. */
export function foundryApi(worldId: string) {
  const base = `/worlds/${worldId}/foundry-connection`;
  return {
    get: () => request<FoundryConnection>(base),
    put: (body: FoundryConnectionRequest) =>
      request<FoundryConnection>(base, { method: 'PUT', body: JSON.stringify(body) }),
    test: () => request<FoundryConnectionTestResult>(`${base}/test`, { method: 'POST' }),
  };
}

export interface FoundryPushResult {
  foundryDocumentId: string;
  pushedAt: string;
  warnings: string[];
}

export interface FoundryPushStatus {
  pushed: boolean;
  foundryDocumentId: string | null;
  pushedAt: string | null;
}

export interface FoundrySessionPushResult {
  articlesPushed: number;
  handoutsPushed: number;
  rollTablesPushed: number;
  cardDecksPushed: number;
  sessionGuideDocumentId: string;
  beatsIncluded: number;
  warnings: string[];
}

export type FoundryCategoryPushMode = 'FOLDER' | 'SINGLE_DOCUMENT';

export interface FoundryCategoryPushResult {
  foundryDocumentId: string | null;
  pushedAt: string;
  articlesPushed: number;
  warnings: string[];
}

export interface FoundryCampaignPushResult {
  sessionsPushed: number;
  articlesPushed: number;
  handoutsPushed: number;
  rollTablesPushed: number;
  cardDecksPushed: number;
  sessionGuidesCreated: number;
  warnings: string[];
}

/** Push Campaign Organizer content into a world's connected Foundry session (ADR-0115). */
export function foundryPushApi(worldId: string) {
  const base = `/worlds/${worldId}/foundry`;
  return {
    pushArticle: (articleId: string) =>
      request<FoundryPushResult>(`${base}/articles/${articleId}/push`, { method: 'POST' }),
    articlePushStatus: (articleId: string) =>
      request<FoundryPushStatus>(`${base}/articles/${articleId}/push-status`),
    pushHandout: (handoutId: string) =>
      request<FoundryPushResult>(`${base}/handouts/${handoutId}/push`, { method: 'POST' }),
    handoutPushStatus: (handoutId: string) =>
      request<FoundryPushStatus>(`${base}/handouts/${handoutId}/push-status`),
    pushRollTable: (rollTableId: string) =>
      request<FoundryPushResult>(`${base}/roll-tables/${rollTableId}/push`, { method: 'POST' }),
    rollTablePushStatus: (rollTableId: string) =>
      request<FoundryPushStatus>(`${base}/roll-tables/${rollTableId}/push-status`),
    pushCardDeck: (cardDeckId: string) =>
      request<FoundryPushResult>(`${base}/card-decks/${cardDeckId}/push`, { method: 'POST' }),
    cardDeckPushStatus: (cardDeckId: string) =>
      request<FoundryPushStatus>(`${base}/card-decks/${cardDeckId}/push-status`),
    pushSession: (campaignId: string, sessionId: string) =>
      request<FoundrySessionPushResult>(`${base}/campaigns/${campaignId}/sessions/${sessionId}/push`, {
        method: 'POST',
      }),
    pushCampaign: (campaignId: string) =>
      request<FoundryCampaignPushResult>(`${base}/campaigns/${campaignId}/push`, { method: 'POST' }),
    pushCategory: (categoryId: string, mode: FoundryCategoryPushMode) =>
      request<FoundryCategoryPushResult>(`${base}/categories/${categoryId}/push?mode=${mode}`, {
        method: 'POST',
      }),
    categoryPushStatus: (categoryId: string) =>
      request<FoundryPushStatus>(`${base}/categories/${categoryId}/push-status`),
    pushWorldWiki: () => request<FoundryCategoryPushResult>(`${base}/wiki/push`, { method: 'POST' }),
    worldWikiPushStatus: () => request<FoundryPushStatus>(`${base}/wiki/push-status`),
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

export type CampaignStatus = 'PLANNED' | 'ACTIVE' | 'ON_HIATUS' | 'COMPLETED';
export const CAMPAIGN_STATUSES: CampaignStatus[] = ['PLANNED', 'ACTIVE', 'ON_HIATUS', 'COMPLETED'];

export interface Campaign {
  id: string;
  worldId: string;
  name: string;
  description?: string | null;
  notes?: string | null;
  status: CampaignStatus;
  systemId?: string | null;
  color?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CampaignRequest {
  name: string;
  description?: string | null;
  notes?: string | null;
  status?: CampaignStatus;
  systemId?: string | null;
  color?: string | null;
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
  statblockIds: string[];
  encounterIds: string[];
  tableIds: string[];
  deckIds: string[];
  sessionId?: string | null;
  /** Optional GM-defined beat kind tag (ADR-0101), informational only. */
  kindId?: string | null;
  position: number;
  createdAt: string;
  updatedAt: string;
}

export interface BeatRequest {
  title: string;
  body?: string | null;
  done?: boolean;
  articleIds?: string[];
  statblockIds?: string[];
  encounterIds?: string[];
  tableIds?: string[];
  deckIds?: string[];
  sessionId?: string | null;
  kindId?: string | null;
  position?: number | null;
}

export interface ClockSegment {
  filled: boolean;
  title?: string | null;
  description?: string | null;
}

export interface Clock {
  id: string;
  campaignId: string;
  title: string;
  description?: string | null;
  segments: ClockSegment[];
  position: number;
  createdAt: string;
  updatedAt: string;
}

export interface ClockRequest {
  title: string;
  description?: string | null;
  segments: ClockSegment[];
  position?: number | null;
}

/**
 * No HP/resource override on purpose - not every system tracks HP
 * (Forbidden Lands, Vaesen, ...). Whatever a combatant's trackable
 * resource is stays live/auto-detected and editable at print time
 * (EncounterSheetView, ADR-0069), not persisted schema.
 */
export interface EncounterEntry {
  statblockId: string;
  quantity: number;
}

/** A named, reusable, printable grouping of statblocks (ADR-0097). Linked to beats via ArcBeat.encounterIds. */
export interface Encounter {
  id: string;
  campaignId: string;
  name: string;
  notes?: string | null;
  entries: EncounterEntry[];
  createdAt: string;
  updatedAt: string;
}

export interface EncounterRequest {
  name: string;
  notes?: string | null;
  entries: EncounterEntry[];
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

// ---- Campaign .ics export (ADR-0108): download + subscribe feed ----

export interface CalendarFeedToken {
  token: string;
}

export function campaignCalendarApi(worldId: string, campaignId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}`;
  return {
    getOrCreateFeed: () => request<CalendarFeedToken>(`${base}/calendar-feed`),
    regenerateFeed: () =>
      request<CalendarFeedToken>(`${base}/calendar-feed/regenerate`, { method: 'POST' }),
    downloadIcs: () => exportCampaignIcs(worldId, campaignId),
  };
}

/** Downloads a campaign's dated sessions as an .ics file (ADR-0108). */
export async function exportCampaignIcs(worldId: string, campaignId: string): Promise<void> {
  const token = getToken();
  const response = await fetch(`/api/worlds/${worldId}/campaigns/${campaignId}/calendar.ics`, {
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
  const filename = match ? match[1] : `campaign-${campaignId}.ics`;
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

// ---- Players (FR-53): a world-scoped, reusable pool shared across campaigns ----

export interface Player {
  id: string;
  worldId: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface PlayerRequest {
  name: string;
}

export function playersApi(worldId: string) {
  const base = `/worlds/${worldId}/players`;
  return {
    list: () => request<Player[]>(base),
    get: (id: string) => request<Player>(`${base}/${id}`),
    create: (body: PlayerRequest) =>
      request<Player>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: PlayerRequest) =>
      request<Player>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

// ---- World overview stats (FR-62): read-only, consumed by the Overview dashboard ----

export interface RecentlyEditedArticle {
  articleId: string;
  title: string;
  updatedAt: string;
}

export interface NextSessionSummary {
  sessionId: string;
  campaignId: string;
  campaignName: string;
  title: string;
  date: string;
  sessionNumber?: number | null;
}

export interface ClockSummary {
  clockId: string;
  campaignId: string;
  campaignName: string;
  title: string;
  filledSegments: number;
  totalSegments: number;
}

export interface LooseThreadSummary {
  threadId: string;
  campaignId: string;
  campaignName: string;
  text: string;
}

export interface ScheduledSessionSummary {
  sessionId: string;
  campaignId: string;
  campaignName: string;
  campaignColor?: string | null;
  title: string;
  sessionNumber?: number | null;
  date: string;
}

export interface WorldOverviewStats {
  articleCount: number;
  sessionsRunCount: number;
  recentlyEdited: RecentlyEditedArticle[];
  nextSession?: NextSessionSummary | null;
  openClocks: ClockSummary[];
  openLooseThreads: LooseThreadSummary[];
  scheduledSessions: ScheduledSessionSummary[];
}

export function worldOverviewApi(worldId: string) {
  return {
    get: () => request<WorldOverviewStats>(`/worlds/${worldId}/overview`),
  };
}

// ---- Campaign overview dashboard (issue #67) ----

export interface CampaignNextSessionSummary {
  sessionId: string;
  title: string;
  date: string;
  sessionNumber?: number | null;
}

export interface ClockPrepHint {
  clockId: string;
  title: string;
  filledSegments: number;
  totalSegments: number;
  nextUnfilledSegmentTitle?: string | null;
}

export interface CampaignLooseThreadSummary {
  threadId: string;
  text: string;
}

export interface OpenBeatSummary {
  beatId: string;
  arcId: string;
  arcTitle: string;
  beatTitle: string;
}

export interface CampaignTodoSummary {
  todoId: string;
  text: string;
}

export interface CampaignOverviewStats {
  nextSession?: CampaignNextSessionSummary | null;
  openClocksNearFilling: ClockPrepHint[];
  openLooseThreads: CampaignLooseThreadSummary[];
  openBeatsInActiveArcs: OpenBeatSummary[];
  nextSessionTodos: CampaignTodoSummary[];
}

export function campaignOverviewApi(worldId: string, campaignId: string) {
  return {
    get: () => request<CampaignOverviewStats>(`/worlds/${worldId}/campaigns/${campaignId}/overview`),
  };
}

// ---- Global landing page (issue #68) ----

export interface UpcomingSessionSummary {
  sessionId: string;
  worldId: string;
  worldName: string;
  campaignId: string;
  campaignName: string;
  campaignColor?: string | null;
  title: string;
  sessionNumber?: number | null;
  date: string;
}

export interface CampaignNeedingAttention {
  worldId: string;
  worldName: string;
  campaignId: string;
  campaignName: string;
  status: CampaignStatus;
}

export interface GlobalOverviewStats {
  upcomingSessions: UpcomingSessionSummary[];
  campaignsNeedingAttention: CampaignNeedingAttention[];
}

export function globalOverviewApi() {
  return {
    get: () => request<GlobalOverviewStats>('/overview'),
  };
}

// ---- Campaign roster (FR-53): whole-set replace, players flagged regular/guest ----

export interface RosterEntry {
  playerId: string;
  name: string;
  guest: boolean;
}

export interface RosterEntryInput {
  playerId: string;
  guest?: boolean;
}

export function campaignRosterApi(worldId: string, campaignId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/roster`;
  return {
    get: () => request<RosterEntry[]>(base),
    put: (entries: RosterEntryInput[]) =>
      request<RosterEntry[]>(base, { method: 'PUT', body: JSON.stringify({ entries }) }),
  };
}

// ---- Session attendance (FR-53): pre-populated from the roster, whole-set replace ----

export interface AttendanceEntry {
  playerId: string;
  name: string;
  guest: boolean;
  present: boolean;
  characterId?: string | null;
  characterName?: string | null;
}

export interface AttendanceEntryInput {
  playerId: string;
  present: boolean;
  characterId?: string | null;
}

export function sessionAttendanceApi(worldId: string, campaignId: string, sessionId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/sessions/${sessionId}/attendance`;
  return {
    get: () => request<AttendanceEntry[]>(base),
    put: (entries: AttendanceEntryInput[]) =>
      request<AttendanceEntry[]>(base, { method: 'PUT', body: JSON.stringify({ entries }) }),
  };
}

export interface PacketBeat {
  id: string;
  title: string;
  body?: string | null;
  done: boolean;
  arcTitle?: string | null;
  articleIds: string[];
}

export interface PacketArticle {
  id: string;
  title: string;
  template: string;
  parentArticleId?: string | null;
  bodyHtml?: string | null;
}

export interface PacketPin {
  x: number;
  y: number;
  label?: string | null;
}

export interface PacketMap {
  id: string;
  name: string;
  imageUrl?: string | null;
  pins: PacketPin[];
}

export interface PacketRollTableEntry {
  minResult?: number | null;
  maxResult?: number | null;
  bodyHtml: string;
}

export interface PacketRollTable {
  id: string;
  title: string;
  diceExpression: string;
  minResult: number;
  maxResult: number;
  entries: PacketRollTableEntry[];
}

export interface PacketDeckCard {
  title?: string | null;
  bodyHtml: string;
}

export interface PacketCardDeck {
  id: string;
  title: string;
  cards: PacketDeckCard[];
}

export interface PacketHandout {
  id: string;
  title: string;
  preset: HandoutPreset;
  body: string;
}

/** No `filled` field - the packet prints a blank diagram for hand-marking (ADR-0084). */
export interface PacketClockSegment {
  title?: string | null;
  description?: string | null;
}

export interface PacketClock {
  id: string;
  title: string;
  description?: string | null;
  segments: PacketClockSegment[];
}

/** Resolved fresh from its source at print time, same as the standalone cheat sheet (ADR-0086). */
export interface PacketCheatSheetFragment {
  type: CheatSheetFragmentType;
  missing: boolean;
  text?: string | null;
  statblock?: Statblock | null;
  tableTitle?: string | null;
  tableEntry?: PacketRollTableEntry | null;
  deckTitle?: string | null;
  deckCard?: PacketDeckCard | null;
}

export interface PacketCheatSheet {
  id: string;
  fragments: PacketCheatSheetFragment[];
}

export interface SessionPacket {
  session: Session;
  campaignName: string;
  beats: PacketBeat[];
  articles: PacketArticle[];
  maps: PacketMap[];
  statblocks: Statblock[];
  rollTables: PacketRollTable[];
  cardDecks: PacketCardDeck[];
  handouts: PacketHandout[];
  clocks: PacketClock[];
  cheatSheet?: PacketCheatSheet | null;
}

export function sessionsApi(worldId: string, campaignId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/sessions`;
  return {
    list: () => request<Session[]>(base),
    packet: (id: string) => request<SessionPacket>(`${base}/${id}/packet`),
    create: (body: SessionRequest) =>
      request<Session>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: SessionRequest) =>
      request<Session>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

// ---- Session cheat sheet (FR-37): one condensed, ordered sheet per session ----

export type CheatSheetFragmentType = 'FREEFORM' | 'STATBLOCK' | 'TABLE_ROW' | 'DECK_CARD';

export interface CheatSheetFragmentInput {
  type: CheatSheetFragmentType;
  text?: string | null;
  statblockId?: string | null;
  tableId?: string | null;
  entryId?: string | null;
  deckId?: string | null;
  cardId?: string | null;
}

export interface CheatSheetFragment extends CheatSheetFragmentInput {
  id: string | null;
}

/** `id: null` means no sheet has been saved for the session yet. */
export interface CheatSheet {
  id: string | null;
  sessionId: string;
  fragments: CheatSheetFragment[];
  createdAt: string | null;
  updatedAt: string | null;
}

export function cheatSheetsApi(worldId: string, campaignId: string, sessionId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/sessions/${sessionId}/cheat-sheet`;
  return {
    get: () => request<CheatSheet>(base),
    put: (fragments: CheatSheetFragmentInput[]) =>
      request<CheatSheet>(base, { method: 'PUT', body: JSON.stringify({ fragments }) }),
    remove: () => request<void>(base, { method: 'DELETE' }),
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

export function clocksApi(worldId: string, campaignId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/clocks`;
  return {
    list: () => request<Clock[]>(base),
    create: (body: ClockRequest) =>
      request<Clock>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: ClockRequest) =>
      request<Clock>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function encountersApi(worldId: string, campaignId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/encounters`;
  return {
    list: () => request<Encounter[]>(base),
    create: (body: EncounterRequest) =>
      request<Encounter>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: EncounterRequest) =>
      request<Encounter>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
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

// ---- Beat kinds (ADR-0101): a world-scoped, GM-defined catalog a beat can optionally be tagged with ----

export interface BeatKind {
  id: string;
  worldId: string;
  name: string;
  color?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BeatKindRequest {
  name: string;
  color?: string | null;
}

export function beatKindsApi(worldId: string) {
  const base = `/worlds/${worldId}/beat-kinds`;
  return {
    list: () => request<BeatKind[]>(base),
    get: (id: string) => request<BeatKind>(`${base}/${id}`),
    create: (body: BeatKindRequest) =>
      request<BeatKind>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: BeatKindRequest) =>
      request<BeatKind>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export type LooseThreadStatus = 'OPEN' | 'RESOLVED' | 'ABANDONED';
export const LOOSE_THREAD_STATUSES: LooseThreadStatus[] = ['OPEN', 'RESOLVED', 'ABANDONED'];

export interface LooseThread {
  id: string;
  sessionId: string;
  campaignId: string;
  text: string;
  status: LooseThreadStatus;
  createdAt: string;
  updatedAt: string;
}

export interface LooseThreadRequest {
  text: string;
  status?: LooseThreadStatus;
}

export function looseThreadsApi(worldId: string, campaignId: string, sessionId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/sessions/${sessionId}/loose-threads`;
  return {
    list: () => request<LooseThread[]>(base),
    create: (body: LooseThreadRequest) =>
      request<LooseThread>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: LooseThreadRequest) =>
      request<LooseThread>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

/** Null sessionId means a standing campaign-level todo (ADR-0092). */
export interface Todo {
  id: string;
  campaignId: string;
  sessionId: string | null;
  text: string;
  done: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TodoRequest {
  text: string;
}

export interface TodoUpdateRequest {
  text: string;
  done: boolean;
}

/** Standing campaign todos, plus the shared update/delete route for any todo. */
export function campaignTodosApi(worldId: string, campaignId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/todos`;
  return {
    list: () => request<Todo[]>(base),
    create: (body: TodoRequest) => request<Todo>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: TodoUpdateRequest) =>
      request<Todo>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

/** A session's todos; update/delete for these rows go through campaignTodosApi. */
export function sessionTodosApi(worldId: string, campaignId: string, sessionId: string) {
  const base = `/worlds/${worldId}/campaigns/${campaignId}/sessions/${sessionId}/todos`;
  return {
    list: () => request<Todo[]>(base),
    create: (body: TodoRequest) => request<Todo>(base, { method: 'POST', body: JSON.stringify(body) }),
  };
}

// ---- Character sheets, statblocks, dice (mirrors docs/api/openapi.yaml) ----

export type TemplateKind = 'CHARACTER' | 'STATBLOCK' | 'DOCUMENT';

export type FieldType = 'TEXT' | 'TEXTAREA' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'CIRCLES';

export type FieldWidth = 'FULL' | 'HALF' | 'THIRD' | 'QUARTER';

export interface TemplateField {
  key: string;
  label: string;
  type: FieldType;
  options?: string[] | null;
  width?: FieldWidth | null;
  count?: number | null;
}

export interface TemplateSection {
  title: string;
  fields: TemplateField[];
}

export interface FieldTemplate {
  id: string;
  worldId: string;
  categoryId?: string | null;
  name: string;
  kind: TemplateKind;
  systemId?: string | null;
  sections: TemplateSection[];
  createdAt: string;
  updatedAt: string;
}

export interface FieldTemplateRequest {
  categoryId?: string | null;
  name: string;
  kind: TemplateKind;
  systemId?: string | null;
  sections: TemplateSection[];
}

export interface BuiltinFieldTemplate {
  name: string;
  kind: TemplateKind;
  system?: string | null;
  sections: TemplateSection[];
}

/** A real, top-level, world-independent game system entity (ADR-0094, ADR-0095). */
export interface GameSystem {
  id: string;
  name: string;
  tagline?: string | null;
  color?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface GameSystemRequest {
  name: string;
  tagline?: string | null;
  color?: string | null;
  notes?: string | null;
}

/** World-independent, system-scoped template catalog (ADR-0093). CHARACTER/STATBLOCK kinds only. */
export interface GlobalFieldTemplate {
  id: string;
  name: string;
  kind: TemplateKind;
  systemId: string;
  sections: TemplateSection[];
  createdAt: string;
  updatedAt: string;
}

export interface GlobalFieldTemplateRequest {
  name: string;
  kind: TemplateKind;
  systemId: string;
  sections: TemplateSection[];
}

/**
 * World-independent, system-scoped statblock catalog (ADR-0096). Importing
 * one into a campaign copies its stats/notes into a new world-scoped
 * Statblock — the copy carries no live link back to this catalog entry.
 */
export interface GlobalStatblock {
  id: string;
  systemId: string;
  globalTemplateId?: string | null;
  name: string;
  stats: Record<string, unknown>;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface GlobalStatblockRequest {
  name: string;
  systemId: string;
  globalTemplateId?: string | null;
  stats?: Record<string, unknown>;
  notes?: string | null;
}

export interface ImportGlobalStatblockRequest {
  worldId: string;
  campaignId: string;
  name?: string;
}

export interface CharacterSheet {
  id: string;
  worldId: string;
  categoryId?: string | null;
  worldTemplateId: string | null;
  globalTemplateId: string | null;
  articleId?: string | null;
  campaignId?: string | null;
  name: string;
  values: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface CharacterSheetRequest {
  categoryId?: string | null;
  name: string;
  worldTemplateId?: string | null;
  globalTemplateId?: string | null;
  articleId?: string | null;
  campaignId?: string | null;
  values?: Record<string, unknown>;
}

export interface Document {
  id: string;
  worldId: string;
  categoryId?: string | null;
  templateId: string;
  campaignId?: string | null;
  name: string;
  values: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentRequest {
  categoryId?: string | null;
  name: string;
  templateId: string;
  campaignId?: string | null;
  values?: Record<string, unknown>;
}

export interface Statblock {
  id: string;
  worldId: string;
  categoryId?: string | null;
  articleId?: string | null;
  campaignId?: string | null;
  worldTemplateId?: string | null;
  globalTemplateId?: string | null;
  name: string;
  stats: Record<string, unknown>;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface StatblockRequest {
  categoryId?: string | null;
  name: string;
  articleId?: string | null;
  campaignId?: string | null;
  worldTemplateId?: string | null;
  globalTemplateId?: string | null;
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

export function fieldTemplatesApi(worldId: string) {
  const base = `/worlds/${worldId}/field-templates`;
  return {
    list: (kind?: TemplateKind) => request<FieldTemplate[]>(kind ? `${base}?kind=${kind}` : base),
    get: (id: string) => request<FieldTemplate>(`${base}/${id}`),
    create: (body: FieldTemplateRequest) =>
      request<FieldTemplate>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: FieldTemplateRequest) =>
      request<FieldTemplate>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
    duplicate: (id: string) =>
      request<FieldTemplate>(`${base}/${id}/duplicate`, { method: 'POST' }),
    promote: (id: string) =>
      request<GlobalFieldTemplate>(`${base}/${id}/promote`, { method: 'POST' }),
  };
}

export const builtinFieldTemplatesApi = {
  list: (kind?: TemplateKind) =>
    request<BuiltinFieldTemplate[]>(kind ? `/field-templates/builtin?kind=${kind}` : '/field-templates/builtin'),
};

/** World-independent CRUD for game systems (ADR-0094). */
export const gameSystemsApi = {
  list: () => request<GameSystem[]>('/game-systems'),
  get: (id: string) => request<GameSystem>(`/game-systems/${id}`),
  create: (body: GameSystemRequest) =>
    request<GameSystem>('/game-systems', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: string, body: GameSystemRequest) =>
    request<GameSystem>(`/game-systems/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  remove: (id: string) => request<void>(`/game-systems/${id}`, { method: 'DELETE' }),
};

/** World-independent CRUD for the global template catalog (ADR-0093). */
export const globalFieldTemplatesApi = {
  list: (kind?: TemplateKind) =>
    request<GlobalFieldTemplate[]>(kind ? `/field-templates/global?kind=${kind}` : '/field-templates/global'),
  get: (id: string) => request<GlobalFieldTemplate>(`/field-templates/global/${id}`),
  create: (body: GlobalFieldTemplateRequest) =>
    request<GlobalFieldTemplate>('/field-templates/global', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: string, body: GlobalFieldTemplateRequest) =>
    request<GlobalFieldTemplate>(`/field-templates/global/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  remove: (id: string) => request<void>(`/field-templates/global/${id}`, { method: 'DELETE' }),
};

/** World-independent CRUD + copy-on-import for the global statblock catalog (ADR-0096). */
export const globalStatblocksApi = {
  list: (systemId?: string) =>
    request<GlobalStatblock[]>(systemId ? `/statblocks/global?systemId=${systemId}` : '/statblocks/global'),
  get: (id: string) => request<GlobalStatblock>(`/statblocks/global/${id}`),
  create: (body: GlobalStatblockRequest) =>
    request<GlobalStatblock>('/statblocks/global', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: string, body: GlobalStatblockRequest) =>
    request<GlobalStatblock>(`/statblocks/global/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  remove: (id: string) => request<void>(`/statblocks/global/${id}`, { method: 'DELETE' }),
  import: (id: string, body: ImportGlobalStatblockRequest) =>
    request<Statblock>(`/statblocks/global/${id}/import`, { method: 'POST', body: JSON.stringify(body) }),
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

export function documentsApi(worldId: string) {
  const base = `/worlds/${worldId}/documents`;
  return {
    list: (params?: { campaignId?: string }) =>
      request<Document[]>(params?.campaignId ? `${base}?campaignId=${params.campaignId}` : base),
    get: (id: string) => request<Document>(`${base}/${id}`),
    create: (body: DocumentRequest) =>
      request<Document>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: DocumentRequest) =>
      request<Document>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function statblocksApi(worldId: string) {
  const base = `/worlds/${worldId}/statblocks`;
  return {
    list: (params?: { campaignId?: string; tag?: string }) => {
      const search = new URLSearchParams();
      if (params?.campaignId) search.set('campaignId', params.campaignId);
      if (params?.tag) search.set('tag', params.tag);
      const qs = search.toString();
      return request<Statblock[]>(qs ? `${base}?${qs}` : base);
    },
    get: (id: string) => request<Statblock>(`${base}/${id}`),
    create: (body: StatblockRequest) =>
      request<Statblock>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: StatblockRequest) =>
      request<Statblock>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
    duplicate: (id: string) => request<Statblock>(`${base}/${id}/duplicate`, { method: 'POST' }),
  };
}

export interface SheetCategory {
  id: string;
  worldId: string;
  parentId?: string | null;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface SheetCategoryRequest {
  name: string;
  parentId?: string | null;
}

/** ADR-0105: one shared taxonomy across sheets, statblocks, documents, and templates. */
export function sheetCategoriesApi(worldId: string) {
  const base = `/worlds/${worldId}/sheet-categories`;
  return {
    list: () => request<SheetCategory[]>(base),
    create: (body: SheetCategoryRequest) =>
      request<SheetCategory>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: SheetCategoryRequest) =>
      request<SheetCategory>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

// ---- Tags (ADR-0083, FR-47): freeform, world-scoped folksonomy tags ----

export interface EntityTags {
  tags: string[];
}

export interface TagBrowseResult {
  tag: string;
  articles: ArticleSummary[];
  statblocks: Statblock[];
}

function entityTagsApi(base: string) {
  return {
    get: () => request<EntityTags>(`${base}/tags`),
    set: (tags: string[]) =>
      request<EntityTags>(`${base}/tags`, { method: 'PUT', body: JSON.stringify({ tags }) }),
  };
}

export function articleTagsApi(worldId: string, articleId: string) {
  return entityTagsApi(`/worlds/${worldId}/articles/${articleId}`);
}

export function statblockTagsApi(worldId: string, statblockId: string) {
  return entityTagsApi(`/worlds/${worldId}/statblocks/${statblockId}`);
}

export interface ArticleAliases {
  aliases: string[];
}

/** Alternate names for an article (ADR-0116) - findable by the auto-link
 * scan and usable directly as a link target. */
export function articleAliasesApi(worldId: string, articleId: string) {
  const base = `/worlds/${worldId}/articles/${articleId}/aliases`;
  return {
    get: () => request<ArticleAliases>(base),
    set: (aliases: string[]) =>
      request<ArticleAliases>(base, { method: 'PUT', body: JSON.stringify({ aliases }) }),
  };
}

export function worldTagsApi(worldId: string) {
  return {
    list: () => request<string[]>(`/worlds/${worldId}/tags`),
  };
}

export function tagBrowseApi(worldId: string) {
  return {
    entities: (tagName: string) =>
      request<TagBrowseResult>(`/worlds/${worldId}/tags/${encodeURIComponent(tagName)}/entities`),
  };
}

export const diceApi = {
  roll: (expression: string) =>
    request<DiceRollResult>('/dice/roll', { method: 'POST', body: JSON.stringify({ expression }) }),
};

export type DraftLevel = 'QUICK_INSPIRATION' | 'READ_ALOUD' | 'BASIC_INFO' | 'FULL_DRAFT';

export const DRAFT_LEVELS: DraftLevel[] = [
  'QUICK_INSPIRATION',
  'READ_ALOUD',
  'BASIC_INFO',
  'FULL_DRAFT',
];

export interface DraftArticleTextResult {
  text: string;
  provider: string;
}

/** AI-assisted text drafting (ADR-0064, levels/kind per ADR-0075). Stateless; worldId is path-only. */
export function aiApi(worldId: string) {
  const base = `/worlds/${worldId}/ai`;
  return {
    draftArticleText: (
      instructions: string,
      existingContent: string,
      level: DraftLevel,
      template: ArticleTemplate,
    ) =>
      request<DraftArticleTextResult>(`${base}/draft-article-text`, {
        method: 'POST',
        body: JSON.stringify({ instructions, existingContent, level, template }),
      }),
    /** On-demand digest of a session's private GM notes (ADR-0082). Not persisted. */
    summarizeSessionNotes: (notes: string) =>
      request<DraftArticleTextResult>(`${base}/summarize-session-notes`, {
        method: 'POST',
        body: JSON.stringify({ notes }),
      }),
  };
}

export interface AiProviderSetting {
  providerId: string;
  model: string | null;
  defaultModel: string;
  configured: boolean;
  priority: number;
}

export interface AiProviderSettingInput {
  providerId: string;
  model: string | null;
}

export interface AiProviderTestResult {
  ok: boolean;
  model: string;
  latencyMs: number;
  error?: string | null;
}

/** Instance-global settings (ADR-0065) - no worldId. API keys stay env-only (NFR-7). */
export const aiSettingsApi = {
  get: () => request<AiProviderSetting[]>('/ai/settings'),
  update: (providers: AiProviderSettingInput[]) =>
    request<AiProviderSetting[]>('/ai/settings', { method: 'PUT', body: JSON.stringify({ providers }) }),
  /** Tiny round-trip to verify a provider actually works (Settings "Test" button). */
  test: (providerId: string) =>
    request<AiProviderTestResult>(`/ai/settings/${providerId}/test`, { method: 'POST' }),
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

/** Downloads a full instance backup (every world + media) as a ZIP (ADR-0061). */
export async function downloadBackup(): Promise<void> {
  const token = getToken();
  const response = await fetch('/api/backup', {
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
  const filename = match ? match[1] : 'campaign-organizer-backup.zip';
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

export type ImportMode = 'ADDITIVE' | 'OVERWRITE';

/** Imports a backup ZIP produced by {@link downloadBackup} (ADR-0061). */
export async function importBackup(file: File, mode: ImportMode): Promise<void> {
  const token = getToken();
  const body = new FormData();
  body.set('mode', mode);
  body.set('file', file);
  const response = await fetch('/api/backup/import', {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    body,
  });
  if (response.status === 401) {
    clearToken();
    throw new ApiError(401, 'Not authenticated');
  }
  if (!response.ok) {
    throw new ApiError(response.status, await safeProblemDetail(response));
  }
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

/** Downloads a document's filled fillable PDF. */
export async function exportDocumentPdf(worldId: string, documentId: string): Promise<void> {
  const token = getToken();
  const response = await fetch(`/api/worlds/${worldId}/documents/${documentId}/pdf`, {
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
  const filename = match ? match[1] : `document-${documentId}.pdf`;
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

export interface RollTableEntryInput {
  minResult?: number | null;
  maxResult?: number | null;
  body: string;
  nestedTableIds?: string[];
  nestedDeckIds?: string[];
}

export interface RollTableEntry {
  id: string;
  minResult?: number | null;
  maxResult?: number | null;
  body: string;
  nestedTableIds: string[];
  nestedDeckIds: string[];
}

export interface RollTable {
  id: string;
  worldId: string;
  categoryId?: string | null;
  title: string;
  description?: string | null;
  diceExpression: string;
  minResult: number;
  maxResult: number;
  entries: RollTableEntry[];
  createdAt: string;
  updatedAt: string;
}

export interface RollTableRequest {
  categoryId?: string | null;
  title: string;
  description?: string;
  diceExpression: string;
  entries: RollTableEntryInput[];
}

export function rollTablesApi(worldId: string) {
  const base = `/worlds/${worldId}/roll-tables`;
  return {
    list: () => request<RollTable[]>(base),
    create: (body: RollTableRequest) =>
      request<RollTable>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: RollTableRequest) =>
      request<RollTable>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
    duplicate: (id: string) => request<RollTable>(`${base}/${id}/duplicate`, { method: 'POST' }),
  };
}

export interface DeckCardInput {
  title?: string;
  body: string;
  nestedTableIds?: string[];
  nestedDeckIds?: string[];
}

export interface DeckCard {
  id: string;
  title?: string | null;
  body: string;
  nestedTableIds: string[];
  nestedDeckIds: string[];
}

export interface CardDeck {
  id: string;
  worldId: string;
  categoryId?: string | null;
  title: string;
  description?: string | null;
  cards: DeckCard[];
  createdAt: string;
  updatedAt: string;
}

export interface CardDeckRequest {
  categoryId?: string | null;
  title: string;
  description?: string;
  cards: DeckCardInput[];
}

export function cardDecksApi(worldId: string) {
  const base = `/worlds/${worldId}/card-decks`;
  return {
    list: () => request<CardDeck[]>(base),
    create: (body: CardDeckRequest) =>
      request<CardDeck>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: CardDeckRequest) =>
      request<CardDeck>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
    duplicate: (id: string) => request<CardDeck>(`${base}/${id}/duplicate`, { method: 'POST' }),
  };
}

export interface TableDeckCategory {
  id: string;
  worldId: string;
  parentId?: string | null;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface TableDeckCategoryRequest {
  name: string;
  parentId?: string | null;
}

/** ADR-0105: one shared taxonomy for both roll tables and card decks. */
export function tableDeckCategoriesApi(worldId: string) {
  const base = `/worlds/${worldId}/table-deck-categories`;
  return {
    list: () => request<TableDeckCategory[]>(base),
    create: (body: TableDeckCategoryRequest) =>
      request<TableDeckCategory>(base, { method: 'POST', body: JSON.stringify(body) }),
    update: (id: string, body: TableDeckCategoryRequest) =>
      request<TableDeckCategory>(`${base}/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    remove: (id: string) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}
