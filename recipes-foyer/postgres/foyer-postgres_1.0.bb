SUMMARY = "Shared PostgreSQL, run as a podman Quadlet container"
DESCRIPTION = "Ships the Quadlet unit for a single shared PostgreSQL instance, \
plus the machinery that provisions a role and database per app on top of it. \
The container image itself is baked into the seed slot by \
foyer-container-seed and loaded into podman by foyer-seed-import, so an A/B \
update replaces the image alongside the OS while the database contents stay \
on the data partition."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

INHIBIT_DEFAULT_DEPS = "1"

SRC_URI = "\
    file://foyer-postgres.container \
    file://foyer-pg-secret \
    file://foyer-pg-provision \
    file://foyer-pg-provision.service \
    "

S = "${UNPACKDIR}"

inherit systemd

# The image import and the /var/lib/postgres bind mount both have to be
# in place before the container starts; foyer-container-net owns the shared
# network the container joins.
RDEPENDS:${PN} = "podman foyer-seed-import foyer-fs-layout foyer-container-net"

do_install() {
    install -d ${D}${sysconfdir}/containers/systemd
    install -m 0644 ${UNPACKDIR}/foyer-postgres.container \
        ${D}${sysconfdir}/containers/systemd/foyer-postgres.container

    install -d ${D}${libexecdir}/foyer
    install -m 0755 ${UNPACKDIR}/foyer-pg-secret    ${D}${libexecdir}/foyer/foyer-pg-secret
    install -m 0755 ${UNPACKDIR}/foyer-pg-provision ${D}${libexecdir}/foyer/foyer-pg-provision

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/foyer-pg-provision.service \
        ${D}${systemd_system_unitdir}/foyer-pg-provision.service

    # Drop-in directory that app recipes install <app>.conf into to request a
    # role and database of the same name.
    install -d ${D}${nonarch_libdir}/foyer/pg-databases.d
}

FILES:${PN} += "\
    ${sysconfdir}/containers/systemd/foyer-postgres.container \
    ${libexecdir}/foyer/foyer-pg-secret \
    ${libexecdir}/foyer/foyer-pg-provision \
    ${nonarch_libdir}/foyer/pg-databases.d \
    "

# foyer-postgres.container is a Quadlet, generated into a transient unit at
# boot rather than enabled here (same as homeassistant, which sets no
# SYSTEMD_SERVICE at all). foyer-pg-provision.service is a plain systemd unit
# and does need enabling.
SYSTEMD_SERVICE:${PN} = "foyer-pg-provision.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
