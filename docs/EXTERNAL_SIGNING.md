# External release signing

Laqqinni and DarbakAdhkar keep their existing production keys in the owner's private Darbak Signing Keys folder. Their CI release intermediates are explicitly unsigned using `-PdarbakExternalSigning=true`; the normal release path still requires production signing configuration. An unsigned intermediate must never be distributed as an installable update.

DarbakTools 1.2.0 establishes its first stable release key, also backed up in that private folder as `tools-stable.jks` and `tools-keystore.properties`. Previous Tools CI produced ephemeral debug-signed APKs, with no stable signing configuration or Drive baseline available for comparison. Do not claim upgrade compatibility with those debug builds and do not ask users to uninstall an existing copy to force an update.

| App | Expected certificate SHA-256 |
|---|---|
| Laqqinni | b5e46c1606a6c3a20fd1207a4f6f286b340a64279e462d5ed79e7918177c9f92 |
| DarbakAdhkar | 7cc8f46d2c3b0160ec2c81e83bd63410652783adc0f1fbb971fa0f50420a533a |
| DarbakTools | 78a2dd4f09ccb61368776ef6211dc33260913c04d0d974d68b27aa5605fc2b47 |

Download the unsigned release artifact, then sign locally with Android SDK apksigner using the existing keystore and alias. Supply passwords through environment variables or protected files, never command output or repository content. Enable APK signature scheme v2 for API 25 and verify the final APK with `apksigner verify --verbose --print-certs`. Match the certificate above, check package/version/minSdk and non-debuggable status, and compare the embedded approved ownership image with the original bytes. Record the final SHA-256 and size only after signing.

The September 2026 identity bundle is delivered for owner testing on the actual 1024×600 car screen. Publishing a trial bundle does not certify real-device GPS, audio hardware, external storage or power-cycle behavior. Production updater feeds remain subject to the existing release gate.
