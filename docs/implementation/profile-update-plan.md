# Profile Update - Implementation Plan

## Objective

Allow an authenticated user to update their own name from the frontend. Today `POST /auth/register`, `POST /auth/login`, and `GET /auth/me` exist, but there is no way to change `User.fullName` or `Participant.displayName` after registration.

## Decisions Already Made

These were confirmed with the product owner before drafting this plan; do not revisit them without a new explicit decision:

- **Single field, no schema change.** Keep one free-text "full name" field, matching how registration already works (`RegisterRequest.fullName`, a single input). Do **not** split into `firstName`/`lastName`. No new columns, no migration.
- **Keep `User.fullName` and `Participant.displayName` in sync.** A single update always writes the same value to both. They already start equal at registration (`RegisterUserUseCase` creates the participant with `savedUser.fullName()`); this feature preserves that invariant instead of letting them diverge.

## Out Of Scope

- Changing email (requires re-verification/uniqueness flow, not part of this change).
- Changing password (already covered separately if/when implemented; different security profile).
- Per-account nicknames (would require treating `displayName` as independent from `fullName`, which was explicitly rejected above).
- Any admin-initiated rename of another user (this is self-service only, same actor whose token is used).

## Backend Design

Follows the existing hexagonal structure of the `identity` module (mirrors `GetCurrentUserUseCase`/`RegisterUserUseCase`).

### Domain layer

`identity/domain/model/User.java` - add an update method next to the existing factories:

```java
public User rename(String fullName) {
    return new User(id, email, passwordHash, fullName, status, globalRoles);
}
```

Validation is already enforced by the private constructor (`requireText(fullName, "FULL_NAME_REQUIRED", ...)`), so no duplicate validation is needed here - same pattern `Expense.update()` uses in the `expenses` module.

`identity/domain/model/Participant.java` - add the equivalent:

```java
public Participant rename(String displayName) {
    return new Participant(id, userId, displayName, status);
}
```

Validation already enforced by the constructor's `requireText(displayName)` (throws `DISPLAY_NAME_REQUIRED`).

No changes needed to `UserStatus`, `ParticipantStatus`, or any Flyway migration - `users.full_name` and `participants.display_name` already exist and are already `NOT NULL`.

### Application layer

New files, following the same package layout as the rest of `identity`:

- `identity/application/command/UpdateProfileCommand.java`
  ```java
  public record UpdateProfileCommand(String fullName) {}
  ```
- `identity/application/port/in/UpdateProfilePort.java`
  ```java
  public interface UpdateProfilePort {
      AuthenticatedUserResponse updateProfile(UpdateProfileCommand command);
  }
  ```
- `identity/application/usecase/UpdateProfileUseCase.java` implements `UpdateProfilePort`. Structure mirrors `GetCurrentUserUseCase`:
  1. Resolve `CurrentUser` via `CurrentUserProvider`, same `UNAUTHENTICATED` guard.
  2. Load `User` by `currentUser.userId()` (`USER_NOT_FOUND` if missing) and `Participant` by `user.id()` (`PARTICIPANT_NOT_FOUND` if missing) - reuse `UserRepositoryPort`/`ParticipantRepositoryPort`, already injected the same way in `GetCurrentUserUseCase`.
  3. Reuse the same active-status guard `GetCurrentUserUseCase` already has inline (`USER_BLOCKED`/`USER_NOT_ACTIVE`/`PARTICIPANT_NOT_ACTIVE`) so a blocked/inactive identity cannot rename itself either. (Note: this duplicates logic already centralized in `identity/infrastructure/security/IdentityAuthenticatedUserStatusValidator`, which the JWT filter chain runs before any controller executes; `GetCurrentUserUseCase` already carries its own inline copy today, so this plan mirrors the existing precedent rather than introducing a new one. Consolidating the two is a separate, unrelated cleanup - do not bundle it into this feature.)
  4. `@Transactional` - `userRepository.save(user.rename(command.fullName()))`, then `participantRepository.save(participant.rename(command.fullName()))`. Trim/blank handling is already covered by the domain constructors.
  5. Build and return `AuthenticatedUserResponse` the same way `GetCurrentUserUseCase.toResponse` does today.

No new output ports needed - `save(User)` and `save(Participant)` already exist on `UserRepositoryPort`/`ParticipantRepositoryPort` and already support updates (used today by other flows that persist a modified aggregate).

### Entrypoint layer

`identity/entrypoint/rest/dto/UpdateProfileRequest.java`:

