# 360° Patient Care — B2B Go‑To‑Market & Pricing Playbook

> A complete, plain‑language guide: what the product is, who buys it, how the
> business works end‑to‑end, how to price and charge, how to sell and onboard,
> what to fix before selling, and a 30/60/90‑day action plan.
>
> Currency: ₹ (Indian market). All prices are **illustrative starting points** —
> anchor them to each customer's real cost‑to‑serve.

---

## 0. TL;DR (read this first)

- **Model:** You are a **software company**. You *rent* your platform to care
  businesses (ambulance operators, home‑care companies, hospitals). **B2B SaaS.**
- **Your customer:** a company that already has patients + staff but runs on
  phone calls, WhatsApp and paper. You give them software to run it properly.
- **How you earn:** a **recurring monthly subscription** per company. Start with
  one flat monthly fee in 3 plan sizes. Add per‑case/setup fees later.
- **Simplest start:** Starter ₹25k/mo · Growth ₹75k/mo · Enterprise ₹1.5L+/mo.
- **One customer at ₹40k/mo = ₹4.8L/year.** Ten customers = ₹48L/year recurring.
- **Before you sell:** fix the 3 blockers in §10 (security, multi‑tenancy, live
  push/payments).

---

## 1. What the product actually is

Three connected surfaces:

| Surface | Who uses it | What it does |
|---|---|---|
| **Operations Portal (Command Centre)** | The *business's* staff — admins, dispatchers, coordinators, finance | Service requests, case files, dispatch, live tracking, SLA, billing, settlements, cash reconciliation, fleet, hospitals, agents, reports |
| **Agent app** (Android) | Field staff — care agents, ambulance crew | Receive jobs, navigate, pickup/handover OTP, earnings, settlement |
| **Patient app** (Android) | Patients / families | Book care, track ambulance, care packages, records, support |

