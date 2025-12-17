# Publishing Setup Guide

This guide explains how to set up publishing for the Ripple Kotlin SDK to Maven Central.

## Prerequisites

### 1. Create Sonatype Account

1. Go to [Sonatype JIRA](https://issues.sonatype.org/secure/Signup!default.jspa)
2. Create an account
3. Create a new issue to request access to `com.tapsioss` group ID:
   - Project: Community Support - Open Source Project Repository Hosting (OSSRH)
   - Issue Type: New Project
   - Group Id: `com.tapsioss`
   - Project URL: `https://github.com/Tap30/ripple-kotlin`
   - SCM URL: `https://github.com/Tap30/ripple-kotlin.git`
4. Wait for approval (usually takes 1-2 business days)

### 2. Generate GPG Key

```bash
# Generate a new GPG key
gpg --gen-key

# List keys to get the key ID
gpg --list-secret-keys --keyid-format LONG

# Export the public key to upload to key servers
gpg --armor --export YOUR_KEY_ID

# Upload to key servers
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

### 3. Set Up Credentials

#### Option A: Local gradle.properties

Create `~/.gradle/gradle.properties`:

```properties
# Sonatype credentials
sonatypeUsername=your_sonatype_username
sonatypePassword=your_sonatype_password

# GPG signing
signing.keyId=YOUR_KEY_ID
signing.password=your_gpg_passphrase
signing.secretKeyRingFile=/path/to/secring.gpg
```

#### Option B: Environment Variables

```bash
export SONATYPE_USERNAME=your_sonatype_username
export SONATYPE_PASSWORD=your_sonatype_password
export SIGNING_KEY_ID=YOUR_KEY_ID
export SIGNING_PASSWORD=your_gpg_passphrase
export SIGNING_SECRET_KEY_RING_FILE=/path/to/secring.gpg
```

### 4. GitHub Secrets (for CI/CD)

Add these secrets to your GitHub repository settings:

- `SONATYPE_USERNAME`: Your Sonatype username
- `SONATYPE_PASSWORD`: Your Sonatype password
- `SIGNING_KEY_ID`: Your GPG key ID
- `SIGNING_PASSWORD`: Your GPG key passphrase
- `SIGNING_SECRET_KEY_RING_FILE`: Base64 encoded GPG secret key ring

To encode the GPG key ring:
```bash
base64 ~/.gnupg/secring.gpg | pbcopy  # macOS
base64 ~/.gnupg/secring.gpg | xclip   # Linux
```

## Publishing Process

### Manual Publishing

1. **Create and push a git tag:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **Publish to staging:**
   ```bash
   ./gradlew publishToSonatype
   ```

3. **Close and release:**
   ```bash
   ./gradlew closeAndReleaseSonatypeStagingRepository
   ```

### Automated Publishing (Recommended)

1. **Push a tag:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **GitHub Actions will automatically:**
   - Run tests
   - Build artifacts
   - Publish to Maven Central
   - Create GitHub release

## Version Management

Versions are automatically determined from git tags:

- Tagged releases: `v1.0.0` → `1.0.0`
- Development builds: `commit-hash-SNAPSHOT`

## Verification

After publishing, verify your artifacts at:
- [Maven Central Search](https://search.maven.org/search?q=g:com.tapsioss.ripple)
- [Sonatype Repository](https://s01.oss.sonatype.org/#nexus-search;quick~com.tapsioss.ripple)

## Troubleshooting

### Common Issues

1. **GPG signing fails:**
   - Ensure GPG key is properly configured
   - Check that the key hasn't expired
   - Verify the passphrase is correct

2. **Sonatype authentication fails:**
   - Double-check username/password
   - Ensure your account has access to the group ID

3. **Staging repository not found:**
   - Wait a few minutes after publishing
   - Check Sonatype Nexus UI for staging repositories

### Getting Help

- [Sonatype Documentation](https://central.sonatype.org/publish/)
- [Gradle Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin)
- [GitHub Issues](https://github.com/Tap30/ripple-kotlin/issues)
