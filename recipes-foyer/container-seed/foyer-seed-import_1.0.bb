SUMMARY = "Import container images from the Foyer seed slot into podman storage"
DESCRIPTION = "Mounts the seed partition matching the booted root slot and \
loads the OCI archives it carries into podman. Generic over the seed \
manifest, so shipping another container is a change to \
FOYER_SEED_IMAGES in foyer-container-seed, not to this recipe."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "\
    file://foyer-seed-mount \
    file://foyer-seed-mount.service \
    file://foyer-seed-import \
    file://foyer-seed-import.service \
    "

S = "${UNPACKDIR}"

inherit systemd

RDEPENDS:${PN} = "podman util-linux-blkid util-linux-mountpoint foyer-fs-layout"

do_install() {
    install -d ${D}${libexecdir}/foyer
    install -m 0755 ${UNPACKDIR}/foyer-seed-mount  ${D}${libexecdir}/foyer/foyer-seed-mount
    install -m 0755 ${UNPACKDIR}/foyer-seed-import ${D}${libexecdir}/foyer/foyer-seed-import

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/foyer-seed-mount.service  ${D}${systemd_system_unitdir}/foyer-seed-mount.service
    install -m 0644 ${UNPACKDIR}/foyer-seed-import.service ${D}${systemd_system_unitdir}/foyer-seed-import.service

    # Mount point for the seed slot, which must exist in the read-only rootfs.
    install -d ${D}${datadir}/foyer/seed
}

FILES:${PN} += "\
    ${libexecdir}/foyer/foyer-seed-mount \
    ${libexecdir}/foyer/foyer-seed-import \
    ${datadir}/foyer/seed \
    "

SYSTEMD_SERVICE:${PN} = "foyer-seed-mount.service foyer-seed-import.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
