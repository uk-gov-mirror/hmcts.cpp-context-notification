# notification — Java 17 → Java 25 Behavioural Parity Findings

**Context:** cpp-context-notification (Platform Engineering-owned), upgraded to Java 25 / WildFly 40 /
Jakarta EE 11 on `team/25.104.x` (merged, PR #35; service-parent-pom 25.104.0-M10). Baseline: `main`
(service-parent-pom 17.104.0, Hibernate 5.4.24). Jira: PEG-3534 (under PEG-3377).

Method: the CTP guide *Parity Testing Java17 → Java25* (Confluence 1990371020) and its 24-BC catalogue —
**J17 is the source of truth; a J25 difference is a finding, not a loosened assertion.** Everything below
was run/verified, not assumed. Investigation date: 2026-08-26.

---

## Headline

notification is in **good parity shape** — **no functional-equivalence blocker**. The generic hotspots
(finder rewrites, JSON serialization, lazy loading) are parity-neutral / not-reached here, and the golden
baseline is intact: **0 of 36 test-resource JSON files changed** since J17, and the full build + unit suite
is **green on J25 (H2 2.3.232)**. The only actionable items are a config sweep (BC-07, done) and a
test-harness guard (BC-20, done).

The distinctive angle for notification (vs resulting) is the **H2 1.4.196 → 2.3.232 major bump** and a
**native-JDBC** repository — see §3.

---

## 1. Persistence — clear

11-repo situation is simpler here: 3 repositories.
- **BC-01 / BC-02 (single-result finders):** N/A. `EventCacheRepository` finders all return `List`
  (`findByUserIdOrderByCreatedDesc`, …); `SubscriptionRepository` was a DeltaSpike `CriteriaSupport` repo,
  **rewritten to plain JPQL** (`SELECT s FROM Subscription s WHERE s.modified < :expiredTime`) preserving
  `List` semantics; `findBy(id)` → `entityManager.find(...)` is null ↔ null on both runtimes. No throwing
  single-result finder.
- **BC-04 (NULL → primitive):** N/A — no primitive fields on entities (checked all `@Entity` classes).
- **BC-05 (JPQL `!= null` → silently empty):** none present.
- **BC-06 (LazyInitializationException):** N/A — no `@OneToMany`/`@ManyToMany` collections, no lazy
  associations; 0 `@Transactional` needed in query-view.

Persistence unit tests green on **H2 2.3.232**: `EventCacheRepositoryTest` 6/6, `SubscriptionRepositoryTest` 3/3.

## 2. Serialization / access control

- **BC-11 (`JsonObjectBuilder.add(key, null)` → Parsson NPE):** **not reached.** `NotificationCommandApi`'s
  `add()` sites pass `userId` (from `metadata().userId().orElseThrow(...)` — never null) and `subscriptionId`
  (from `getString(name)` — throws on absent, never null), or constants. No site feeds a Java `null`.
- **BC-20 (Drools 0-rule vacuous deny):** 2 kbases (COMMAND_API, QUERY_API). **Fixed** — see §4.

## 3. notification-specific: H2 2.x + native JDBC

- **H2 1.4.196 → 2.3.232** is a major bump (H2 2.x is much stricter: reserved words, cast/type and `LIMIT`
  semantics). The persistence unit suite is **green on 2.3.232**, discharging this risk for the unit layer.
- **`EventCacheJdbcRepository`** is native JDBC (`PreparedStatementWrapper`), with SQL like
  `DELETE FROM event_cache WHERE id IN (SELECT id FROM event_cache WHERE cast(created as timestamp) < cast(? as timestamp) LIMIT ?)`
  and a dynamic `queryByFilter(filterClause, …)` (`String.format` into the WHERE). This SQL is **unchanged
  J17 → J25** (imports only), so its behaviour rides entirely on the runtime.
  - **Coverage observation:** the unit tests present (`EventCacheJdbcRepositoryConfigTest`,
    `...ProducerTest`) exercise config/wiring, not the actual SQL against a DB — so the native `cast`/`LIMIT`
    SQL on H2 2.x (and pgjdbc, BC-24) is only validated by the integration tests. Confirm the ITs cover the
    expiry-delete and filter paths on the WF40/H2-2.x stack.

## 4. Actionable items

| Item | Detail | Status |
|---|---|---|
| **BC-07** | `notification-viewstore-liquibase/…/liquibase.properties` had `liquibase.hub.mode: off`; Liquibase 5 rejects the removed Hub property → **deploy-time migration failure**. | ✅ **DONE** — key removed (kept `changelogFile` + `liquibase.headless`). |
| **BC-20** | 2 Drools kbases (COMMAND_API, QUERY_API) could silently load 0 rules → deny-tests pass vacuously. | ✅ **DONE** — `AccessControlRuleCountTest` in command-api and query-api asserting the kbase compiled `> 0` rules; both green. Platform follow-up: add the same guard to the shared `BaseDroolsAccessControlTest`. |

## 5. Golden-master baseline

**0 of 36** test-resource JSON files changed J17 → J25 (0 expected/output goldens), and the suite carries
25 exact `equalTo(expected)` / `isJson` assertions — all green on J25. Output parity is preserved by
construction (J17-recorded expectations still hold on the J25 runtime).

## 6. Recommendation

No functional-equivalence blocker. Close out with:
1. BC-07 sweep — **done**.
2. BC-20 rule-count guards — **done**.
3. Confirm the integration tests exercise `EventCacheJdbcRepository`'s native `cast`/`LIMIT`/filter SQL on
   the WF40 / H2-2.x (and Postgres) stack — the one path not covered by unit tests.
