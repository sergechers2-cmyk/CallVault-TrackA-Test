# CallVault Track A Test

Experimental isolated debug build for testing Track A on the user's Redmi Note 14 Pro+ 5G.

The workflow pins upstream CallVault to tag v2.2.0, applies a small overlay, builds an isolated debug APK, verifies it, and uploads the APK as a GitHub Actions artifact.

This is a diagnostic build, not a production release. It does not replace the existing working CallVault installation.

## Build

Open **Actions → Build CallVault Track A Test → Run workflow**.

The resulting artifact is named `CallVault-TrackA-Test-v2.2.0`.
