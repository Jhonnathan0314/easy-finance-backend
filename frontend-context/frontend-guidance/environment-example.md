# Angular Environment Example

```ts
export const environment = {
  production: false,
  appName: 'Easy Finance',
  apiBaseUrl: 'http://localhost:8080',
  apiPrefix: '/api/v1'
};
```

For production:

```ts
export const environment = {
  production: true,
  appName: 'Easy Finance',
  apiBaseUrl: 'https://api.example.com',
  apiPrefix: '/api/v1'
};
```

`apiPrefix` is combined with `apiBaseUrl` by the HTTP client to build request URLs (`{apiBaseUrl}{apiPrefix}/...`).

Never store JWT secrets in the frontend. The frontend stores only the user access token returned by login/register.

