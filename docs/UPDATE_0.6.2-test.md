# LUNARVOIDSMP 0.6.2-test

This is a candidate test build based on the supplied 0.6.1-test source archive.

Changes in this packaging revision:
- Bumped Maven artifact and plugin.yml version to 0.6.2-test.
- Updated the GitHub Actions artifact name/path for the new version.
- Set conservative auction defaults (10 active listings per player, 0% sale fee) so the configured listing and purchase flow is not blocked by null owner settings.
- Enabled the documented full-stack purchase confirmation layout.

Important: this is not yet a verified compiled JAR. Run the GitHub Actions workflow on Java 25 and inspect the build result before installing it. Test on a backup/test server first.