```java
public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 150, message = "Full name must be at most 150 characters.")
        String fullName
) {}
```

(150 is the existing column length for `participants.display_name`; keep both fields under the smaller of the two column sizes to avoid a DB-level truncation/constraint error surfacing as a raw 500.)

`AuthRestMapper` - add `toCommand(UpdateProfileRequest request)` returning `UpdateProfileCommand`, following the same static-method style already used for `RegisterRequest`/`LoginRequest`.

`AuthController` - add:

```java
@PutMapping("/me")
public AuthenticatedUserDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
    return AuthRestMapper.toDto(updateProfilePort.updateProfile(AuthRestMapper.toCommand(request)));
}
```

Reuses the existing `AuthenticatedUserDto`/`AuthRestMapper.toDto(AuthenticatedUserResponse)` - no new response DTO needed since the shape (`userId`, `participantId`, `email`, `fullName`, `globalRoles`) is already exactly what changes.

`PUT /api/v1/auth/me` was chosen (not `PATCH /api/v1/users/{id}` or similar) because:
- It operates on "myself", identified by the token, exactly like the existing `GET /auth/me` - no `{id}` path variable, no risk of editing someone else's profile through it.
- It's consistent with this project's convention of `PUT` for full-resource update (`PUT /accounts/{accountId}`, `PUT /budgets/{year}/{month}`).

### Error codes (new)

| Code | HTTP status | Trigger |
|---|---|---|
| `FULL_NAME_REQUIRED` | 400 (via bean validation `VALIDATION_ERROR`, or 422 if it reaches the domain constructor) | Blank/missing `fullName` |
| `DISPLAY_NAME_REQUIRED` | 422 | Should not surface in practice (same input feeds both), kept for domain-level defense in depth |

No new codes beyond these; `USER_NOT_FOUND`, `PARTICIPANT_NOT_FOUND`, `USER_BLOCKED`, `USER_NOT_ACTIVE`, `PARTICIPANT_NOT_ACTIVE`, `UNAUTHENTICATED` are already defined and reused as-is.

## Frontend Design

Mirrors the existing `core/auth` + a new lightweight feature, following the same layered pattern as every other feature (`core/<feature>/*-api.service.ts` + `*.store.ts`, `features/<feature>/*-page.component.ts`).

### Models

`shared/models/auth.models.ts` - add:

```ts
export interface UpdateProfileRequest {
  fullName: string;
}
```

`AuthenticatedUserDto` needs no changes (fullName already there).

### API service

`core/auth/auth-api.service.ts` - add:

```ts
updateProfile(request: UpdateProfileRequest): Observable<AuthenticatedUserDto> {
  return this.api.put<AuthenticatedUserDto, UpdateProfileRequest>('/auth/me', request);
}
```

### Store

`core/auth/auth.store.ts` - add a method that updates the in-memory session the same way `bootstrapSession` already patches `user` into the stored session:

```ts
updateProfile(request: UpdateProfileRequest): Observable<AuthenticatedUserDto> {
  this.isLoading.set(true);
  this.authError.set(null);

  return this.authApi.updateProfile(request).pipe(
    tap((user) => {
      const current = this.session();
      if (current) {
        this.setSession({ ...current, user });
      }
    }),
    catchError((error: unknown) => {
      this.authError.set(toApiError(error));
      return throwAuthError(error);
    }),
    finalize(() => this.isLoading.set(false))
  );
}
```

This keeps `authStore.user()` (already read everywhere, e.g. the topbar in `private-layout.component.ts`) automatically up to date after a successful save, with no extra plumbing.

### Feature: profile page

New folder `features/profile/`:
- `profile-page.component.ts` - standalone component, `NonNullableFormBuilder` reactive form with a single `fullName` control (`Validators.required`, `Validators.maxLength(150)`), prefilled from `authStore.user()?.fullName` on init. Same structural pattern as the simplest existing forms (e.g. the manual-debt or category forms): a panel, a form, a submit button disabled while `authStore.isLoading()`, a success message signal, and a `friendlyError(code, message)` map for `FULL_NAME_REQUIRED`/validation errors - same convention used in `expenses-page.component.ts`/`debts-page.component.ts`.
- `profile.routes.ts`:
  ```ts
  export const PROFILE_ROUTES: Routes = [
    { path: '', component: ProfilePageComponent }
  ];
  ```

### Routing

`app.routes.ts` - add a sibling to `dashboard`/`accounts` (profile is user-scoped, not account-scoped, so it does not need `accountRouteGuard`):

