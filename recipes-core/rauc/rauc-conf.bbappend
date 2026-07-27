# system.conf lives under files/${MACHINE}/ so a future foyer-aarch64 can ship
# its own with bootloader=uboot without disturbing this one.
PACKAGE_ARCH = "${MACHINE_ARCH}"

# Verification keyring.
#
# No key material is committed to this layer. Run scripts/foyer-rauc-dev-ca to
# generate a development CA into FOYER_RAUC_PKI_DIR; the next build picks it up
# automatically and bakes the certificate into the image. Without it the build
# still succeeds, falling back to meta-rauc's placeholder certificate (with a
# bbwarn from the recipe), which is enough to boot but will reject every bundle
# at install time.
FOYER_RAUC_PKI_DIR ??= "${TOPDIR}/foyer-pki"

# Put the PKI directory on the file search path rather than overriding
# RAUC_KEYRING_URI with an absolute file:// URI. bitbake's file fetcher mirrors
# absolute paths into a directory tree under UNPACKDIR (sources/home/...), so
# the upstream recipe's `install ${UNPACKDIR}/ca.cert.pem` cannot find it.
# Searching this directory first means the stock relative URI resolves to our
# certificate when it exists, and to meta-rauc's placeholder when it does not.
FILESEXTRAPATHS:prepend := "${FOYER_RAUC_PKI_DIR}:"
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

RAUC_KEYRING_FILE = "ca.cert.pem"

python () {
    import os
    # Register the certificate as a parse dependency so that generating the CA
    # invalidates bitbake's cache. Otherwise "no CA yet" is baked into the parse
    # cache, the placeholder silently stays in the image, and every bundle is
    # rejected at install time with nothing in the build output to explain why.
    bb.parse.mark_dependency(d, os.path.join(d.getVar('FOYER_RAUC_PKI_DIR'), 'ca.cert.pem'))
}
