# Deploying Cadence for Free

This is the step-by-step for the path we settled on: **Angular frontend on Vercel (free) + everything else on one Oracle Cloud "Always Free" VM (free) + Upstash Redis (free, managed) + Caddy for automatic HTTPS (free, via Let's Encrypt).**

Why not put every backend service on Railway/Upstash Kafka instead? Cadence is 15 Spring Boot services + Gateway + Eureka = 17 always-on JVMs (~5-6GB RAM combined). Railway's free tier is a one-time trial credit, not an ongoing free allowance — 17 always-on services burn through it in days and then bill real money monthly. Upstash Kafka's free tier daily message quota is also too small for a platform that fires Kafka events on nearly every user action. The Oracle Always-Free VM (2 OCPU/12GB ARM, genuinely free forever) is the only piece of this stack sized for 17 services at zero cost — so it stays the backbone. Upstash Redis is the one piece that *is* worth externalizing: this app's Redis usage (short-lived caching, session locks) comfortably fits Upstash's free tier, and moving it off the VM frees ~130MB of RAM and removes one more container that loses its cache on a VM restart.

Read [CADENCE_PLATFORM_DOCUMENTATION.md](CADENCE_PLATFORM_DOCUMENTATION.md) first if you haven't — it explains what each piece is and what's genuinely still incomplete. This guide only covers *hosting* what already exists.

## HTTPS is mandatory, not optional

Vercel always serves the frontend over HTTPS. Browsers block a HTTPS page from calling a plain-HTTP API ("mixed content"). So the backend **must** be served over HTTPS too, or the deployed frontend simply won't be able to call it at all. That's what the `caddy` service in `docker-compose.yml` is for — it auto-issues and renews a Let's Encrypt certificate for whatever domain you point at the VM.

You need *some* domain name pointing at the VM's public IP. If you don't own one:
- Free option: [DuckDNS](https://www.duckdns.org) — sign in, claim a subdomain like `cadence-api.duckdns.org`, point it at your VM's IP. Takes 2 minutes, no cost.
- Or use a domain you already own and add an `A` record for a subdomain (e.g. `api.yourdomain.com`) pointing at the VM's IP.

## 1. Provision the VM

1. Create an [Oracle Cloud](https://www.oracle.com/cloud/free/) account (free tier, requires a card for identity verification but won't charge you if you stay within Always Free limits).
2. Create a Compute instance:
   - Shape: **VM.Standard.A1.Flex** (Ampere ARM) — this is the Always Free shape. Currently capped at 2 OCPU / 12GB RAM for the always-free allocation.
   - Image: **Ubuntu 22.04** (or newer LTS).
   - Boot volume: default is fine (up to 200GB is free).
   - Note: Always-Free ARM capacity is sometimes unavailable in a given region right after signup ("out of host capacity" error) — if this happens, try a different Availability Domain, or try again later; it's a known, common Oracle Free Tier friction point, not something wrong with your account.
3. In the VM's **Virtual Cloud Network → Security List**, add ingress rules for ports **22** (SSH, restrict to your IP if possible), **80**, and **443** from `0.0.0.0/0`. Everything else (8080-8094, 3306, 9092, 9000/9001) should stay **closed to the internet** — those are only reached over the VM's internal Docker network, never directly. (Redis is no longer self-hosted on the VM at all — see step 3 below.)
4. SSH in, then install Docker:
   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker $USER
   # log out and back in for the group change to apply
   ```

## 2. Get the code onto the VM

```bash
git clone <your-repo-url> cadence
cd cadence
# (or scp the E:\HIring_AI_Tool directory up if it's not in git yet)
```

## 3. Create the free Upstash Redis database

1. Sign up at [console.upstash.com](https://console.upstash.com/redis) (free, no card required for the free tier).
2. **Create Database** → type **Regional** (not Global — Global is a paid feature) → pick a region close to your VM → TLS is on by default, leave it on.
3. Open the new database, go to its **Connect** tab, and note down the **Endpoint** (host), **Port**, and **Password** — you'll need all three in the next step.

## 4. Configure secrets

```bash
cp .env.example .env
nano .env
```

Fill in at minimum:
- `MYSQL_ROOT_PASSWORD` — pick a real password.
- `JWT_SECRET` — generate one: `openssl rand -base64 32 | base64 -w0`. This must be a real secret in production (the checked-in default is a placeholder every service falls back to, fine for local dev, **not** fine once this is reachable from the internet).
- `CORS_ALLOWED_ORIGINS` — your Vercel URL, once you know it (step 6).
- `CADDY_DOMAIN` — the domain/subdomain from the HTTPS section above.
- `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` — pick real values.
- `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` — from the Upstash "Connect" tab in step 3. Leave `REDIS_SSL_ENABLED=true`.
- `GEMINI_API_KEY` (and optionally `GEMINI_MODEL`, defaults to `gemini-2.0-flash`) or `GROQ_API_KEY` — at least one, for resume-parser-service / ai-interview-service / coding-assessment-service's AI features to work (both have generous free tiers; Gemini's is usually the easier one to get a key for quickly).
- SMTP credentials if you want notification-service to actually send email (e.g. a Gmail account with an [App Password](https://myaccount.google.com/apppasswords)). Leave blank to skip — the rest of the platform still works, emails just won't send.

## 5. Start everything

```bash
docker compose up -d --build
```

First boot will take a while — 13 Spring Boot services building from source, plus MySQL/Redpanda/MinIO/Caddy pulling images. Watch it with:

```bash
docker compose logs -f
```

Check that everything registered with Eureka:

```bash
curl -u eureka:eureka_pass http://localhost:8761/eureka/apps | grep -o '<name>[^<]*' 
```

You should see all 14 application names (13 business services + the Gateway) within a minute or two of startup — Flyway migrations run on each service's first boot and can take a few seconds each.

Verify the Gateway itself is reachable and JWT-protected:

```bash
curl -s https://<your-domain>/auth/api/v1/auth/login -X POST -H "Content-Type: application/json" -d '{}'
# expect a 400 (validation error) from auth-service, not a network error --
# confirms Caddy -> Gateway -> auth-service routing works end to end

curl -s https://<your-domain>/company/api/v1/companies/some-id
# expect a 401 from the Gateway's JWT filter (no token sent) -- confirms the
# JWT perimeter check is actually active on protected routes
```

## 6. Deploy the frontend to Vercel

1. Edit `cadence_angular/src/environments/environment.prod.ts` — set `apiBaseUrl` to `https://<your-domain>` (the same domain Caddy is serving).
2. Push that change, then in Vercel: **New Project → import the repo → root directory `cadence_angular`**. Vercel auto-detects `vercel.json` (already set up in this repo) for the build command and output directory.
3. Deploy. Once it's live, copy the Vercel URL back into the VM's `.env` as `CORS_ALLOWED_ORIGINS`, then:
   ```bash
   docker compose up -d api-gateway-service
   ```
   (only the Gateway needs restarting — it's the only service that reads that variable.)

## 7. Judge0 (coding-assessment-service's code execution) — not included

Running code execution needs [Judge0](https://github.com/judge0/judge0) self-hosted, which needs privileged container access (cgroups/isolate) and its own Postgres+Redis — on a 2 OCPU/12GB free VM already running everything else, this is genuinely tight. Two options:
- Skip it for now — every other feature works fine; only "Run"/"Submit" inside a coding assessment will fail.
- Use Judge0's free hosted instance on RapidAPI (has a free quota) instead of self-hosting: set `JUDGE0_BASE_URL` in `.env` to the RapidAPI endpoint and add the required RapidAPI key to coding-assessment-service's config (not currently wired as an env var — a small code change would be needed to pass the RapidAPI key header through).

## 8. Automatic deploys via GitHub Actions (optional but recommended)

`.github/workflows/ci-cd.yml` builds and tests all 15 services + the Angular app on every push/PR, and on a push to `master` it rsyncs a fresh production Angular build into the VM's `frontend-dist` and SSHes in to re-deploy the backend (`git pull && docker compose up -d --build`). To enable the deploy step, add these three repo secrets (GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**):

- `VM_HOST` — the VM's public IP or your `CADDY_DOMAIN`.
- `VM_USER` — the SSH user you log in as (e.g. `ubuntu`).
- `VM_SSH_KEY` — the **private** half of an SSH key pair whose public half is already in the VM's `~/.ssh/authorized_keys`. Generate a dedicated deploy key rather than reusing your personal one: `ssh-keygen -t ed25519 -f deploy_key -N ""`, add `deploy_key.pub` to the VM, paste the contents of `deploy_key` (the private key) as the `VM_SSH_KEY` secret.

Without these three secrets the build-and-test jobs still run (so broken code is still caught on every PR), the deploy job just won't have anything to connect with and will fail — which is safe, it only means deploys stay manual (step 5's `docker compose up -d --build` over SSH) until you add them.

## 9. What to expect, honestly

- This is a **single VM, no redundancy**. If it reboots or runs out of memory, everything on it goes down together until it recovers. That's the tradeoff of "free."
- The cross-service Kafka gaps documented in `CADENCE_PLATFORM_DOCUMENTATION.md` §6 are still there — this deployment makes the *infrastructure* production-shaped, it doesn't fix the application-level gaps (resumeId-null bug, no advance-stage endpoint, etc.).
- Memory is tuned tight (see `docker-compose.yml`'s `x-jvm-small` block). If a service crashes with an OOM, that's the signal to either bump its `mem_limit`/`-Xmx` a bit (there's roughly 3-4GB of headroom left in the 12GB budget now that Redis moved off the VM) or turn off a service you're not actively using.
- Upstash's free tier has a monthly command-count cap. This app's Redis usage (short caches, session locks) is light, so it comfortably fits — but if the app grows a lot busier, that cap is the first thing to check if Redis calls start failing.
