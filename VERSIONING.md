# Releasing

1. Update the `VERSION_NAME` in `gradle.properties` to the release version.

2. Update the `CHANGELOG.md`:
   1. Change the `Unreleased` header to the release version.
   2. Add a link URL to ensure the header link works.
   3. Add a new `Unreleased` section to the top.

3. Update the `README.md` so the "Download" section reflects the new release version.

4. Commit

   ```
   $ git commit -am "Prepare version X.Y.Z"
   ```

5. Tag

   ```
   $ git tag -am "Version X.Y.Z" X.Y.Z
   ```

6. Update the `VERSION_NAME` in `gradle.properties` to the next "SNAPSHOT" version.

7. Commit

   ```
   $ git commit -am "Prepare next development version"
   ```

8. Push!

   ```
   $ git push && git push --tags
   ```

   This will trigger a GitHub Action workflow which will create a GitHub release and upload the
   release artifacts to Maven Central.

## Example Release Process

For releasing version 1.0.0:

1. **Prepare release:**
   ```bash
   # Update gradle.properties
   VERSION_NAME=1.0.0
   
   # Update CHANGELOG.md
   ## [1.0.0] - 2024-01-15
   ### Added
   - Initial release
   
   # Update README.md download versions to 1.0.0
   
   git commit -am "Prepare version 1.0.0"
   git tag -am "Version 1.0.0" 1.0.0
   ```

2. **Prepare next development version:**
   ```bash
   # Update gradle.properties
   VERSION_NAME=1.1.0-SNAPSHOT
   
   git commit -am "Prepare next development version"
   ```

3. **Push:**
   ```bash
   git push && git push --tags
   ```

The GitHub Action will automatically:
- Run tests
- Build all modules
- Sign artifacts with GPG
- Publish to Maven Central
- Create GitHub release with release notes
