# Spring Boot migration and dependency triage

The migration is stacked on the session hardening branch so the configuration fixes can be reviewed on their own. The target is Spring Boot 4.0.10, a supported maintenance release available on Maven Central. The application keeps Java 17 and its Gradle 8.14.3 wrapper. The change adopts the Web MVC and security test starters and Jackson 3. The compatibility defaults preserve the application's existing JSON conventions. [Spring Boot 4.0.10 release](https://spring.io/blog/2026/11/20/spring-boot-4-0-10-available-now/) · [Migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)

## Advisory applicability

The November 27, 2026 review resolved the actual Gradle runtime classpath and queried OSV with Maven names and versions. The baseline returned **70 distinct advisories across 16 of 75 artifacts**. The migration returned **zero matches across 91 resolved artifacts**. Advisory matching indicates exposure at the component version level. It does not establish exploitation or account for infrastructure outside the repository. [OSV query method](https://google.github.io/osv.dev/post-v1-querybatch/) · [Advisory-by-advisory matrix](osv-baseline-triage.csv)

| Assessment | Evidence and disposition |
| --- | --- |
| Application surface present | The embedded Tomcat HTTP parser, Spring Security response-header writer and Spring MVC static resource handler are exercised by this application. Prioritize their advisories even when production exploitability depends on traffic and topology. |
| Deployment or input dependent | Verify the real reverse proxy, multipart parsing, SMTP TLS, temporary directories, HTTP/2 and WebSocket settings. JSON is exposed through DTOs, but parser and coercion prerequisites differ per Jackson advisory. |
| Prerequisite absent from the reviewed defaults | The implementation uses Spring authorization and custom session sign-in. It does not configure container DIGEST or FORM authentication, AJP, WebDAV, Tomcat cluster membership, the affected Spring Security alternate login modes, a Logback socket receiver or user-supplied SpEL expressions. DevTools was absent from the packaged baseline JAR. |

Representative fixed versions are Tomcat 11.0.20 for the HTTP parser advisory, Spring Security 7.0.4 for servlet response headers and Spring Framework 7.0.8 for the static resource advisory. The migration resolves Tomcat 11.0.27, Spring Security 7.0.9 and Spring Framework 7.0.11. The affected Jackson 2 core and databind artifacts are replaced by Jackson 3. Logback Core resolves to 1.5.38, above the 1.5.34 fix listed for the latest matched baseline Logback entry. [Tomcat advisory](https://osv.dev/vulnerability/GHSA-563x-q5rq-57qp) · [Spring Security advisory](https://osv.dev/vulnerability/GHSA-mf92-479x-3373) · [Spring MVC advisory](https://osv.dev/vulnerability/GHSA-mq64-j8f9-9gcj)

## Implementation and verification

- Use the Boot 4 Web MVC starter and the corresponding test starters. Use the new Boot test annotation package and the Jackson 3 mapper in email settings and JSON assertions.
- Keep the API paths, authorization, CSRF configuration, seed profiles and frontend lockfile. Verify these with the existing tests and builds before merging.
- The local Gradle run built the application JAR and passed all 190 backend tests. Run the GitHub Actions jobs on the stacked review branch to validate the Ubuntu backend and Vue build.

## Deployment and residual risks

Review the migrated dependencies and the supported release schedule before every deployment. Test on a copy of the target database with a verified restore and exercise Hibernate schema validation. Rehearse sign-in, order operations, password reset and membership mail behind the actual HTTPS proxy. Retain the previous known-good package and configuration during the test rollout, and restore data from the same compatible snapshot if rollback requires it.

The version change leaves account-wide session revocation, reset and mail retention, abuse monitoring, and production TLS verification as separate tasks. The OSV result is a dated dependency snapshot, and these application and operational risks remain relevant.
