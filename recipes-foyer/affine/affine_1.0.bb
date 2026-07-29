SUMMARY = "AFFiNE, run as a podman Quadlet container against shared Postgres/Redis"
DESCRIPTION = "Ships only the Quadlet units. The container image itself is \
baked into the seed slot by foyer-container-seed and loaded into podman by \
foyer-seed-import, so an A/B update replaces the image alongside the OS while \
AFFiNE's own data stays on the data partition. Its database and cache are the \
shared foyer-postgres/foyer-redis services, provisioned via the \
pg-databases.d drop-in mechanism rather than private sidecars."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

INHIBIT_DEFAULT_DEPS = "1"

# Must match the version baked in via FOYER_SEED_IMAGES in
# foyer-container-seed_1.0.bb — the Quadlets reference the loaded image by tag.
AFFINE_VERSION = "0.27.3"

SRC_URI = "\
    file://affine.container.in \
    file://affine-migration.container.in \
    file://affine.pgdb \
    "

S = "${UNPACKDIR}"

# The image import, the /var/lib/affine bind mount, the shared network and
# the shared Postgres/Redis services all have to be in place before either
# container starts.
RDEPENDS:${PN} = "podman foyer-seed-import foyer-fs-layout foyer-container-net foyer-postgres foyer-redis"

do_install() {
    install -d ${D}${sysconfdir}/containers/systemd
    sed -e 's|@AFFINE_VERSION@|${AFFINE_VERSION}|g' \
        ${UNPACKDIR}/affine.container.in > ${D}${sysconfdir}/containers/systemd/affine.container
    sed -e 's|@AFFINE_VERSION@|${AFFINE_VERSION}|g' \
        ${UNPACKDIR}/affine-migration.container.in > ${D}${sysconfdir}/containers/systemd/affine-migration.container

    install -d ${D}${nonarch_libdir}/foyer/pg-databases.d
    install -m 0644 ${UNPACKDIR}/affine.pgdb ${D}${nonarch_libdir}/foyer/pg-databases.d/affine.conf
}

do_install[vardeps] += "AFFINE_VERSION"

FILES:${PN} += "\
    ${sysconfdir}/containers/systemd/affine.container \
    ${sysconfdir}/containers/systemd/affine-migration.container \
    ${nonarch_libdir}/foyer/pg-databases.d/affine.conf \
    "
