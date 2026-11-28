# Ranked Finance — Design

Status: approved Nov 18, 2026

## Intent

A personal finance app built as a ranked game for money: you climb from Iron to Legend (financial independence) by following your own plan. Net worth comes first on Home, the plan is built from your real accounts, and an AI that knows your plan and rank answers questions on every page.

**Why:** People ask AI chatbots the same money questions over and over: what to do next with a dollar, how much is enough, when they'll hit a goal, keep or switch. Each time they re-enter balances and re-explain their plan, and nothing keeps a record of their net worth over time. An app that already holds the numbers and the plan answers those questions with less friction than a chat that starts cold.

**Name:** Ranked Finance ("Road to Legend" is the name of the rank feature). The closest name conflict found, Rank (formerly Moni), is a small app outside the U.S. and judged low-risk. A trademark search in the software and financial-services classes is due before any public launch.

**Who:** one user at first, starting with the maintainer. A business is possible later, so v1 avoids choices that would block multiple users, but builds nothing for them yet.

**When:** the build starts in January 2027.

## The problems it solves

Recurring money questions fall into seven problems. Each one is a question the app should answer without the user re-entering balances or re-explaining their plan.

| Problem | Typical questions | How the app answers it |
| --- | --- | --- |
| **Where should the next dollar go?** The most frequent one. | Pause retirement contributions until the emergency fund is full? Pay off the car loan or keep building savings? Use savings to clear credit card debt? | The plan's step order plus current balances tells the AI which step the user is on. It answers with the next move and the numbers behind it. V1. |
| **How much is enough?** | How much to keep in savings versus investing? How big should the emergency fund be for my actual expenses, and after a job change? | Goals carry explicit targets with progress bars. The AI reasons from the user's plan notes and real spending rather than generic "3 to 6 months" rules. V1. |
| **When will I hit it?** | When will I be debt-free? What will my net worth be by a given date? What does retirement look like at 65? Can I afford a house by 35? | Projections computed in code from the recent savings rate, shown as a date per goal; the AI explains them. V1. |
| **What's my net worth, and how has it changed?** | Nothing tracks it, so past net worth gets pieced together from memory or old statements. | Daily snapshots and the net worth chart on Home. V1. |
| **Keep it or switch?** | Keep the car or buy a cheaper one? Consolidate banks or keep two? Roth or traditional? | The AI compares both paths in dollars against the plan. No dedicated screen in v1; it's a chat question with full context. |
| **Account mechanics** | Contribution limits and deadlines, which tax year a deposit counts toward, cash sitting uninvested, accounts that won't link. | V1: the AI answers with the user's accounts in view. Later: alerts for deadlines and uninvested cash. |
| **Planning around life changes** | Moving out, a job change, income that swings with tips, overtime, or travel pay. | Life events with dates and expected cost changes in v1. "What if income drops" scenarios are a later idea. |

The common thread: almost every question is "here is my situation, what is the right next step within my plan?" That's why the AI reads the plan and the numbers before every answer, and why keeping the user focused on the plan matters more than surfacing news.

## Goals, non-goals, anti-goals

**Goals (v1)**

- Accounts sync with no manual work, as simply as Monarch or Copilot, through Plaid or SimpleFIN. Any bank's CSV, OFX, or QIF export imports in a few clicks, and any account can be entered by hand.
- Easy to self-host (`docker compose up`), with the bank-data provider and the AI provider both swappable.
- One net worth number with its history, built from daily balance snapshots.
- Ranks from Iron to Legend, checked in code, with real demotion. The plan is the route up: rank steps built from the user's accounts, plus personal goals they add.
- An Ask panel on every page that reads the plan, rank, and current numbers before every answer, and keeps answers tied to that plan.
- Desktop web app first. The layout still works in a phone browser; a dedicated mobile app comes later, only if the app proves itself.

**Non-goals (v1)**

- Monarch- or YNAB-style category budgets (a limit per category, rollovers). V2 adds a plan-linked budget instead (see V2).
- Multiple users, signup, billing, or onboarding for strangers.
- Native iOS or Android apps.
- Moving money, trading, or anything that writes to an account.
- The "why it moved" breakdown, the Money screen (budget and bills), How you compare with per-rank peer shares, and notifications beyond sign-in alerts. These come in v2. Browser push notifications are out entirely; push comes with the phone app.

**Anti-goals (failure even if it works)**

- A headline feed about individual companies. For someone whose money is mostly in index and target-date funds, single-stock news is noise that pulls against staying focused.
- An AI that quietly drifts from the plan the user decided on, or gives advice that ignores it.
- An app people stop opening because connections break and they have to fix them by hand.
- Ranks that feel unfair: decided by the AI, flipping daily with the market, or skippable by deleting steps.

## Constraints

