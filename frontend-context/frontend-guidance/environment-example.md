# Angular Environment Example

```ts
export const environment = {
  production: false,
  appName: 'Easy Finance',
  apiBaseUrl: 'http://localhost:8080'
};
```

For production:

```ts
export const environment = {
  production: true,
  appName: 'Easy Finance',
  apiBaseUrl: 'https://api.example.com'
};
```

Never store JWT secrets in the frontend. The frontend stores only the user access token returned by login/register.

