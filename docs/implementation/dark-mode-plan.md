# Dark Mode - Implementation Plan (Phase 1: Tokens + Shell)

## Objective

The frontend has zero theming infrastructure today: a single 90-line `src/styles.scss` with hardcoded hex colors
(`background: #f6f7fb`, `color: #1d2433`, `color-scheme: light` fixed in `:root`), no CSS custom properties
anywhere in the codebase, no Angular Material/Tailwind, and each of the 11 `*.component.scss` files repeats its
own hex codes independently - including an existing inconsistency where `private-layout.component.scss` uses a
different accent palette (`#174ea6`, `#111827`, `#dbe3ee`) than the rest of the app (`#2563eb`, `#1d2433`,
`#dce2ea`). This plan introduces a design-token layer and a light/dark toggle for the app shell and generic
building blocks (layout, panels, buttons, form fields, badges) - the surface a user sees on every page. It does
**not** migrate each feature page's own hardcoded colors; that happens in later, independent phases.

## Decisions Already Made

Confirmed with the product owner before drafting this plan:

- **Phased rollout.** Phase 1 (this plan) covers tokens + the app shell + shared classes already defined in
  `src/styles.scss` (`.panel`, `.button`, `.field input`, page title/subtitle). Each feature page's own `.scss`
  (dashboard, expenses, debts, budgets, income, catalogs, imports, accounts, members, profile, auth) is migrated
  in later phases, one page at a time. Until a page is migrated, it will look mostly light even when dark mode is
  active - an accepted, visible tradeoff of shipping incrementally.
- Toggle behavior: auto-detect via `prefers-color-scheme` on first visit, manual override persisted afterward
  (standard pattern, not something requiring further product input).

## Out Of Scope (this phase)

- Migrating any of the 11 feature-page `.scss` files' own hardcoded colors.
- A settings/preferences page - the toggle lives directly in the topbar.
- Per-account or per-user server-persisted theme preference (this is a local device preference, `localStorage`
  only, same tier as today's filter-persistence pattern).

## Frontend Design

No backend involvement - this is 100% frontend.

### Token layer

The codebase has no `@use`/`@import`/Sass partials anywhere (confirmed - every `.scss` is self-contained) and no
existing `:root` beyond the one in `src/styles.scss`. Rather than introduce a new Sass module pattern this
codebase doesn't use yet, define the tokens directly inside `src/styles.scss`, in two blocks:

```scss
:root {
  color-scheme: light;

  --color-bg: #f6f7fb;
  --color-bg-panel: #ffffff;
  --color-text: #1d2433;
  --color-text-secondary: #65738a;
  --color-border: #dce2ea;
  --color-border-strong: #c9d2df;
  --color-accent: #2563eb;
  --color-accent-hover: #1d4ed8;
  --color-success-text: #166534;
  --color-success-bg: #f0fdf4;
  --color-success-border: #bbf7d0;
  --color-error-text: #991b1b;
  --color-error-bg: #fff1f2;
  --color-error-border: #fecaca;
  --color-warning-text: #92400e;
  --color-warning-bg: #fffbeb;
  --color-warning-border: #fde68a;
  --color-info-text: #3730a3;
  --color-info-bg: #eef2ff;

  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  background: var(--color-bg);
  color: var(--color-text);
}

:root[data-theme='dark'] {
  color-scheme: dark;

  --color-bg: #11161f;
  --color-bg-panel: #1a2130;
  --color-text: #e6e9ef;
  --color-text-secondary: #9aa5b8;
  --color-border: #2b3444;
  --color-border-strong: #3a4457;
  --color-accent: #5b8def;
  --color-accent-hover: #7ba3f2;
  --color-success-text: #86efac;
  --color-success-bg: #0f2a1a;
  --color-success-border: #1e4d33;
  --color-error-text: #fca5a5;
  --color-error-bg: #2a1416;
  --color-error-border: #4d2427;
  --color-warning-text: #fcd34d;
  --color-warning-bg: #2a2210;
  --color-warning-border: #4d3f1e;
  --color-info-text: #a5b4fc;
  --color-info-bg: #1e2242;
}
```

(Light values are exactly the hex codes already in use today, so light mode is visually unchanged - only dark
values are new and open to visual tuning after implementation.)

Existing rules in `src/styles.scss` (`.panel`, `.button`, `.field input`, borders) are updated to reference the
tokens instead of literal hex, e.g. `.panel { background: var(--color-bg-panel); border-color: var(--color-border); }`.

### `ThemeService`

New `core/theme/theme.service.ts`:

```ts
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly storageKey = 'easyFinance.theme';
  readonly theme = signal<'light' | 'dark'>(this.resolveInitialTheme());

  constructor() {
    effect(() => document.documentElement.setAttribute('data-theme', this.theme()));
  }

  setTheme(theme: 'light' | 'dark'): void {
    this.theme.set(theme);
    localStorage.setItem(this.storageKey, theme);
  }

  toggle(): void {
    this.setTheme(this.theme() === 'dark' ? 'light' : 'dark');
  }

  private resolveInitialTheme(): 'light' | 'dark' {
    const stored = localStorage.getItem(this.storageKey);
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
}
```

Instantiated eagerly so the `effect` runs immediately - either injected once in `AppComponent`'s constructor, or
via `provideAppInitializer` alongside the existing auth bootstrap in `app.config.ts` (whichever this codebase's
convention favors for "must run once at startup" - `provideAppInitializer` is already used for
`AuthStore.bootstrapSession()`, so this follows the same precedent).

### Toggle UI

`core/layout/private-layout.component.ts` - add a text toggle next to "Mi perfil"/"Salir" in the `.user-context`
block (this app has no icon library, so a text toggle matches the existing icon-free convention):

```html
<button type="button" (click)="themeService.toggle()">
  {{ themeService.theme() === 'dark' ? 'Modo claro' : 'Modo oscuro' }}
</button>
```

Also migrate `private-layout.component.scss` itself to the new tokens as part of this phase (it's the app shell,
in scope) - this incidentally reconciles its pre-existing inconsistent accent palette (`#174ea6`/`#111827`/
`#dbe3ee`) with the rest of the app, since everything now reads from the same token set.

## Tests To Cover

- `theme.service.spec.ts`: defaults to `prefers-color-scheme` when nothing stored; reads a stored preference over
  the media query; `setTheme`/`toggle` persist to `localStorage` and set the `data-theme` attribute on
  `document.documentElement`.
- `private-layout.component.spec.ts`: toggle button renders current state and calls `themeService.toggle()` on
  click.

## Documentation To Update

- `frontend-context/frontend-guidance/ui-pages-map.md` / mirrored `docs/frontend-guidance/ui-pages-map.md`: note
  the theme toggle in the topbar and that only the shell/shared classes are dark-mode-aware in this phase (so a
  future contributor doesn't assume full coverage).

## Suggested Implementation Order

1. Token blocks in `src/styles.scss` (light values unchanged, new dark block) + migrate the shared classes
   already defined there (`.panel`, `.button`, `.field input`).
2. `ThemeService` with its spec.
3. Migrate `private-layout.component.scss` to tokens + add the toggle button, with its spec.
4. Wire `ThemeService` initialization at app startup.
5. Documentation note about phase scope.
6. Manual visual check in a browser (light and dark, toggle persists across reload) before calling this phase
   done - this is a UI change, so verify it live, not just via unit tests.

## Open Questions

None blocking for phase 1. The exact dark-mode hex values above are a reasonable starting palette, not a hard
requirement - easy to tune visually during/after implementation without affecting the architecture.
