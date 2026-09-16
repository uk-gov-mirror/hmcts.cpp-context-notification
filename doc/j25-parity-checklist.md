# notification — J17 → J25 Behavioural Parity Checklist

Companion to *Parity Testing Java17 → Java25* (Confluence CTP/1990371020) and its 24-BC catalogue.
Executive summary + evidence: `/J25-PARITY-FINDINGS.md`. Jira: PEG-3534 (under PEG-3377).
Scanned `main` (spp 17.104.0, Hibernate 5.4.24) vs `team/25.104.x` (2c1dbc0c, spp M10). Full build + unit
suite green on J25 (H2 2.3.232).

## Applies to notification

| BC | Trigger | Disposition | Status |
|----|---------|-------------|--------|
| **BC-07** | `liquibase.hub.mode` in `notification-viewstore-liquibase/…/liquibase.properties` — Liquibase 5 rejects it → deploy-time migration failure. | Fix (sweep) | ✅ removed |
| **BC-20** | 2 Drools kbases (COMMAND_API, QUERY_API) could silently load 0 rules → vacuous deny-tests. | Fix (guard) | ✅ `AccessControlRuleCountTest` in both modules (`>0` rules), green |

## Low-risk / cleared by the scan

| BC | Finding | Action |
|----|---------|--------|
| BC-01 / BC-02 | All finders return `List`; `SubscriptionRepository` DeltaSpike `CriteriaSupport` → plain JPQL preserving `List`; `findBy(id)`→`find()` null↔null. | none |
| BC-04 | No primitive fields on entities. | none |
| BC-05 | No JPQL `!= null` guards. | none |
| BC-06 | No collection associations / lazy loading. | none |
| BC-11 | `add()` values are `userId` (orElseThrow — never null), `subscriptionId` (getString — throws on absent) or constants; null never reaches `add`. | none |
| BC-24 / H2 2.x | `EventCacheJdbcRepository` native SQL (`cast(created as timestamp)`, `LIMIT`, dynamic filter) unchanged J17→J25; unit suite green on H2 2.3.232. | verify ITs exercise this SQL on the WF40/H2-2.x + Postgres stack (unit tests only cover config/wiring) |

## Not applicable

- BC-09 / BC-10 (Activiti), BC-22 (Tika), BC-19 (SJP) — features not used.
- BC-03 / BC-14 — refuted in the catalogue.

## Golden-master baseline

0 of 36 test-resource JSON files changed since J17 (0 expected/output goldens); 25 exact `equalTo`/`isJson`
assertions, all green on J25 → output parity preserved by construction.

## Coverage tracker

- [x] BC-07 `liquibase.hub.mode` swept
- [x] BC-20 rule-count guards added (COMMAND_API, QUERY_API), green
- [ ] Confirm ITs exercise `EventCacheJdbcRepository` native SQL on H2-2.x / Postgres
- [x] BC-01/02/04/05/06/11 verified low/N-A
