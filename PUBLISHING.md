# Publishing to Maven Central

This guide explains how to publish the Ripple Kotlin SDK to Maven Central.

## Prerequisites

### 1. Create Sonatype OSSRH Account

1. **Sign up for Sonatype JIRA:**
   - Go to [Sonatype JIRA](https://issues.sonatype.org/secure/Signup!default.jspa)
   - Create an account with your email

2. **Create a New Project ticket:**
   - Project: `Community Support - Open Source Project Repository Hosting (OSSRH)`
   - Issue Type: `New Project`
   - Summary: `Request for com.tapsioss group ID`
   - Group Id: `com.tapsioss`
   - Project URL: `https://github.com/Tap30/ripple-kotlin`
   - SCM URL: `https://github.com/Tap30/ripple-kotlin.git`
   - Description: `Ripple Kotlin SDK - Event tracking library for Kotlin/Java`

3. **Wait for approval** (usually 1-2 business days)

### 2. Generate GPG Key for Signing

```bash
# Generate GPG key
gpg --gen-key
# Follow prompts: use your email, set a passphrase

# List keys to get the key ID (last 8 characters)
gpg --list-secret-keys --keyid-format LONG

# Export public key and upload to key servers
gpg --armor --export YOUR_KEY_ID > public-key.asc
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID

# Export secret key for GitHub Actions
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc
```

### 3. Set Up Local Credentials

Create `~/.gradle/gradle.properties`:

```properties
# OSSRH credentials (from Sonatype JIRA account)
ossrhUsername=your_jira_username
ossrhPassword=your_jira_password

# GPG signing
signing.keyId=YOUR_KEY_ID
signing.password=your_gpg_passphrase
signing.secretKeyRingFile=/Users/yourusername/.gnupg/secring.gpg
```

### 4. GitHub Repository Secrets

Add these secrets in GitHub: **Settings → Secrets and variables → Actions**

- `OSSRH_USERNAME`: Your Sonatype JIRA username
- `OSSRH_PASSWORD`: Your Sonatype JIRA password  
- `SIGNING_KEY_ID`: Your GPG key ID (last 8 characters)
- `SIGNING_PASSWORD`: Your GPG key passphrase
- `SIGNING_SECRET_KEY_RING_FILE`: Base64 encoded private key

To encode the private key:
```bash
# macOS
base64 -i private-key.asc | pbcopy

# Linux  
base64 private-key.asc | xclip -selection clipboard
```

## Publishing Process

### Automated Publishing (Recommended)

1. **Create and push a version tag:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **GitHub Actions automatically:**
   - Runs all tests
   - Builds all artifacts (core, android, spring, reactive)
   - Signs with GPG
   - Publishes to Maven Central staging
   - Releases to public Maven Central
   - Creates GitHub release

### Manual Publishing

```bash
# Publish to staging
./gradlew publishToSonatype

# Close and release staging repository
./gradlew closeAndReleaseSonatypeStagingRepository
```

## Verification

After publishing, verify at:
- **Maven Central:** https://search.maven.org/search?q=g:com.tapsioss.ripple
- **Sonatype Nexus:** https://s01.oss.sonatype.org/#nexus-search;quick~com.tapsioss.ripple

## Usage for Consumers

Once published, users can add dependencies:

```kotlin
// Core module
implementation("com.tapsioss.ripple:core:1.0.0")

// Android module  
implementation("com.tapsioss.ripple:android:1.0.0")

// Spring Boot module
implementation("com.tapsioss.ripple:spring:1.0.0")

// Reactive module
implementation("com.tapsioss.ripple:reactive:1.0.0")
```

## Troubleshooting

### Common Issues

1. **"Unauthorized" error:**
   - Verify OSSRH credentials are correct
   - Ensure your account has access to `com.tapsioss` group

2. **GPG signing fails:**
   - Check GPG key hasn't expired: `gpg --list-keys`
   - Verify passphrase is correct
   - Ensure key is uploaded to key servers

3. **Staging repository not found:**
   - Wait a few minutes after publishing
   - Check [Sonatype Nexus UI](https://s01.oss.sonatype.org/#stagingRepositories)

4. **Version already exists:**
   - Maven Central doesn't allow overwriting versions
   - Create a new version tag

### Getting Help

- [Maven Central Documentation](https://central.sonatype.org/publish/)
- [Gradle Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin)
- [Sonatype Support](https://issues.sonatype.org)
