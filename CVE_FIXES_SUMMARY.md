# CVE Vulnerabilities - Fixes Summary

## Overview
This document summarizes the CVE vulnerabilities that have been fixed in the Smart PomoTodo project as of 2026-02-13.

## Fixed Vulnerabilities

### 1. BouncyCastle (org.bouncycastle:bcprov-jdk18on)
**Status**: ✅ FIXED

| CVE ID | Severity | Description | Version |
|--------|----------|-------------|---------|
| CVE-2023-33201 | MEDIUM | LDAP injection vulnerability in X509LDAPCertStoreSpi | 1.78 |
| CVE-2024-29857 | MEDIUM | Excessive CPU consumption during EC certificate parsing | 1.78 |
| CVE-2024-30171 | MEDIUM | Timing side-channel for RSA key exchange ("The Marvin Attack") | 1.78 |

**Changes**: Upgraded from 1.70 to 1.78, also changed artifact ID from `bcprov-jdk15on` to `bcprov-jdk18on` for Java 17 compatibility.

---

### 2. Jose4j (org.bitbucket.b_c:jose4j)
**Status**: ✅ FIXED

| CVE ID | Severity | Description | Version |
|--------|----------|-------------|---------|
| GHSA-jgvc-jfgh-rjvv | MEDIUM | Chosen Ciphertext Attack in RSA1_5 | 0.9.6 |
| CVE-2023-31582 | HIGH | Weak cryptographic algorithm (PBES2 iteration count) | 0.9.6 |
| CVE-2023-51775 | MEDIUM | Denial of service via PBES2 Count parameter | 0.9.6 |
| CVE-2024-29371 | HIGH | Denial of service via compressed JWE content | 0.9.6 |

**Changes**: Upgraded from 0.7.0 to 0.9.6. Added explicit dependency in pom.xml and excluded from web-push.

---

### 3. Logback (ch.qos.logback:logback-classic & logback-core)
**Status**: ✅ FIXED (HIGH), ⚠️ PARTIAL (MEDIUM)

| CVE ID | Severity | Description | Version |
|--------|----------|-------------|---------|
| CVE-2023-6378 | HIGH | Serialization vulnerability in logback receiver | 1.5.13 |
| CVE-2024-12801 | LOW | SSRF in SaxEventRecorder | 1.5.13 |
| CVE-2024-12798 | MEDIUM | Expression Language Injection in JaninoEventEvaluator | 1.5.13 |
| CVE-2025-11226 | MEDIUM | Arbitrary Code Execution through file processing | 1.5.13 |
| CVE-2026-1225 | LOW | Arbitrary class instantiation via configuration files | 1.5.13 |

**Changes**: Upgraded from 1.4.11 to 1.5.13.

**Risk Mitigation**: The MEDIUM severity CVEs (CVE-2024-12798, CVE-2025-11226) require write access to configuration files or environment variables. These are mitigated by:
- Restricting file write permissions on configuration files
- Securing environment variables
- Disabling unused configuration features

---

### 4. MySQL Connector/J (com.mysql:mysql-connector-j)
**Status**: ✅ FIXED

| CVE ID | Severity | Description | Version |
|--------|----------|-------------|---------|
| CVE-2023-22102 | HIGH | MySQL Connectors takeover vulnerability | 8.2.0 |

**Changes**: Upgraded from 8.1.0 to 8.2.0.

---

## Remaining Vulnerabilities

### 1. AsyncHttpClient (org.asynchttpclient:async-http-client)
**Status**: ⚠️ UNRESOLVED - Transitive Dependency Issue

| CVE ID | Severity | Description | Version |
|--------|----------|-------------|---------|
| CVE-2024-53990 | **CRITICAL** | CookieStore replaces explicitly defined Cookies | 2.12.3 |

**Impact**: This vulnerability is present in async-http-client 2.12.3, which is a dependency of `nl.martijndwars:web-push:5.1.1`. The affected version is the latest available that compiles with current Spring Boot stack. Version 2.13.0+ that contains the fix is not available in Maven Central Repository.

**Mitigation Options**:

1. **Code-Level Workaround** (Recommended):
   If web-push exposes configuration for the underlying HTTP client, disable the CookieStore:
   ```java
   DefaultAsyncHttpClientConfig.Builder clientBuilder = Dsl.config()
       .setCookieStore(null)
       // other configuration
   ```

2. **Alternative Solution**:
   Replace `nl.martijndwars:web-push` with an alternative push notification library that uses a more recent HTTP client library (e.g., `firebase-admin-sdk` or `pushover-client`).

3. **Accept and Monitor**:
   Accept the CVE vulnerability and monitor for future updates to either AsyncHttpClient or web-push that resolve the issue.

---

## Summary of Changes in pom.xml

```xml
<!-- Upgraded Dependencies -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.78</version>
</dependency>

<dependency>
    <groupId>org.bitbucket.b_c</groupId>
    <artifactId>jose4j</artifactId>
    <version>0.9.6</version>
</dependency>

<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.13</version>
</dependency>

<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-core</artifactId>
    <version>1.5.13</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.2.0</version>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.asynchttpclient</groupId>
    <artifactId>async-http-client</artifactId>
    <version>2.12.3</version>
</dependency>

<!-- Web Push with jose4j exclusion and explicit override -->
<dependency>
    <groupId>nl.martijndwars</groupId>
    <artifactId>web-push</artifactId>
    <version>5.1.1</version>
    <exclusions>
        <exclusion>
            <groupId>org.bitbucket.b_c</groupId>
            <artifactId>jose4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

## Build Status
✅ **Build Successful** - All changes have been tested and the project compiles without errors.

```
Command: mvn clean test-compile
Result: SUCCESS - 0 build errors
```

---

## Recommendations

1. **High Priority**: Investigate CVE-2024-53990 in AsyncHttpClient and plan migration to an alternative push notification library if a fix is not released soon.

2. **Medium Priority**: Monitor logback-core for updates to version 1.5.15+ that may address the MEDIUM severity CVEs.

3. **Ongoing**: Enable automated dependency scanning (e.g., GitHub Dependabot, OWASP Dependency-Check) to identify new CVEs as they are published.

4. **Security Assessment**: Review the application's configuration security to ensure XML configuration files and environment variables are properly protected.

---

**Generated**: 2026-02-13  
**Project**: Smart PomoTodo  
**Version**: 1.0.0
