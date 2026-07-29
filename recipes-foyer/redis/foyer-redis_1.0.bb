SUMMARY = "Shared Redis, run as a podman Quadlet container"
DESCRIPTION = "Ships only the Quadlet unit. The container image itself is \
baked into the seed slot by foyer-container-seed and loaded into podman by \
foyer-seed-import, so an A/B update replaces the image alongside the OS while \
Redis's own data stays on the data partition. One instance serves many apps \
via distinct db indices, allocated in a comment in foyer-redis.container."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

INHIBIT_DEFAULT_DEPS = "1"

SRC_URI = "file://foyer-redis.container"

S = "${UNPACKDIR}"

# The image import and the /var/lib/redis bind mount both have to be in
# place before the container starts; foyer-container-net owns the shared
# network the container joins.
RDEPENDS:${PN} = "podman foyer-seed-import foyer-fs-layout foyer-container-net"

do_install() {
    install -d ${D}${sysconfdir}/containers/systemd
    install -m 0644 ${UNPACKDIR}/foyer-redis.container \
        ${D}${sysconfdir}/containers/systemd/foyer-redis.container
}

FILES:${PN} += "${sysconfdir}/containers/systemd/foyer-redis.container"
