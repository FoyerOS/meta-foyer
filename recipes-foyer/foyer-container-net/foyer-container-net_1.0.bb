SUMMARY = "Shared podman network for Foyer's app containers"
DESCRIPTION = "Ships only the Quadlet .network unit that defines the 'foyer' \
user-defined podman network. Postgres, Redis and every app container that \
talks to them join this network by name, so it is its own recipe rather than \
being duplicated into each app's RDEPENDS."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

INHIBIT_DEFAULT_DEPS = "1"

SRC_URI = "file://foyer.network"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "podman aardvark-dns netavark"

do_install() {
    install -d ${D}${sysconfdir}/containers/systemd
    install -m 0644 ${UNPACKDIR}/foyer.network \
        ${D}${sysconfdir}/containers/systemd/foyer.network
}

FILES:${PN} += "${sysconfdir}/containers/systemd/foyer.network"
