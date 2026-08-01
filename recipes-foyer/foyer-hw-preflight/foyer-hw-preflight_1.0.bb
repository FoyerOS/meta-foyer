SUMMARY = "Refuse to start the application stack on underpowered hardware"
DESCRIPTION = "A oneshot unit that checks /proc/meminfo MemTotal against \
FOYER_MIN_MEMTOTAL_KB and fails (without halting the system) if the board \
is below the floor. RAM is a runtime property — a 4GB and an 8GB Pi 5 report \
the same Compatible= string — so this has to run on the booted system \
rather than at build time. concierge, haproxy and sshd are not gated on it, \
so an unsupported board stays reachable and diagnosable; only the app \
quadlets (homeassistant, affine, foyer-postgres, foyer-redis) depend on it."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

INHIBIT_DEFAULT_DEPS = "1"

SRC_URI = "file://foyer-hw-preflight file://foyer-hw-preflight.service"

S = "${UNPACKDIR}"

inherit systemd

do_install() {
    install -d ${D}${libexecdir}/foyer
    install -m 0755 ${UNPACKDIR}/foyer-hw-preflight ${D}${libexecdir}/foyer/foyer-hw-preflight

    install -d ${D}${systemd_system_unitdir}
    sed -e 's/@@FOYER_MIN_MEMTOTAL_KB@@/${FOYER_MIN_MEMTOTAL_KB}/' \
        ${UNPACKDIR}/foyer-hw-preflight.service \
        > ${D}${systemd_system_unitdir}/foyer-hw-preflight.service
}

do_install[vardeps] += "FOYER_MIN_MEMTOTAL_KB"

FILES:${PN} += "${libexecdir}/foyer/foyer-hw-preflight"

SYSTEMD_SERVICE:${PN} = "foyer-hw-preflight.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
