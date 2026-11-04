# Security notes

## Initial administrator

The backend creates an administrator only when a new database is empty and both `ADMIN_EMAIL` and `ADMIN_PASSWORD`
are explicitly configured. The credentials are never supplied as repository defaults. The initial account is inserted
with a durable `system_state` marker, so deleting the administrator later does not cause it to be recreated on restart.
Existing non-empty D1 databases are marked initialized during schema setup. Provision a replacement administrator
through the authenticated admin-management workflow.

## Login throttling

Login requests use atomic, persistent D1 fixed-window counters for both the client network address and the
email/network pair. The pair limit avoids account-wide lockout: a failed-login flood from one address does not lock a
victim out from another address. Keys are HMAC-SHA-256 hashes, not plaintext IP addresses or email/IP pairs. Defaults
are 120 attempts per IP and 8 attempts per email/IP pair per 15 minutes; the limits and window are configurable with
`LOGIN_RATE_LIMIT_IP_MAX_ATTEMPTS`, `LOGIN_RATE_LIMIT_ACCOUNT_IP_MAX_ATTEMPTS`, and `LOGIN_RATE_LIMIT_WINDOW`.
Successful authentication resets the corresponding pair counter.

The application uses `HttpServletRequest.getRemoteAddr()` and does not trust `X-Forwarded-For` or other forwarded
headers. Until trusted proxy addresses are explicitly configured, deployments behind a shared proxy may see requests
under the proxy address and therefore share the IP-wide limit. Consider that operational constraint when setting the
IP limit; do not enable forwarded headers without restricting trust to known proxy CIDRs.

Login failures for unknown, inactive, wrong-password, and throttled accounts use a generic message. Public
registration still returns a conflict for a duplicate email to preserve the existing API contract; this allows
account-existence enumeration until an email-verification flow is introduced.

## Browser and API

The frontend continues storing bearer JWTs in `localStorage` for compatibility. This means successful script injection
could expose the current token; migrating to HttpOnly cookies requires a separate CSRF/session design and is not part
of this change. The Vercel configuration adds a restrictive Content Security Policy and standard browser security
headers. It permits the current API host (`https://dolphin-rlvv.onrender.com`) and local development API
(`http://localhost:8080`); update `connect-src` when the deployed API host changes. Inline style attributes remain
allowed for existing React-rendered styles, while inline scripts are blocked.

API CORS accepts only exact origins from `CORS_ALLOWED_ORIGINS`; wildcard patterns are rejected. Set production origins
in the hosting environment. No deployment configuration was changed or deployed as part of this work.

## Database and runtime scope

Login counters and bootstrap state are additive D1 tables; existing user records and other application data are not
dropped or rewritten. D1 provider errors returned by the API are sanitized and the Cloudflare token is not logged.
Database credentials must be provided through the environment or the ignored local `backend/d1.env` file.

No Cloudflare Worker source or production hosting runtime configuration is tracked in this repository. Worker-specific
endpoints, production origin values, proxy topology, and deployed headers therefore require separate verification.