**Key insight:** the heavy, valuable part is the **portal** (dispatch +
settlements + SLA = an *operator's* control room). That tells you the **buyer is
a business**, and the patient is the end‑consumer served *through* that business.
→ This is a **B2B2C** platform, sold **B2B**.

---

## 2. The business model (B2B SaaS), in plain words

You do **not** treat patients or run ambulances. You **rent your software** to
companies that do. They pay you **every month** to use it.

- You sell **access + support**, not labour.
- Revenue is **recurring** (monthly/annual subscription) — predictable, compounding.
- You scale by **adding customers** and **upselling modules**, not by hiring ops staff.

Analogy: you're selling the "operating system" for a patient‑care business, the
way a restaurant buys billing/KDS software instead of building it.

---

## 3. Who to sell to (Ideal Customer Profile)

Pick customers who **already have both sides** (patients/demand + staff/supply) and
are in pain from manual coordination.

| Target customer | Their pain | Which modules sell it |
|---|---|---|
| **Ambulance / medical transport operators** | Phone‑call dispatch, no live tracking, disputed trips, cash leakage | Dispatch, live map, trip OTP, cash reconciliation |
| **Home‑healthcare / elder‑care companies** | Scheduling caretakers, no visibility, manual billing | Agents, care services, billing, patient app |
| **Hospitals / hospital networks** | Patient transport + admission coordination, bed visibility | Requests→cases, hospitals/beds, SLA |
| **Insurers / TPAs** | Coordinating claimant care, proof of service | Case files, SLA, reports, settlements |
| **Senior‑living / corporate health** | On‑demand medical help for residents/employees | Patient app, nearby hospitals, packages |

**Best first customer:** a **single ambulance operator or home‑care company in
your city** with 10–60 field staff — small enough to close fast, big enough to pay.

---

## 4. The problem you solve (your pitch)

Today they run on: **phone calls + WhatsApp + Excel + paper receipts.** Result:
- Missed/duplicated jobs, slow dispatch, no live location for families
- Disputes over trips and cash, revenue leakage
- No SLA proof, no reports, no audit trail
- Manual billing and agent settlements = hours of admin

**You replace all of that with one system:** a request comes in → triaged →
converted to a case → auto‑dispatched to the nearest available agent → live
tracked on a map → OTP‑verified pickup/handover → auto‑billed → agent settled →
everything reported. **One source of truth, less chaos, more revenue captured.**

**One‑line pitch:** *"We turn your phone‑call chaos into a live command centre —
every request tracked from booking to settlement, on one screen."*

---

## 5. The full business flow (end‑to‑end)

### 5.1 Your sales → revenue flow (how YOU make money)
1. **Find** a prospect (ICP in §3).
2. **Demo** the portal + agent app (the "wow" is the live map + auto‑dispatch).
3. **Quote** a price (§6) based on their size/volume.
4. **Close** — sign a simple 1‑year subscription contract.
5. **Onboard** — create their account, load agents/vehicles/hospitals, train (1–2 days).
6. **Go live** — they run daily operations on the platform.
7. **Invoice monthly** — they pay. You support + upsell. **Repeat.**

### 5.2 Your customer's operational flow (what they DO with it)
`Patient books (app/call) → Portal triage → Convert to Case → Auto‑dispatch to agent
→ Agent accepts → En route (live GPS) → Pickup OTP → In transit → Hospital handover OTP
→ Case closed → Billing/quote → Payment → Agent settlement → Reports/SLA.`

This is the loop they pay you to run smoothly.

---

## 6. Pricing — how to charge

### 6.1 The bases you *can* charge on

| Base | You meter… | Best for | Illustrative |
|---|---|---|---|
| Per portal seat | each admin/dispatcher/finance login | all | ₹800–2,500 / user / mo |
| Per active agent | each field worker on the agent app | staffing‑heavy | ₹300–800 / agent / mo |
| Per vehicle | each ambulance | transport operators | ₹1,500–4,000 / vehicle / mo |
| **Per case / per trip** ⭐ | each completed request/trip | everyone (usage = value) | ₹10–60 / completed case |
| Per bed / facility | beds or sites served | hospitals, senior‑living | by capacity band |
| % take‑rate (GMV) | % of money flowing through billing | if you process payments | 1–3% of GMV |
| Tiered subscription | flat plan by volume + features | all (predictable base) | see §6.3 |
| One‑time setup | onboarding/implementation | enterprise | ₹1–3L one‑time |

### 6.2 The principle: charge on a **value metric**
Pick the thing that (a) **grows as the customer gets more value**, (b) is **easy to
measure**, (c) the **customer understands**. For this platform that is the
**completed case/trip**. Keep a **subscription floor** so revenue is predictable.

> **Recommended model:** **Monthly subscription (floor) + per‑completed‑case fee
> (growth lever) + one‑time setup.** You win when they win.

**Avoid** pure per‑seat pricing — it punishes adoption and caps your upside on a
high‑volume ops tool.

### 6.3 The simplest way to start (do this first)
Charge **ONE flat monthly fee per company**, in 3 sizes. Don't over‑engineer.

| Plan | For | Includes | Price / month |
|---|---|---|---|
| **Starter** | small operator (≤10 staff, ≤500 cases/mo) | Portal + agent app, dispatch, 5 seats, 15 agents | **₹25,000** |
| **Growth** | mid (≤50 staff, ≤3,000 cases/mo) | + live tracking, billing, patient app, 20 seats, 60 agents | **₹75,000** |
| **Enterprise** | large / white‑label | Unlimited + multi‑tenant/white‑label + API + SLA + priority support | **₹1.5L+ (custom)** |

**Add‑ons (introduce later, not day one):**
- **Setup/onboarding (one‑time):** ₹1–3 lakh
- **Overage:** ₹20–40 per completed case beyond the plan cap
- **White‑label branding:** +₹25–50k/mo
- **Take‑rate** (if you run their billing/settlements): 1–3% of GMV

### 6.4 Which base by customer type

| Customer | Charge on |
|---|---|
| Ambulance operator | per‑vehicle + per‑trip (or Growth tier) |
| Hospital network | tier by beds/volume + per‑case |
| Home‑healthcare | per‑agent + per‑visit |
| Insurer / TPA | purely per‑case/claim processed |
| Running their billing | add 1–3% take‑rate |

### 6.5 How to arrive at the actual number
Ask: **"What does your current manual process cost you?"** — count staff time,
missed jobs, disputes, cash leakage. If your software saves them ₹2L/month of pain,
₹40k/month is an easy yes. **Price at a fraction of the value you create**, not at
your cost. Offer **annual prepay (10–15% discount)** to improve cash flow.

### 6.6 Example deal math
- 1 Growth customer = ₹75k/mo = **₹9L/year**
- 10 customers (mix of plans, avg ₹50k) = **₹60L/year recurring**
- Add setup fees + overage → more. This is the compounding B2B SaaS engine.

---

## 7. How you actually collect the money (mechanics)

1. **Contract:** a simple **1‑year subscription agreement** (auto‑renew) — scope,
   price, payment terms (e.g., monthly in advance, Net‑7), data ownership, support SLA.
2. **Invoice:** raise a **GST invoice** each month/quarter (you'll need **GST
   registration** + ideally a Pvt Ltd / LLP).
3. **Collect via:** bank transfer / UPI / **payment link** (Razorpay, Cashfree) or
   **auto‑debit (eNACH/mandate)** for hands‑off recurring collection.
4. **Dunning:** auto‑reminders for due/failed payments; suspend access after grace.
5. **Billing ops:** start manual (spreadsheet + invoices); automate with a
   subscription‑billing tool (Chargebee/Zoho Subscriptions/Razorpay Subscriptions)
   once you have 5–10 customers.

> Tip: You already have **billing + settlement + cash‑reconciliation modules** for
> *their* operations — don't confuse that with **how you bill them**. The former is
> product; the latter is your own finance process (invoices to your customers).

---

## 8. The sales process (step by step)

1. **Prospect list:** 30–50 ICP companies in your city (ambulance operators,
   home‑care, nursing agencies). LinkedIn, JustDial, local associations, referrals.
2. **Outreach:** short message — "I built software that replaces your phone‑call
   dispatch with a live command centre. 15‑min demo?"
3. **Discovery (15 min):** understand their volume, staff, current tools, pains.
4. **Demo (20–30 min):** show the *live* flow — book → dispatch → map → OTP →
   settlement. Lead with the live map; it sells itself.
5. **Proposal:** 1‑page quote (plan + price + setup + what's included).
6. **Pilot (optional):** 1‑month paid pilot or 30‑day trial on a discounted plan to
   de‑risk their decision.
7. **Close:** sign, collect setup fee + first month.
8. **Handoff to onboarding.**

**Objection handling:**
- *"Too expensive"* → compare to their monthly cost of chaos/leakage.
- *"We'll build our own"* → time + cost + maintenance; you're live today.
- *"Is our data safe?"* → point to your security posture (fix §10 first!).

---

## 9. Onboarding & retention (keep them paying)

**Onboarding (first 1–2 weeks):**
- Create their tenant/account, configure branding, rate cards, hospitals, service areas.
- Import agents/vehicles; install the apps on field staff phones.
- Train admins (portal) + agents (app) — 1–2 short sessions + a quick guide.
- Run 2–3 real cases together; confirm go‑live.

**Retention (ongoing):**
- Monthly check‑in + usage report (cases handled, time saved).
- Fast support (WhatsApp/email SLA).
- Quarterly upsell review (more agents, new modules, white‑label).
- Track **adoption** — a customer who uses it daily never churns.

---

## 10. Before you sell — readiness blockers (fix these)

| # | Blocker | Why it matters | Fix |
|---|---|---|---|
| 1 | **Secrets in a public repo** 🔴 | DB password + JWT secret are in `application.yml` in a **public** GitHub repo — an instant "no" from any serious buyer | Move secrets to **env vars**, rotate them, make the repo **private** |
| 2 | **Single‑tenant** | B2B SaaS needs **one deployment serving many customers** with isolated data | Add **multi‑tenancy** (tenant ID scoping + data isolation) |
| 3 | **Push + payments not live** | FCM is scaffolded but off; payment/settlement rails need wiring | Activate **FCM**; integrate a **payment gateway** for production |
| 4 | Compliance/trust | Healthcare data = privacy expectations | Data encryption, access controls, audit logs, a basic DPA/privacy policy |
| 5 | Reliability | Customers run operations on it | Backups, monitoring/uptime, support process |

**Minimum to demo safely:** fix #1 now. **Minimum to sign paying B2B customers:**
#1 + #2 + #3.

---

## 11. Metrics to track (your SaaS dashboard)

- **MRR / ARR** (monthly/annual recurring revenue)
- **New customers, churn rate, net revenue retention**
- **CAC** (cost to acquire a customer) vs **LTV** (lifetime value) — aim LTV:CAC ≥ 3
- **Activation** (did they go live?) and **adoption** (daily active usage)
- **Cases processed per customer** (drives per‑case revenue + proves value)

---

## 12. Risks & how to handle them

| Risk | Mitigation |
|---|---|
| Long enterprise sales cycles | Start with small operators who decide fast |
| Customers want customization | Standardize; charge for custom work; build config, not forks |
| Support load grows | Self‑serve docs, in‑app help, tiered support by plan |
| One big customer = concentration | Diversify to 10+ customers early |
| Regulatory/data concerns | Compliance posture (§10) from day one |

---

## 13. 30 / 60 / 90‑day action plan

**Days 0–30 — get sellable**
- Fix security (#1), rotate secrets, private repo.
- Make a 1‑page pricing sheet + a 10‑min demo script.
- Build a list of 30 ICP prospects.
- Register GST / set up invoicing + a payment link.

**Days 31–60 — first customer**
- Add basic multi‑tenancy (#2) + activate push (#3).
- Do 15–20 outreach calls, 5–8 demos.
- Run 1 paid pilot. Refine pricing from real feedback.

**Days 61–90 — repeat & systemize**
- Convert pilot → paying annual contract.
- Close 2–3 more customers.
- Stand up subscription billing + a simple onboarding checklist.
- Start tracking MRR, activation, churn.

**Goal by day 90:** 2–3 paying B2B customers, a repeatable demo + onboarding, and
a clear price list — i.e., a real, recurring‑revenue business.

---

## 14. One‑line summary

> **Sell the software, not the service.** Rent your command‑centre platform to
> care businesses for a **recurring monthly subscription** (flat tiers to start,
> per‑case as you grow), **price on the value you create**, fix the security +
> multi‑tenancy blockers first, land one anchor customer, then repeat.

*Document generated as a working playbook — adjust prices and segments to your
local market and your first customers' real economics.*