- **Plaid Trial plan: 10 Items, lifetime.** It's free and uses real production data, with no business registration ([Plaid](https://support.plaid.com/hc/en-us/articles/39994173227159-What-is-the-Plaid-Trial-plan)). An Item is one login at one institution. Removing an Item does not give the slot back ([Plaid](https://support.plaid.com/hc/en-us/articles/39917307426967)). A typical setup (a bank, a savings app, a brokerage, a 401(k) provider, an HSA, an auto lender) uses about 7 Items. So all development and testing runs in Plaid Sandbox, and real accounts connect once, deliberately. Access tokens must be persisted and never lost.
- **Fidelity may not connect.** Plaid gates Fidelity behind a separate access request on some plans ([Plaid docs](https://plaid.com/en-gb/docs/investments/)), and the Trial covers "most", not all, of the institutions that use their own sign-in flow. Any account must be able to fall back to manual balance entry. Some cards (such as Apple Card) may need the same.
- **Upgrading off the Trial is one-way.** That's fine for later, but no need to apply early.
- **Security.** The app holds live read access to every account the user has. Even with one user it needs real authentication and encrypted provider tokens (Plaid and SimpleFIN) at rest.
- **Timing.** The build starts in January 2027.
- **Skills.** The backend is in Java with Spring Boot, chosen for long-term maintainability and as a learning goal, accepting that v1 will take longer. The frontend is React and TypeScript.

## Approaches

The chosen approach is **A: net worth first.** Plaid plus snapshots plus the plan plus AI chat makes something usable within a few weekends. The feed needs weeks of snapshot history to explain anything anyway.

| Approach | What v1 includes | Trade-off | Decision |
| --- | --- | --- | --- |
| A. Net worth first | Plaid sync, daily snapshots, net worth chart, plan page, AI chat with plan context | Feed and alerts wait for v2 | **Picked** |
| B. Everything at once | A plus the "why it moved" feed, breakdown of each change, push notifications | Much longer before it's usable; the feed is empty until history builds | Rejected |
| C. AI first, manual balances | AI chat plus hand-entered balances, Plaid later | Fastest AI demo, but drops the "as simple as other apps" requirement | Rejected |

Earlier choices made along the way:

- **Data in:** Plaid over manual check-ins or CSV imports, for zero-effort sync. Manual entry stays as a per-account fallback. Updated Nov 19, 2026: Plaid and SimpleFIN both ship in v1 behind one provider interface, and file import (CSV, OFX/QFX, QIF) is a first-class source. See Data sources.
- **Platform:** desktop web app first, with a mobile app later if it earns one. The earlier pick was an installable phone web app; the phone mockups stay as the reference for that later app. The frontend is React.
- **News:** "why your net worth moved" over a headline feed, because most people's holdings are diversified funds.
- **Main feature:** a universal rank ladder (Iron to Legend) with demotion, over plain goal tracking. Chosen to give the app a reason to open every week and something people want to share.

## Ranks: the main feature

The app is a "Road to Legend": you rise through ranks by pursuing your own money goals. Ranks are the same for everyone and follow standard financial advice. Your plan's steps are your personal route up.

| Rank | Milestone |
| --- | --- |
| Iron | Accounts connected, tracking net worth |
| Bronze | $1,000 starter emergency fund (your emergency fund account plus any taxable brokerage) |
| Silver | No high-interest debt (anything over 8% APR). No employer-plan requirement, since not everyone has one |
| Gold | Emergency fund account plus taxable brokerage hold 3 months of your actual spending |
| Platinum | All non-mortgage debt paid off, or low-rate debt you could pay off today, and 15% of income invested (retirement accounts, a taxable brokerage, or HSA investments all count) |
| Diamond | $100,000 net worth |
| Master | $250,000 net worth |
| Grandmaster | $500,000 net worth |
| Legend | Financial independence: invested assets of 25× your yearly expenses (the 4% rule), calculated from your actual spending |

- **Your starting rank is instant.** At first-time setup, the app pulls up to 24 months of transactions when you link accounts (Plaid's `days_requested`, set when Link starts), so your 3 months of spending and your income are known on day one. The rank reveal uses your balances as they are that day, with no 14-day wait and no 60-day spending-history wait. Someone who joins with $25k saved, no high-interest debt, and $120k net worth starts at Diamond. After signup, every rank change follows the 14-day rule, including a rise from an account you link later. (Decided Nov 18, 2026.)
- **Ranks stack.** Your rank is the highest rank whose requirement you meet *and* every rank below it. To hold Diamond, you must still meet Bronze through Platinum, so someone with $150k in a 401(k) but no emergency fund and a 24% credit card is Iron, not Diamond. Income never counts on its own; what you've built and what you owe does. (Decided Nov 18, 2026.)
- **Your rank is where you are now, and you can drop.** Keeping wealth is part of the game. You earn a rank by staying at or above its line for 14 days, and you drop after 14 days straight below it. The app warns you on day 1 ("Diamond at risk, 13 days to recover"). The buffer stops market wobbles from flipping your rank daily. New high-interest debt drops you immediately.
- **The emergency fund is an account you choose, plus a taxable brokerage.** In Settings & accounts (and during first-time setup), you mark one or more cash accounts, linked or hand-entered, as your emergency fund. Bronze, Gold, and the Legend keep-it step count those accounts plus any taxable brokerage at full balance, since you can sell investments in a few days. Retirement accounts, HSAs, home equity, and other assets never count toward the emergency fund. Checking doesn't count unless you mark it. If you have neither a marked account nor a brokerage, the Bronze step reads "Choose your emergency fund account." The 14-day rule applies to changes after signup. (Decided Nov 18, 2026.)
- **Ranks are checked in code** from balances, debts, and Plaid data. The AI explains what it would take to rank up, but never decides your rank.
- **Comparison:** each rank shows the share of unmarried men under 35 (or your own group) who have reached it, computed from survey microdata. Net worth tiers come straight from the data; emergency fund tiers are estimated from liquid savings.
- **Where it shows:** Home keeps net worth first, with a rank badge right beside it ("Silver · 82% to Gold"). Desktop Home runs top to bottom: net worth (full width, with the chart), the Cash / Investments / Liabilities cards, then a full-width Road to Legend band, then This month. The band has the headline ("Silver · 82% to Gold"), a track of all nine ranks with your position marked, "To reach Gold" with a progress bar (which is also step 1 of your plan), and what Platinum needs next. The Ranks page (in the sidebar) has two tabs: Ladder (the full ladder with Reached, In progress, Next, and Locked states, plus how ranks work) and How you compare (the full peer comparison).
- **Notifications:** rank-ups, "at risk" warnings, and drops are notification types.
- **Drops are framed as a path back, not a verdict** (from the competitor research). "At risk" warnings lead with the fix in dollars and days ("$400 more by Dec 3 keeps Diamond"). A drop shows what it takes to earn the rank back and the recovery step, never language like "you lost" or "you failed." The rank history shows earned-back ranks as comebacks.
- **Rank thresholds are content, not code:** they're stored as data so they can be tuned later without a release.

**How the numbers are read** (decided Nov 18, 2026)

- **Credit cards count as debt only when they carry a balance.** A card is debt if its last payment was less than its last statement balance, or interest was charged; both come from Plaid Liabilities. Someone who pays the card in full every month has no card debt, and a purchase never drops their rank. A card on a promotional rate (say 0% on a balance transfer) is judged by the rate applied to the balance, not the regular purchase rate.
- **Spending excludes money that isn't spent.** Transfers between your own accounts, credit card payments (the card purchases count instead), and money invested are not spending. Gold's target uses your typical month: the median monthly spending over the last 6 months, so one big purchase like a car down payment doesn't spike it.
- **Low-rate debt you choose to keep doesn't block Platinum.** A loan under 5% APR counts as covered when you hold enough to pay it off today, outside what Gold already needs: emergency fund and taxable brokerage beyond 3 months of spending, which together must be at least the loan balance. In the plan, a covered loan shows as "Covered: keeping it is your choice" instead of a payoff step. If the cover drops below the balance, the 14-day rule applies as usual. Until a low-rate loan is covered, its Platinum step offers two routes and the user picks one: "Pay it off" or "Build the cover" (grow the emergency fund and brokerage beyond Gold's target until they match the balance). The AI shows the trade-off in interest paid versus investment growth, and the user can switch routes later. Loans at 5–8% still need paying off for Platinum, and anything over 8% still blocks Silver.
- **Every source counts the same.** Hand-entered assets (a car, other property, a hand-entered account) and imported accounts count toward net worth and ranks exactly like linked ones, with no "self-reported" or "verified" label on accounts, ranks, or anything shared. No aggregator reaches every institution, so the source says nothing about honesty. (Decided Nov 20, 2026; replaces the Nov 18 labeling rule.)
- **Legend's number leaves out debt payments.** Legend is 25× your yearly spending without debt payments, since Platinum already means no debt beyond a mortgage or covered low-rate loans. Mortgage payments stay in, because housing doesn't end when the loan does: property tax and insurance continue.
- **Student loans count like any other debt,** including while in school, in grace, or in deferment: a loan over 8% blocks Silver whether or not a payment is due, the same as Ramsey's Baby Step 2. Someone in medical school may sit at Bronze for years; that's intended. (Decided Nov 20, 2026; considered and rejected: exempting deferred loans from Silver, and a projected-rank display.)
- **Platinum's 15% needs income.** The investing step can't be met with no income source, so 15% of $0 never passes automatically.
- **Crypto never counts as emergency fund.** It counts toward net worth and ranks like any investment, but Bronze, Gold, and the Legend keep-it step ignore it, because it can drop by half. The "taxable brokerage counts" rule means stocks and funds only.
- **No shared accounts.** Each user's accounts and rank are theirs alone. There are no joint-account splits, household ranks, or partner access, now or later. If an account is jointly owned, it counts in full for the user who links it.

**How plans work with ranks**

- The plan is your route up the ladder. It has two kinds of steps: **rank steps**, which are added automatically, and **personal goals**, which you add yourself (move-out fund, house, a trip).
- **Rank steps are fixed.** They always appear in ladder order, and they can't be moved or removed. Each one shows a lock and an "Unlocks Gold / Platinum / Diamond" tag; a rank with two requirements becomes two steps ("Platinum · 1 of 2").
- **Personal goals can sit anywhere between rank steps** and can be dragged. The AI shows what a placement delays and can suggest a move ("Start the move-out fund after Gold? Delays Platinum about 5 weeks").
- Money that's "free for your plan" goes to the first unfinished step, so progress on step 1 is also progress to the next rank.
- If you drop a rank, a recovery step goes to the top until it's fixed.
- Rank milestones adapt where the advice allows: "3–6 months of expenses" is calculated from your actual bills.

* **Rank steps are personalized: one step per action, built from your real accounts.** Each rank's requirement expands into concrete steps. Silver becomes one step per card or loan over 8% APR (two credit cards make two steps); Gold becomes one emergency-fund step sized from your expenses; Platinum becomes one step per remaining non-mortgage debt plus the 15% investing step. Steps are grouped under their rank in the plan.
* **Payoff order within a rank:** for debt steps you choose highest APR first (the default, saves the most interest) or smallest balance first (quicker wins). The steps reorder automatically, and the AI shows the tradeoff in dollars and weeks. Other debts stay at minimum payments until their step comes up.
* **Steps update themselves:** a new card adds a step; a paid-off loan completes one. Balances and APRs come from Plaid (its Liabilities data covers credit cards and student loans); for other loans, like auto loans, the user enters the APR once.
* **Rank steps from Diamond to Legend** (decided Nov 21, 2026). These ranks are about how much you've built rather than a list of debts, so each one is a single step showing its target, your progress, and a projected date.
  * **Diamond, Master, Grandmaster:** target is the rank's net worth ($100k, $250k, $500k). Progress is net worth, home equity and every asset included.
  * **Legend:** target is your independence number, 25× yearly spending without debt payments (mortgage payments stay in). Progress counts retirement accounts, HSAs, taxable brokerage, crypto, and cash; home equity, cars, and other property are left out because you can't live off them. Ranks stack, so Legend still waits for Grandmaster; if the independence number is below $500k, both unlock together.
  * **Projected dates** use what you actually add each month plus a conservative growth estimate for investments (plans may use estimates; ranks never do). The step names the assumption ("assumes about 5% a year after inflation"). The growth rate is stored as data, like rank thresholds, so it can be tuned without a release; because it's after inflation while the Diamond to Grandmaster targets are fixed dollar amounts, those dates lean late, which is intended.
  * **Code computes the options with real numbers, the same as "When a goal isn't reachable":** investing more ("$1,000 more a month: about 2 years sooner"); spending less, which for Legend works twice, since every $1 less spent a year lowers the target by $25 and frees that dollar to invest; and, where there's a mortgage, paying it down versus investing the same money, in dollars and dates (paying it off lowers the Legend number because the payment leaves your spending). The AI explains the options; the user picks.
  * **Life events move these steps too:** a planned cost change updates the independence number and every projected date from the event's date on.
  * Steps for lower ranks never disappear: if a lower rank stops being met, its recovery step goes back to the top, as with any rank.
* Mockups: the Plan screen, plus a "Plan example: two credit cards" artboard showing a Bronze user with two credit card steps (Card A and Card B) and the payoff-order toggle.

**Life events** (decided Nov 18, 2026)

- **Any personal goal can be a life event.** It has a name you choose (move out, new car, baby, wedding, new job, anything), plus an optional date, an optional one-time cost (the amount to save, like any personal goal), and optional expected monthly cost changes. A cost change can be positive (rent starting) or negative (daycare ending).
- **The app suggests amounts for common events.** Picking a preset like "Move out" pre-fills a one-time cost (deposit + first month's rent + moving + a furniture allowance) and prompts for rent and utilities. Every number is editable. A custom event starts blank.
- **Plans use the change from its date on.** After the event's date, projections use your spending plus the expected change, so later steps get realistic dates and the money free for your plan changes on that date.
- **Gold shows both targets before the event:** "Gold today: $9k · after moving: $12.6k." The plan suggests building toward the after-event target before the date.
- **Ranks only use real numbers.** A planned cost never changes your rank before it actually shows up in your spending. Plans can use estimates; ranks can't.
- **After the date,** the app replaces the estimate with real numbers from your transactions once they appear, and updates the plan.
- This replaces the earlier idea of a card for users living with parents; telling the app your plan beats it guessing from your living situation.

**Big-bill suggestion card** (decided Nov 18, 2026)

- A recurring payment triggers one suggestion card when it takes more of your take-home pay (actual deposits over 90 days) than is normal for its type: **housing** (rent, mortgage) over 40%, **vehicle** (auto loan or lease) over 15%, **anything else** over 25%. Rent at a third of take-home is normal and never triggers it; a $2,000 car payment on $5,000 take-home (40%) does. Each payment's type comes from its category (see Categories and transfers), and the user can change it. The card compares keeping it, paying it off early, refinancing, and selling or replacing (for housing: a cheaper place or a roommate at the next lease renewal), in dollars per month and in rank dates. Code computes the numbers; the AI explains them. Dismissed cards don't return unless the payment or income changes by more than 10%. The thresholds are stored as data, like rank thresholds, so they can be tuned without a release.

**When a goal isn't reachable** (decided Nov 18, 2026)

- **Code finds it, not the AI.** When a step's projected date lands after its target date, or never at the current pace, the step shows "Not reachable at this pace" instead of a date.
- **Code computes every option, with real numbers:** the date the current pace does reach ("$15k by August 2028, not October 2027"); the amount reachable by the target date ("about $11k by October 2027"); the specific spending categories or bills that would close the gap ("$180 a month less on eating out"); and, only when it's real for that person, income: for hourly, tips, or gig income, extra shifts in dates ("one extra shift a week reaches it in June"). Salaried users don't get "ask for a raise" advice unless they ask about income.
- **The AI explains the options and says which one moves the date most, but doesn't choose.** The user picks. A later date or a smaller goal is a valid plan, not a failure.
- **Spending more than you bring in gets its own first step.** If spending exceeds income for two months in a row, a locked step goes to the top of the plan: "Spend less than you bring in," with the monthly gap and the biggest categories. Money free for the plan is $0 until it's fixed, so other steps show as waiting rather than late.
- **Tone:** never shame, never call a goal unrealistic, never lead with "make more money."
- **Fairness check:** Bronze through Platinum scale to the user's own spending and income, so they're reachable at any income. Only Diamond through Grandmaster are fixed dollar amounts, and Legend scales back to the user's spending.

**After Legend**

- **Legend is financial independence**, the true finish. Ladder order still holds: if someone's FI number is below $500,000, Legend unlocks when they reach Grandmaster, since they're already independent by then.
- **Keep-it steps (auto-built, like the debt steps):** the emergency fund still covers 6 months; no single stock or account above a set share of net worth; not too much cash sitting uninvested; beneficiaries set on accounts and a will in place. Legend can still drop, like any rank.
- **Personal goals lead:** with the ladder done, the plan is mostly the user's own goals (house paid off, early retirement date, giving, funding a business). The AI's main question becomes "can I afford this and stay Legend?"

**How you get paid**

- Each income source has a type (fixed salary, hourly, tips, commission, per diem, gig work, or other), a schedule (weekly, every 2 weeks, twice a month, monthly, or irregular), and the account it lands in. The app proposes sources from repeating Plaid deposits, and the user confirms or edits them. They're edited in Settings & accounts and summarized on the Plan page.
- **Plans use steady pay.** Fixed pay is planned as-is. Variable pay (tips, hourly, overtime) is planned at the user's slower weeks, roughly the low end of the last 12 weeks, so dates hold up in a slow month. A bartender averaging $750 a week, ranging $520 to $980, is planned at about $600.
- **Extra pay goes to the current step.** Anything above the planned amount (a big tip week, overtime, per diem kept) flows to the first unfinished step, and the app shows what it bought ("pulled Gold 9 days closer").
- **Work more, rank up sooner.** For users who can pick up more hours or shifts, the Plan shows the payoff in dates ("one extra shift a week reaches Silver in February instead of March"), and the AI can answer "is it worth picking up Saturdays?"
- Platinum's "15% of income" and the budget's "came in" use actual deposits; plan dates use the planned (steady) amount.
- A new income_sources table holds type, schedule, planned amount, range, and account.

**About you**

- An "About you" section in Settings & accounts (and in first-time setup) asks for age, sex, marital status, living situation (on your own, with a partner, with parents), and country. It shows the resulting comparison group ("Unmarried men under 35"), and How you compare links to it ("Edit about you").
- Used only for ranks' peer shares and How you compare: never shared, and never given to the AI beyond the comparison group's name.
- Every field is optional, with "Prefer not to say." A skipped field widens the group (for example, all adults under 35), and the comparison says which group it used.
- Living situation helps with the open SIPP question: people living with parents can be compared against single adults in their age group, rather than only those heading their own household.
- The profile row on users holds birth year, sex, marital status, living situation, and country, all nullable.

## V1 design

V1 is a desktop web app with six pages (Home, Plan, Ranks, Account detail, Settings & accounts, Notifications), an Ask panel on every page, bank sync through Plaid or SimpleFIN plus file import, and an AI that answers from computed numbers, the plan, and the rank.

### Releases (decided Nov 20, 2026)

- **v1 is the first demoable product.** The core loop works end to end on Plaid Sandbox, manual accounts, and the demo data: add accounts, see net worth, get a starting rank in first-time setup, follow the plan, and ask the AI (Claude). It's what goes to early testers.
- **1.0 is the first public, self-hostable release.** It adds the rest of this section: SimpleFIN, file import with the other-app formats, AI category suggestions, the other AI providers, USD/CAD conversion, OIDC sign-in, the Docker image and self-hosting guide, backups, the update notice, API tokens, export for moving between installs, and passkey and email recovery. Then the hosted deployment.
- Correctness rules stay in v1, because ranks are wrong without them: pending transactions, refunds, transfer matching, starter category rules, the time zone, and the performance target.
- Where this document says "v1" about a feature, ROADMAP.md says whether it lands in v1 or 1.0.

### Screens

1. **Home:** top to bottom: **net worth** first (one large number, the change this week, and a rank badge, "Silver · 82% to Gold"), with a clean line chart you drag across to see the amount and date on the line, start and end dates underneath, and 30D / 1Y / All. Then the **Cash, Investments, Liabilities** cards, each with a subtotal; every account row shows when it last updated, hand-entered balances past their update reminder are flagged in amber, and broken sign-ins say "Needs reconnect." Clicking a row opens Account detail. Then the full-width **Road to Legend** band (rank track, what the next rank needs, and what comes after). Then **This month** (v2 budget summary).
2. **Plan:** your route up the ladder. Rank steps are generated from your accounts, grouped under their rank, locked in ladder order, and tagged with what they unlock. Personal goals have a dashed border and a drag handle and can sit between rank steps. A notes box for the AI, and suggested-change cards you accept or dismiss.
3. **Ranks:** two tabs. **Ladder** shows all nine ranks (Reached, In progress, Next, Locked), how ranks work, and your rank history. **How you compare** is the peer comparison (v2 data).
4. **Account detail:** balance, status, last update, the goal or rule it funds, a search box, and transactions grouped by month with money in and out; recurring charges are tagged.
5. **Settings & accounts (behind the gear):** one card per bank login with status and Reconnect, a balance field for hand-entered accounts, which goal each account funds (including which account is your emergency fund), the Plaid connection count, and notification settings.
6. **Notifications (behind the bell):** the in-app inbox, grouped Today, This week, Earlier, with Reconnect and Confirm actions inline.

Clicking an account row anywhere opens its Account detail page; there are no separate "Manage" links.

**Navigation.** The desktop sidebar holds four pages (Home, Plan, Money, Ranks) plus the account-health line ("5 up to date · 1 needs attention"). Top-right on every page: an Ask button, the bell (with an unread count), and the gear (Settings & accounts). Ask opens a chat panel that slides in from the right and knows which page you're on, so on Money, "can I afford a $300 trip?" is answered with the budget and bills in view. At phone width, the sidebar collapses into a top bar with a menu button (Home, Plan, Money, Ranks, Ask) and the bell and gear; there are no bottom tabs. The tabbed phone mockups are the reference for the later native app, where bottom tabs are the platform norm. On desktop, the sidebar stays fixed while the main page scrolls, and the Ask panel and dialogs stay pinned to the window. Theme: white, black, and one purple accent (#5B3FD6); green for gains, red for debt, amber for warnings.

### Sync

- Development and tests run against Plaid Sandbox only. Production Items connect once each (see Constraints).
- Plaid updates on its own schedule: it checks each bank for new transactions one to four times a day, depending on the institution, and fires the SYNC_UPDATES_AVAILABLE webhook when it finds new data. The backend listens for that webhook and pulls changes with /transactions/sync, instead of polling. ([Plaid docs](https://plaid.com/docs/api/products/transactions/))
- Cached balances (free, from /accounts/get) only refresh after Plaid has checked fresh transactions. Each Item stores Plaid's last successful update time, and that drives the per-account "Updated ..." labels. ([Plaid support](https://support.plaid.com/hc/en-us/articles/15373441901207))
- The daily net worth snapshot runs after the morning updates arrive (default 6:00 AM, adjustable in Settings), not at a fixed time before them. It writes one row per account per day.
- There is no "Sync now" button and no forced refresh (/transactions/refresh or real-time /accounts/balance/get), so nothing has to change if this becomes a business. Balances update as fast as Plaid does: the moment a webhook says new data is ready, the backend pulls it and updates the app.
- There's no single "Synced" time on Home. Instead, a health summary (for example "5 up to date · 1 needs attention") links to Settings & accounts, because net worth is only as fresh as its stalest account.
- Plaid webhooks flag logins that need re-authentication. The app sends the user through Link's update mode to fix them. This doesn't use up a new Item.
- Manual accounts store whatever the user enters. Each one has an update reminder (every 2 weeks, monthly, quarterly, or never; monthly by default, since most people update from monthly statements), and the app nudges the user once an account is past it. A stale balance still counts at its last value; the reminder never changes the numbers. (Decided Nov 28, 2026; replaces a flat 14 days.)
- **Hiding an account is display-only.** Any account can be hidden from Home and other screens (an old $3 savings account Plaid pulls in with a login, say). A hidden account still counts toward net worth, the Home card totals, and ranks, and Settings & accounts always lists it, dimmed, with a toggle to unhide it. A hidden hand-entered account gets no update reminder; a broken connection still needs attention even when all its accounts are hidden. Nothing can be left out of net worth: the only way out is marking an account closed. (Decided Nov 28, 2026.)
- **Any account can be added by hand.** "Add an account" offers Plaid, SimpleFIN, file import, or manual (whichever are configured; see Data sources). A manual account can be cash, an investment (taxable brokerage, retirement, HSA, or crypto), or a debt (balance, APR, minimum payment). This covers debts Plaid can't see (buy now, pay later loans, medical bills, money owed to family or the IRS, small lenders) and institutions Plaid can't reach. Manual accounts count toward net worth and ranks exactly like linked ones. First-time setup asks "Any accounts or debts we can't see?" (Decided Nov 18, 2026.)
- **Closed is not stale.** A stale account (a broken connection, or a manual balance that hasn't been updated) keeps counting at its last known balance. A closed account counts as $0 from the day it closes, and its history stays. An account is closed when its provider stops returning it or the user marks it closed.
- **No double counting.** When a new account matches an existing one (same institution, last 4 digits, and kind, or a manual account with the same name and kind), the app asks "Is this the same as X?" before counting it. If it is, the linked account replaces the manual one and the history merges.

### Data sources (decided Nov 19, 2026)

Every source writes to the same normalized tables (connections, accounts, transactions, balance_snapshots). Ranks, the plan, and the AI read only those tables, never a provider's own fields, so a provider can be added or swapped without touching them. Each bank-data source implements one `DataProvider` interface: connect, list accounts, sync balances and transactions, report connection status, and reconnect. This is how Actual Budget and Sure handle several aggregators. See [ADR 0007](adr/0007-swappable-data-providers.md).

- **Plaid** (the hosted version, and self-hosters with their own Plaid developer account): webhook-driven, as described in Sync. It is the only source that supplies Liabilities (APRs) and investment transactions.
- **SimpleFIN** (self-hosters in the US and Canada): the user subscribes to SimpleFIN Bridge themselves (about $15 a year, up to 25 institutions) and pastes a setup token; the app exchanges it for an access URL and stores that encrypted, like a Plaid token. No developer keys are needed on the server. SimpleFIN has no webhooks, so a scheduled job pulls once a day before the snapshot. It provides balances and transactions but no APRs or contribution detail, so the existing fallbacks apply: the APR is entered once, and investment contributions come from transfers into investment accounts or a monthly figure the user enters. ([SimpleFIN protocol](https://www.simplefin.org/protocol.html), [Bridge pricing](https://beta-bridge.simplefin.org/))
- **File import** (anyone, any bank): CSV, OFX/QFX, and QIF files, into a new or existing account.
  - Steps: pick the account, upload the file, check the mapping (CSV only), review, import. For CSV the app guesses the date, description, and amount columns (or separate debit and credit columns), the date format, and whether spending is negative, then shows the first rows as they'll be imported so the user can correct anything before continuing.
  - The mapping is saved per account, so the next file from the same bank imports with no questions.
  - Duplicates: OFX/QFX by the bank's transaction ID; CSV and QIF by date, amount, and cleaned-up description. Matches are flagged on the review screen, never dropped silently, and repeats inside one file are kept (two real $5 coffees on the same day).
  - Balances: OFX files include an ending balance. For CSV and QIF the user enters the current balance once, and the app works earlier daily balances backward from the transactions.
  - Imported history counts toward the instant starting rank and Gold's 60 days of spending history, so someone who never links a bank still gets ranked on day one.
  - Imported accounts get the same update reminder as manual accounts.
  - **Imports from other apps:** built-in formats for the CSV exports of Monarch, YNAB, Copilot, Actual Budget, and Mint, so a switcher's full history (accounts, transactions, and their categories) arrives in one step and their starting rank is right on day one. (Decided Nov 20, 2026.)
- **Manual:** as described in Sync.
- Which sources appear is configuration. The hosted version turns on Plaid; a self-hosted install turns on whatever it has credentials for (SimpleFIN, file import, and manual need none). A source that isn't configured doesn't appear in "Add an account."

### Self-hosting (decided Nov 19, 2026)

- `docker compose up` runs the app and Postgres. The app ships as one container image (Spring Boot serving the built frontend), published to GitHub Container Registry with each release.
- Everything is configured through environment variables: database URL, encryption key, optional Plaid keys, and AI settings. AWS (KMS, Secrets Manager, managed Postgres) is how the hosted version runs, never a requirement of the code.
- The encryption key comes from an environment variable on a self-hosted install and from KMS on the hosted version. Losing it means re-linking every account, so the self-hosting guide says to back it up alongside the database.
- Flyway runs migrations on startup, so upgrading is pulling the new image and restarting.
- A `SELF_HOSTING.md` (setup, backups, upgrades, which sources to configure) ships with the first release.
- One user per install in v1, the same as the hosted v1.
- Every secret setting also accepts a `_FILE` variant (for example `ENCRYPTION_KEY_FILE`) so Docker secrets work, and the app runs correctly behind a reverse proxy at any base URL or subpath. (Both are common self-hoster requests.)
- **Update notice:** the app checks GitHub's latest release once a day and shows "Version X is available" in Settings. Nothing about the install is sent; it can be turned off. (Decided Nov 20, 2026.)
- **One-command backup:** a documented command (and a script in the repo) dumps the database to a file, and the guide reminds you the encryption key must be backed up separately. Restore is the same command in reverse, and the guide includes a test restore.
- **API:** the OpenAPI spec the frontend already uses is the public API. Users create read-only API tokens in Settings for their own scripts. Writes through the API wait until after v1.
- **Export for moving installs:** a full export (accounts, history, categories, rules, plan, and settings, but never provider tokens) that a fresh install imports completely, so people can move between the hosted version and their own server in either direction. No lock-in either way. (Decided Nov 20, 2026.)
- **Health check:** a `/health` endpoint so Docker can restart the app if it stops responding.
- **Demo data:** a command seeds an install with a fake person's accounts and history. Development needs it from Milestone 4 anyway, and it makes the README screenshots and the project website without showing real money. A public hosted demo isn't needed, since anyone can self-host one.

### Performance (decided Nov 20, 2026)

Slowness in self-hosted finance apps usually comes from the database and the UI, not the language: missing indexes (Firefly III) and transaction lists with no pagination (Maybe). So speed is designed in and checked, not hoped for.

- **Target:** every page loads in under 500 ms, and the Ask panel's context is built in under 200 ms, with 10 years of history (about 50,000 transactions across 15 accounts), on a self-hosted install with 1 GB of RAM for the app and Postgres together.
- **Test data at that size:** the demo-data command generates 10 years of history, so every milestone from 4 on is tried at full size, not with a handful of rows.
- **Indexes ship with the tables.** Every migration that adds a table adds the indexes its queries need (for example, transactions on user, account, and date). A slow query gets an `EXPLAIN` and a fix before the feature is done.
- **Lists load a page at a time.** Transaction lists are paginated on the server (by date, not page numbers) and the frontend renders only the visible rows.
- **No totals on page load.** Net worth, history, and ranks read daily snapshots; monthly spending and category totals are precomputed when transactions change, never summed across all history per request.
- **Memory:** the JVM's heap is capped in the container image so the app stays inside the 1 GB budget. A GraalVM native image is a later option, not a v1 plan, because its closed-world rules conflict with turning providers on and off by configuration.

### Accessibility and formatting (decided Nov 20, 2026)

- Every page works by keyboard alone, with a visible focus ring. Text and controls meet WCAG AA contrast in light and dark mode.
- Every chart has a data table behind it that screen readers can reach, and color is never the only signal (gains and debt also carry a sign or label).
- Money, dates, and percentages are formatted with the browser's `Intl` APIs for the user's locale, never by hand.
- The UI is English-only in v1, but all text lives in one place per feature, so a French translation (for Quebec) can be added without hunting through components.

### AI providers (decided Nov 19, 2026)

- The AI sits behind the `AiClient` interface, built on Spring AI, which gives one chat API across Anthropic, OpenAI, Ollama, and others. A self-hosted install picks Anthropic, OpenAI, or any OpenAI-compatible endpoint (base URL, model, optional key), which covers OpenRouter, Groq, LM Studio, vLLM, and Ollama. See [ADR 0008](adr/0008-swappable-ai-providers.md).
- The hosted version uses Claude (Haiku by default, Sonnet for hard questions) with prompt caching. If Spring AI's Anthropic support lacks a Claude feature the hosted version needs, the Claude path can use the official Anthropic SDK behind the same interface.
- **The app works fully with no AI configured.** The Ask panel is hidden and suggestion cards use plain templated wording. Ranks, plan math, and every number are computed in code and never depend on the AI.
- With a local model (Ollama), financial data never leaves the server. The privacy screen names whichever provider is configured.
- The context is built on the server and sent in the prompt, so v1 doesn't require a model with tool calling. A small local model may word answers less well, but it can't change the numbers, since those come from code.

### Categories and transfers (decided Nov 20, 2026)

Spending, bill types, recurring-bill detection, and Gold's target all depend on categories, and only Plaid supplies them. SimpleFIN and imported files don't. So every transaction gets a category from the same pipeline, whatever its source.

- **Where a category comes from, in order:** a category the user set, then the user's own rules, then the starter rules, then the provider's category (Plaid's, mapped to the app's list), then an AI suggestion. Each transaction stores which of these set it.
- **Starter rules** ship with the app as data (like rank thresholds): merchant patterns for common categories (rent and mortgage servicers, auto lenders, utilities, groceries, gas, payroll deposits, and so on). They can be updated without a release.
- **AI suggestions:** for transactions nothing else categorized, the AI suggests a category and the user confirms or changes it in a short review list. Nothing counts as spending in a new category until it's confirmed. With no AI configured, those transactions wait in the same list as "Uncategorized." (Confirmed by the user, not applied silently, because categories decide Gold's target and the bill cards.)
- **Changing a category** offers "Always do this for [merchant]," which saves a user rule.
- **Transfers between your own accounts** are matched: same amount with opposite signs, in two of your accounts, within 5 days. A match is shown for the user to confirm, then both sides are marked as a transfer and left out of spending and income. Credit card payments and money moved into investment accounts are matched the same way. This works across sources (a CSV from one bank and Plaid on another). Matching rules are test-first, next to duplicate detection.
- **Pending transactions:** Plaid and SimpleFIN return pending transactions, and when one posts it often gets a new ID and sometimes a new amount (a restaurant tip). Each transaction stores a pending flag; when the posted version arrives it replaces the pending one instead of duplicating it, and anything still pending after 14 days is dropped. Pending transactions show on Account detail but never count in spending, income, or rank math. (Decided Nov 20, 2026.)
- **Refunds** reduce spending in the category of the purchase; they never count as income. A refund that can't be matched to a purchase takes the category its rules give it, still as negative spending.
- **Splits:** a transaction can be split into lines with their own categories and amounts that add up to the original (one store receipt into groceries and household). The table supports it in v1; the split screen can come in 1.0.
- **Time zone:** each user has a time zone (set from the browser at first sign-in, changeable in Settings). "Today," the daily snapshot, "due in 2 days," and the 14-day rank buffer are all computed in that time zone, never in the server's.
- **Region:** v1 supports the US and Canada only, matching SimpleFIN's coverage. Each install has a home currency (USD or CAD) used for net worth and rank thresholds, and each account keeps its own currency. Canadians often hold a USD account, so an account in the other currency is converted to the home currency at that day's exchange rate, and its row shows both amounts and the rate. **A missing rate is never treated as 1:1:** the app uses the last known rate and says how old it is. Daily USD/CAD rates come from a free official source (the Bank of Canada publishes them) and are stored in an `exchange_rates` table, so history converts at each day's rate. (Decided Nov 20, 2026.)

### Data model

| Table | Holds | Notes |
| --- | --- | --- |
| users | the one user, with time zone and home currency | every other table carries user_id, so multi-user later isn't a rewrite |
| connections | provider (plaid or simplefin), institution, encrypted access token or access URL, status, last successful update | one row per bank login; credentials never leave the server; status and update time drive the reconnect badge and "Updated..." labels |
| accounts | name, currency (USD or CAD), kind (cash, investment, retirement, HSA, crypto, debt), source (provider connection, file import, or manual), closed date, hidden flag, update reminder, APR and minimum payment for debts, emergency-fund flag, institution and last 4 digits for duplicate checks | debts stored as positive balances, subtracted in net worth; APR from Plaid Liabilities or entered once; stale and closed are derived (closed when the closed date is set), not stored |
| categories | name, parent, counts as spending or not | content, not code: the starter list ships as data |
| category_rules | match pattern (merchant or description), category, owner (starter or user), priority | starter rules ship as data; user rules come from "Always do this for..." |
| api_tokens | name, hashed token, created and last-used dates | read-only API access; the token is shown once and only its hash is stored |
| exchange_rates | date, from currency, to currency, rate, source | daily USD/CAD; history converts at each day's rate |
| import_mappings | account, file type, column mapping, date format, sign convention | saved per account so repeat imports need no questions |
| balance_snapshots | account, date, balance | one row per account per day; net worth history and the 14-day rank buffer read from these |
| transactions | account, date, merchant, amount, category, category source (user, user rule, starter rule, provider, AI suggestion), confirmed flag, transfer pair (the matching transaction, if any), pending flag, parent transaction (for split lines), recurring flag | also stores the source's own ID (provider transaction ID or OFX transaction ID) for duplicate checks; powers Account detail now and the Money screen in v2 |
| rank_definitions | rank, order, requirement rules, thresholds | content, not code: tuned without a release |
| rank_history | rank, earned or dropped, date | drives Your rank history and rank notifications |
| plan_steps | kind (rank step or personal goal), rank it unlocks, linked account or target, order, and for life events: event date, one-time cost, expected monthly cost change; for covered-loan steps: chosen route | rank steps are generated and locked; personal goals are user-ordered between them; progress is computed, never stored |
| plan_notes | free text, with a version history | the AI reads the latest version |
| recurring_bills | name, amount, schedule, next due date, paying account, set-aside on or off, type (housing, vehicle, other) | v2 Money screen |
| notifications | type, message, read state, action, created date | the inbox; email sends from the same rows; kept 90 days |
| chat_messages | role, content, page, timestamp | recent turns go into each request |
| peer_benchmarks | group, metric, value, percentile or share, source survey, survey year, sample size | v2 How you compare; loaded from the versioned file the offline pipeline (`tools/peer-data/`) writes, never computed in the app |

### AI

- On each question, the server builds the context: the current rank and what the next rank needs, the plan (steps in order plus notes), current balances, net worth with 30- and 90-day change, each step's progress and projected finish date at the recent savings rate, the page the question was asked from, and recent chat turns.
- **All numbers are computed in code, never by the model.** The model explains and recommends from those numbers. That keeps "when do I hit $10k" consistent and checkable.
- The system prompt tells the model to answer within the plan and to flag when a question conflicts with it. When a goal isn't reachable, it presents the code-computed options (timeline, goal size, spending, and income only where it applies) and lets the user choose; it never defaults to "make more money."
- The AI can **suggest** a plan change (for example, raise the emergency fund target after moving out). The suggestion shows as a card the user accepts or dismisses. The model never edits the plan directly.

### Security and hosting

- **Passkey-first sign-in** with a secure session cookie: no passwords and no one-time codes, so there's nothing to phish or leak. The sign-in screens follow the familiar pattern of apps like Wealthfront: email when creating the account, and "Continue with passkey" on the login screen. See [ADR 0010](adr/0010-passkey-first-sign-in.md).
- **First-run setup code.** The first passkey can only be registered after entering the setup code, so a stranger can't claim a freshly deployed instance. The operator can set it in `SETUP_CODE`; otherwise the server generates one at startup and prints it in its log, and the setup screen says where to look ([ADR 0012](adr/0012-generated-setup-code-in-logs.md), decided Nov 23, 2026). Once a passkey exists, setup closes for good: the last passkey can't be deleted, so setup can't reopen (decided Nov 23, 2026); the setup code only unlocks registering that first passkey, and only while setup is open, and you then sign in with it ([ADR 0013](adr/0013-setup-code-unlocks-registration-only.md)); backup passkeys are added from a signed-in session and trigger a sign-in alert. (Decided Nov 22, 2026.)
- Provider tokens encrypted at rest. On the hosted version the key lives in AWS KMS; on a self-hosted install it comes from an environment variable. Never in the repo or the database.
- **Backups protect the Plaid connections.** Losing the database or the key would mean losing the Trial plan's lifetime connections. Postgres runs with automatic backups and point-in-time recovery, the KMS key is protected from deletion (deletion protection plus the maximum waiting period), and one full test restore happens before any real account is connected.
- GitHub's free security features are on from the first commit: secret scanning, push protection, Dependabot alerts and updates, and CodeQL code scanning.
- Desktop web app first, with a responsive layout so it still works in a phone browser. No service worker or browser notifications in v1.

### Gaps closed

- **Assets Plaid can't see:** a fourth Home card, Property & other. A home is added by address and valued automatically by a property-data API (RentCast offers value estimates from an address on self-serve plans; Zillow's Zestimate API is by request only), refreshed monthly, with an optional linked mortgage and a manual override. Cars and other assets are entered by hand. **Both count toward net worth and ranks.** ([RentCast docs](https://developers.rentcast.io/reference/property-valuation), [Zillow API access](https://www.openwebninja.com/blog/zillow-api))
- **Stale and unchanged balances:** rank checks always use each account's last known balance, whether it's a broken connection, a hand-entered account, or a balance that simply hasn't moved. Nothing is paused or excluded.
- **Privacy before linking** (from the competitor research): before the first account is linked, one plain-language screen says what's stored, what's sent to the AI and which AI provider receives it (numbers to answer your questions, never your logins; nothing leaves the server when a local model is configured), that data is never sold, and that you can export or delete everything. It links to the full privacy page.
- **First-time setup:** five steps (link accounts, About you, how you get paid, confirm bills), ending with a starting-rank reveal: the medal, the share of peers at that rank or higher, what's already reached, and the next rank's progress. See the Onboarding mockup.
- **Empty state:** before any account is linked, every value shows $0, the rank shows Unranked, and Home asks you to link accounts ("Link an account to earn Iron"). See the empty-state mockup.
- **AI guardrails:** a "not financial advice" line on answers, a monthly cap on AI cost per user, and a clear note that your numbers are sent to the AI to answer questions.
- **Your data:** export your full history as a spreadsheet, and delete your account, which also disconnects every provider and removes stored tokens.
- **Passkey recovery:** register a backup passkey, plus an email recovery code.
- **Sign in with OIDC (self-hosted):** an optional login through the user's own identity provider (Authentik, Authelia, Keycloak, Google, and so on), next to passkeys, configured by environment variables. It's the top open request in Firefly III and the top request in Wealthfolio. Spring Security's OAuth2 login handles it. (Decided Nov 20, 2026.) Accounts are linked to the provider's subject identifier in a `user_identities` table (user, provider, subject), never by email alone; when OIDC is configured, the account whose verified email equals `OWNER_EMAIL` can also complete first-run setup. (Decided Nov 22, 2026.)
- **Dark mode:** Light, Dark, or Match system in Settings. The purple accent lightens for text on dark backgrounds. See the dark Home mockup.
- **Retirement accounts count at full balance** in net worth and ranks, even though 401(k) money hasn't been taxed yet.

## V2 and later

**V2: "Why it moved" on Home, plus progress notifications (once about 4 weeks of snapshots exist)**

- A small line under Home's net worth replaces "+$1,240 this week" and breaks each weekly change into market movement, money the user moved in or out, and debt paydown. For example: "+$1,240 this week: $800 markets, $500 into the HYSA, -$60 loan interest." Separating contributions from gains needs Plaid transactions and investment transactions.
- Tapping that line shows one line of market context, such as how the S&P 500 moved and which of the user's funds followed it. No single-company headlines unless they are a huge part of the market movement.
- Progress updates (a goal reached, crossing 90% of a target, a payoff date moving earlier or later) arrive as notifications in the in-app inbox (push comes with the phone app). There is no feed list.
- Stale or hand-entered balances are flagged on Home, not in a feed.

**V2: Budget and bills, the Money screen (option C)**

- The headline is what's free for the plan this month: "$1,600 free for step 1, after bills and set-asides." It's computed as money that came in, minus money spent, minus bills still due and this month's set-asides, so the number is never overstated.
- A split bar and four numbers: came in, spent, bills to cover, and free for your plan. Income is what actually arrived, including per diem, so travel months and overtime count without assuming a fixed paycheck.
- A pace line: what the month is on track to send to the plan, and whether that finishes the current step.
- Top spending categories with amounts. They're for information, not limits; there are no per-category budgets.
- The tab is called Money and holds only the budget and recurring bills.

**V2: Recurring bills (part of the Money screen)**

- Recurring charges (auto loan, insurance, phone, subscriptions, and rent after the move-out) are listed with amount, schedule, and Paid or Due status for the month.
- Detection: the app proposes recurring charges from repeating Plaid transactions and flags them "Auto-detected" until the user confirms. He can also add a bill by hand or mark any transaction as recurring.
- Bills still due this month are subtracted before the "free for your plan" number, and shown as their own hatched segment on the split bar.
- Paid bills count in Spent. Other spending is shown separately by category.
- The AI knows the bills too, so "can I afford X" answers account for what's still due.
- The plan's emergency-fund sizing can use the monthly bill total as the fixed-cost baseline.

* **Any schedule:** weekly, every 2 weeks, monthly, every 3 months, every 6 months, yearly, or custom, each with a next due date and the account it's paid from.
* **Set-asides for bills that aren't monthly (on by default):** a $1,260 insurance bill every 6 months reserves $210 each month. That amount counts against the "free for your plan" number every month, so the due month isn't a surprise. The Money screen shows it as "Setting aside $210/mo" with the next due date, and the budget line reads "bills to cover" (monthly bills due plus this month's set-asides). Turning set-aside off counts the full amount only in the month it's due.
* The recurring-bills total is shown as a monthly equivalent (here $752 a month).

**Notifications (sign-in alerts in v1, the rest in v2)**

- **No browser alerts.** The web app sends no pop-up notifications. Push notifications arrive with the phone app later.
- **Email for what matters.** Every alert lands in the in-app inbox. Anything that needs action (a sign-in expired) is also emailed, plus an optional weekly summary on Sundays.
- **Triggers come from the backend:** a Plaid webhook leads to recalculated goals (a step finished, 90% reached, payoff date moved); Plaid reports a broken sign-in; the daily job finds a bill due in 2 days or a hand-entered balance past its update reminder.
- **Rules:** a toggle per type in Settings & accounts; quiet hours (default 10 PM to 7 AM), with held alerts delivered after; a cap of 2 to 3 a day; amounts left out of emails by default.
- **Built for the mobile app later:** one notification service decides what to send, and each delivery channel (the in-app inbox and email now, the phone push services later) sits behind it, so adding the mobile app means adding a channel, not rewriting the logic.

* **In-app inbox.** A bell with an unread count sits top-right on every page and opens a Notifications page. It's the main place alerts live in the web app. The page groups alerts by Today, This week, and Earlier, filters by All, Unread, or Action needed, and lets the user act right there (Reconnect, Confirm a bill). Inside the app, behind the login, amounts are shown in full. Notifications are kept for 90 days. A notifications table backs it, and email (and later phone push) sends from the same records.

**V2: Investment holdings (decided Nov 20, 2026)**

- Holdings show only for accounts linked through Plaid, using Plaid Investments: each position's name, ticker, quantity, the brokerage's price and value, and cost basis (with tax lots where the brokerage provides them). No market-data service is used, so nothing depends on free price feeds that break.
- SimpleFIN, imported, and manual investment accounts stay balance-only. SimpleFIN's account data has no holdings.
- What it adds: an allocation view (stocks, bonds, cash) on Account detail and Home, dividends received, and a sharper "why it moved" that separates market gains from contributions.
- Data model: `securities` (name, ticker, type) and a daily `holding_snapshots` (account, security, quantity, price, value, cost basis). Ranks and the plan keep using balances only.
- Plaid Investments is a separate product with its own pricing; check the cost before building.

**V2: How you compare**

The How you compare tab on the Ranks page, plus the per-rank "X% have reached this" lines on the ladder. It shows how rare the user's position is in the U.S., a shareable feature neither Monarch nor Origin has.

- Profile: age, sex, marital status, net worth (from the app), and monthly income (entered or inferred from deposits).
- Headline: "About 1 in N unmarried men under 35 have both your income and your net worth or higher."
- **Where each number comes from** (decided Nov 21, 2026). A figure that combines traits must come from one survey that records all of them, because combining separate surveys ignores how the traits move together (income and net worth are correlated).
  - **Net worth percentile, and the joint "1 in N" headline:** SIPP (the Census household-finance survey), which records both income and wealth for the same people. Compared against unmarried men heading their own household in the user's age group, since wealth is measured per household and a single person's household wealth is all theirs.
  - **Income-only percentile:** the Census Current Population Survey (its Annual Social and Economic Supplement), which records income per person and has the largest sample.
  - **Per-rank shares ("X% have reached Gold"):** the Fed's Survey of Consumer Finances (SCF), the only one of the three with liquid savings, debt with interest rates, and spending in the same record. Ranks stack, so a share must test every rank's requirement on the same household at once. When the user's group has too few survey records for a reliable share, the app uses the next broader group (for example, all adults under 35) and names the group it used.
- A line tying it to the plan: the projected percentile once the next goal is done.
- **Never multiply separate percentages,** and never combine two surveys into one figure. Use the public microdata (IPUMS makes Census files easier) with the survey's weights.
- Open question: how SIPP counts adults who live with their parents. Check its documentation before relying on it.

**How the comparison data stays current**

- **An offline pipeline in the repo** (`tools/peer-data/`) downloads each survey's public microdata, applies the survey weights, and writes small lookup tables: percentiles by group, and per-rank shares by group. It never runs inside the app, and the raw survey files are never committed, only the script and its output.
- **The output ships as data, like rank thresholds:** a versioned file loaded into a `peer_benchmarks` table (group, metric, value, percentile or share, source survey, survey year, sample size). Self-hosters get new numbers with the next release; the hosted version gets them on deploy. No install ever downloads survey data itself.
- **Updating is a reviewed change, not automatic.** Surveys rename variables and change definitions between editions, so a human runs the pipeline, checks that the numbers moved sensibly against the previous edition, and merges it like any other change.
- **A scheduled GitHub Action** checks each source's download page once a month and opens an issue when a new edition appears, so nothing gets missed. Expected cadence: CPS yearly, SIPP yearly, SCF every three years (the 2025 SCF came out October 2026; the next is expected in late 2029).
- **The app always shows its source,** for example "Based on the 2025 Survey of Consumer Finances." When an update shifts someone's percentile or share, the app says the comparison data was updated, so a change doesn't look like the user's money moved.
- U.S. only at first; other countries need their own data sources.

**Later (decided Nov 20, 2026)**

- **Several users per install:** separate logins on one server for a household, each with private accounts, ranks, and plans (no shared accounts). Every table already carries user_id, so this adds signup and an admin role, not a rewrite.

**Later, only if it becomes a business**

- Signup, a paid Plaid plan, multi-user, billing, and a privacy policy and terms. Planned pricing: one plan with a free trial (14 days), then $9.99 a month or $99.99 a year. Everything is included; the AI cost cap above is a fair-use safeguard, not a tier limit. That's in line with Monarch ($99.99 a year), Copilot ($95 a year), and Origin ($99 a year, with its AI Advisor included in the membership). Possible later tiers: $10 with limited AI and $25 with unlimited AI plus extras.
- **Fair billing** (from the competitor research; decided Nov 18, 2026): no teaser prices that jump after the first year; any price increase is announced at least 60 days ahead and never applies to a renewal already charged; a reminder email goes out 7 days before an annual renewal.
- **Cancel in Settings, with one confirmation.** Settings has a Cancel subscription button that opens a single "Are you sure?" screen showing what ends and when access stops, then confirms. No retention maze, no required call or chat, and no extra offers in the way. Export stays available until access ends.
- A native phone app (bottom tabs, push notifications) once the web app proves itself; the phone mockups are its reference.

## Decision log and open questions

**Product decisions**

- A personal tool first, with a business possible later.
- v1 is the first demoable product; 1.0 is the first public, self-hostable release (see Releases).
- Plaid for data in. Updated Nov 19, 2026: Plaid and SimpleFIN both in 1.0, plus CSV, OFX, and QIF import, behind a swappable provider interface.
- Easy to self-host, with the AI provider swappable (Anthropic, OpenAI, or any OpenAI-compatible endpoint, including local models). The app works without AI.
- Desktop web app first; a native phone app later.
- Net worth front and center; the AI knows the user's goals and keeps them focused.
- News means "why your net worth moved", not headlines.
- V1 = approach A (net worth first), with ranks as the main feature: Iron to Legend, real demotion with a 14-day buffer, and Legend meaning financial independence.
- Rank steps are built per account, locked in ladder order, and can't be moved or removed; personal goals go between them.
- Bills on any schedule, with monthly set-asides for bills that aren't monthly.
- No "Sync now" button and no browser alerts; the in-app inbox plus email.
- Black, white, and purple theme; net worth always first on Home, with the Road to Legend band below the accounts.
- Pricing (if it becomes a business): free trial, then $9.99 a month or $99.99 a year.

**Technical and detailed decisions (Nov 18, 2026)**

- Stack: a Java and Spring Boot backend, with a React/Vite/TanStack Query/Tailwind frontend in TypeScript, PostgreSQL, deployed on AWS. Money is stored as BigDecimal and the daily sync runs as a Spring @Scheduled job. The backend publishes an OpenAPI spec and the frontend's TypeScript types are generated from it, so types aren't kept in sync by hand. It uses the official Plaid Java SDK and Spring AI for the AI.
- The hosted version's AI is the Claude API; self-hosters can configure another provider.
- Single-user passkey login. Provider tokens encrypted, with the key in AWS KMS (hosted) or an environment variable (self-hosted).
- Manual entry for any account: cash, investment, or debt, whether or not Plaid can connect it.
- Open for later: the $100k, $250k, and $500k thresholds lose value with inflation. They're stored as data, so they can be raised; before raising them, decide whether people already holding a rank keep it.
- user_id on every table from day one.
- The AI suggests plan changes and never edits the plan itself.
- "Recent savings rate" for projections = the 90-day change in cash accounts plus debt paid down, plus investment contributions (from Plaid investment transactions, or the monthly contribution entered for other investment accounts; money moved between investment accounts isn't counted twice), with an optional monthly figure the user can set on the Plan page to override it.
- Build starts in January 2027.
- Ranks and generated plan steps are in v1, because they're the main feature. The per-rank peer percentages and the How you compare data come in v2, since they need offline survey processing.
- Plaid Transactions and Liabilities are in v1: Account detail needs transactions, and debt steps need APRs.
- Gold's target is 3 months of your actual spending (3 × your median monthly spending over the last 6 months, bills included; see How the numbers are read), with no dollar floor, so a frugal saver isn't held back. Gold can't unlock until the app has 60 days of spending history, which the transaction pull at signup usually provides on day one. A personal goal can push the target toward 6 months. (Decided Nov 18, 2026; replaces the earlier $10,000 floor.)
- Platinum's "15% of income" uses income that actually arrived (paychecks plus per diem) over the last 90 days. It counts money invested anywhere, not just retirement accounts: 401(k) and IRA contributions, deposits into a taxable brokerage, and HSA investments (decided Nov 18, 2026). Contributions come from Plaid investment transactions; for a hand-entered investment account, the user enters a monthly contribution. Money that moves between investment accounts isn't counted twice.
- Free trial length: 14 days.

**Open, decided during the build**

- Which default model, prompt wording, and how many chat turns to include.
- Charting library, visual style, and the time the daily sync runs.
- AWS service choices for the API, the scheduled job, and Postgres.