```ts
{
  path: 'profile',
  loadChildren: () => import('./features/profile/profile.routes').then((m) => m.PROFILE_ROUTES)
}
```

Resulting URL: `/app/profile`.

### Entry point in the UI

`core/layout/private-layout.component.ts` - the `.user-context` block in the topbar currently shows:

```html
<span>{{ authStore.user()?.fullName ?? authStore.user()?.email }}</span>
<button type="button" (click)="logout()">Salir</button>
```

Add a link between the name and "Salir":

```html
<span>{{ authStore.user()?.fullName ?? authStore.user()?.email }}</span>
<a routerLink="/app/profile">Mi perfil</a>
<button type="button" (click)="logout()">Salir</button>
```

This is the only navigation entry point needed; no sidebar item, since it is not a financial-data section.

## Tests To Cover

**Backend** (same layers already covered for every other use case in this codebase):
- `UserTest` (or a new `UserTest` if none exists) / extend existing domain tests: `rename` updates `fullName`, rejects blank via the existing `FULL_NAME_REQUIRED` code.
- `ParticipantTest`: `rename` updates `displayName`, rejects blank via `DISPLAY_NAME_REQUIRED`.
- `UpdateProfileUseCaseTest`: happy path updates both `User` and `Participant` with the same value and returns them in the response; `UNAUTHENTICATED` without a current user; `USER_NOT_FOUND`/`PARTICIPANT_NOT_FOUND`; `USER_BLOCKED`/`USER_NOT_ACTIVE`/`PARTICIPANT_NOT_ACTIVE` block the update the same way they block `GetCurrentUserUseCase`.
- `AuthControllerTest`: `PUT /auth/me` delegates to the port and maps the DTO; validation error on blank `fullName`.
- `AuthControllerSecurityTest`: unauthenticated request to `PUT /auth/me` is rejected, same as the existing `GET /auth/me` case in that file.

**Frontend**:
- `auth-api.service.spec.ts`: `updateProfile` calls `PUT /auth/me` with the request body.
- `auth.store.spec.ts`: `updateProfile` patches `user()` in place after success, propagates errors into `authError()`.
- `profile-page.component.spec.ts`: prefills the form from `authStore.user()`, required/maxLength validation, calls `authStore.updateProfile` on submit, shows a success message, shows a friendly error for `FULL_NAME_REQUIRED`/validation errors, disables submit while saving.
- `private-layout.component.spec.ts` (if one exists): "Mi perfil" link is present and points to `/app/profile`.

## Documentation To Update

Same set of files touched for every other backend-contract change in this project, kept in sync between `easy-finance-backend/frontend-context/` and `easy-finance-frontend/docs/`:

- `frontend-context/api/api-overview.md` / `docs/api/api-overview.md`: add `PUT /api/v1/auth/me` under Auth.
- `frontend-context/api/auth-flow.md` / `docs/api/auth-flow.md`: document the new endpoint and that it keeps `fullName`/`displayName` in sync.
- `frontend-context/models/dto-reference.md` / `docs/models/dto-reference.md`: add `UpdateProfileRequest`.
- `frontend-context/business/business-rules.md` / `docs/business/business-rules.md`: note that `User.fullName` and `Participant.displayName` are always updated together and never diverge.
- `frontend-context/frontend-guidance/ui-pages-map.md` / `docs/frontend-guidance/ui-pages-map.md`: add a "Profile" page entry (goal, endpoint, actions, components, errors).
- `docs/domain/domain-model.md`: no changes needed - checked, it does not claim `fullName`/`displayName` are immutable.

## Suggested Implementation Order

1. Backend domain + application (`User.rename`, `Participant.rename`, command/port/use case) with unit tests.
2. Backend entrypoint (`UpdateProfileRequest`, mapper, controller endpoint) with controller + security tests.
3. Frontend model/api-service/store with their specs.
4. Frontend `profile` feature + route + topbar link with its spec.
5. Documentation sync (both repos) and a final read-through against the actually-implemented code (not against this plan) before calling it done - same discipline used throughout this project's docs.
6. Compile + run full backend (`mvn test`) and frontend (`ng test`) suites; confirm 0 failures before considering the change complete.

## Open Questions

None. Both blocking product decisions were resolved before writing this plan, and every file/pattern referenced above (`AuthControllerTest`, `AuthControllerSecurityTest`, `UserRepositoryPort.save`, `ParticipantRepositoryPort.save`, the `users`/`participants` column definitions) was verified against the current codebase rather than assumed.
