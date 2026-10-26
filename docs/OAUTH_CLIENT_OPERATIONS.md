# OAuth client and signing-key operations

This is an operator guide. Keep credentials and signing-key material out of source control,
container images, command arguments, and logs. The authorization tables must exist before using
the registration tools.

## Supported client types

The initial public service supports browser SPAs, native/mobile applications, CLI clients, and one
confidential Telegram BOT client. Browser, native/mobile, and CLI applications are public clients:
register each manually with an operator-controlled `client_id` and exact redirect URIs, and use
Authorization Code with PKCE (`S256`). CLI clients may use loopback redirects where appropriate.
Public clients do not receive or store a client secret.

The Telegram integration is the only supported confidential client in the initial service. Generic
server-to-server integrations and unrestricted Dynamic Client Registration are not supported.
The Resource Owner Password Credentials grant is not supported.

## Signing key

Production requires a durable RSA private key in a PKCS#12 keystore. Create it outside the
repository and protect the file and password with the deployment's secret-management system:

```powershell
keytool -genkeypair -alias portroyal-signing -keyalg RSA -keysize 3072 -storetype PKCS12 `
  -keystore C:\secure\portroyal-signing.p12 -validity 3650 -dname "CN=Port Royal OAuth"
```

Provide the public issuer URL in `OAUTH2_ISSUER`; provide the keystore path, alias, and password
through `OAUTH2_SIGNING_KEY_PATH`, `OAUTH2_SIGNING_KEY_ALIAS`, and either
`OAUTH2_SIGNING_KEY_PASSWORD` or `OAUTH2_SIGNING_KEY_PASSWORD_FILE`. Docker Compose mounts the
keystore and password as secrets. The `dev` and `test` profiles generate ephemeral keys and must
not be used for production.

For a planned rotation, add the new RSA key pair under a new alias in the same protected
keystore, set `OAUTH2_SIGNING_KEY_ALIAS` to that alias, and restart the API. The Authorization
Server signs with the selected alias while publishing the other keystore aliases as verification
keys. Keep the previous alias until the maximum access-token and ID-token lifetime has elapsed and
clients have refreshed; then remove that alias and restart again. Keep the old keystore available
until the new configuration is verified so the change can be rolled back.

## Bootstrap the production administrator

After Flyway has created the application schema, configure
`ADMIN_BOOTSTRAP_USERNAME` and `ADMIN_BOOTSTRAP_PASSWORD` in the operator's protected environment
and run:

```powershell
mvn -pl portroyal_be exec:java `
  "-Dexec.mainClass=com.matteopaciolla.prbe.cli.AdminBootstrapTool"
```

The tool creates one enabled `ADMIN`/`USER` account and stores only a BCrypt hash. It refuses an
existing username and writes no credentials to output. Remove the bootstrap environment values
after the one-time operation.

## Register OAuth clients

Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` for the
operator tool. Public clients are operator-registered with exact redirect URIs; the tool rejects
wildcards, fragments, credentials in the URI, and non-loopback plain HTTP:

```powershell
mvn -pl portroyal_be exec:java `
  "-Dexec.mainClass=com.matteopaciolla.prbe.cli.OAuthClientRegistrationTool" `
  "-Dexec.args=public portroyal-web https://client.example.com/oauth/callback"
```

Public clients have no secret and require Authorization Code with PKCE. Register only redirect
URIs controlled by the client owner.

The guided registration command is an operator action, not a public registration endpoint. Add
each exact callback URI separately; wildcard redirect URIs are not accepted.

The Telegram BOT is one confidential OAuth client. Configure a unique
`OAUTH2_BOT_CLIENT_ID` and high-entropy `OAUTH2_BOT_CLIENT_SECRET` in a protected operator
environment, then run:

```powershell
mvn -pl portroyal_be exec:java `
  "-Dexec.mainClass=com.matteopaciolla.prbe.cli.OAuthClientRegistrationTool" `
  "-Dexec.args=bot"
```

The BOT secret is BCrypt-encoded in PostgreSQL. The running API needs the BOT client ID for
delegation checks; it does not need the secret. Keep the secret only in the BOT's secret store.
`curl --user "$OAUTH2_BOT_CLIENT_ID"` prompts for the password instead of putting it in the
command line.

To rotate a compromised secret, set the new secret as `OAUTH2_BOT_CLIENT_SECRET` in the operator's
protected environment and run the tool with `-Dexec.args=rotate-bot`. The tool updates the stored
BCrypt hash and deletes all existing BOT authorizations in one database transaction. Coordinate
the BOT deployment's secret update with this operation; its existing access tokens will stop
working immediately.

## Telegram onboarding and delegated access

Use the confidential BOT client token with the `bot:onboard` scope only for account registration
and linking:

```sh
curl --user "$OAUTH2_BOT_CLIENT_ID" \
  -d "grant_type=client_credentials" \
  -d "scope=bot:onboard" \
  "$OAUTH2_ISSUER/oauth2/token"
```

Use that bearer token with `POST /api/v1/user/register/telegram`,
`POST /api/v1/user/new-tg-unify-email-otp`, or `POST /api/v1/user/unify`. Those account-linking
requests may include the Telegram sender ID in `tgId`; registration also requires a matching
`telegramId` in the body.

For ordinary game requests, exchange the sender ID for a delegated human token. The BOT authenticates
at the token endpoint with its confidential client credentials:

```sh
curl --user "$OAUTH2_BOT_CLIENT_ID" \
  --data-urlencode "grant_type=urn:portroyal:grant-type:telegram-delegation" \
  --data-urlencode "tgId=123456789" \
  "$OAUTH2_ISSUER/oauth2/token"
```

The delegated access token has the linked human's stable database ID as `sub` and retains the BOT
client in `client_id` and `actor_client_id`. Send it as `Authorization: Bearer <access-token>` for
game and match requests. Do not send `tgId` on normal API calls.

## Custom Telegram delegation grant

The `urn:portroyal:grant-type:telegram-delegation` grant is a custom grant, not standard
`client_credentials`: only the authenticated Telegram BOT may use it, and it accepts `tgId` only
at this trusted token-exchange boundary. The API trusts the BOT's assertion of the Telegram sender
ID. The delegated token represents the linked human and is short-lived; the BOT does not receive a
refresh token.

## Token lifecycle

Access tokens are signed JWTs with a five-minute lifetime, the configured issuer, and the
`portroyal-api` audience. The API checks the signature, issuer, audience, expiry, and persisted
authorization on every request; deleting the authorization row revokes a token immediately.
Refresh tokens rotate on use and expire after 30 days. Password changes, account deletion, and
browser-session logout revoke the user's persisted authorizations. Password changes and resets also
invalidate existing browser login sessions at their next authorization or consent request.

OAuth clients should also revoke tokens they no longer need through the Authorization Server's
revocation endpoint before discarding their local token state. Public clients must use PKCE with
`S256`; never embed a client secret in a browser or mobile application. The BOT does not receive
refresh tokens; it obtains short-lived onboarding or delegated tokens as needed.
