SUMMARY = "Foyer filesystem layout: config/data partition mounts and first-boot growth"
DESCRIPTION = "Mount units and helpers that turn the GPT layout from \
files/wic/foyer.wks.in into a running system: the ESP at /efi, the data \
partition at /data with the persistent parts of /var bound onto it, and a \
first-boot resize so one image fits any disk size. Everything is addressed by \
filesystem label so the image is portable across QEMU, bare metal and SBCs."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "\
    file://data.mount \
    file://efi.mount \
    file://var-lib-containers.mount \
    file://var-lib-homeassistant.mount \
    file://var-lib-foyer.mount \
    file://foyer-grow-data.service \
    file://foyer-grow-data \
    file://foyer.conf \
    "

S = "${UNPACKDIR}"

inherit systemd

# blkid/lsblk/sfdisk/partx resolve and resize the partition; e2fsck and
# resize2fs grow the filesystem. blkid in particular is also what the
# overlayfs-etc preinit uses before udev exists, so busybox's version is not
# good enough. e2fsprogs-tune2fs is here for e2label, which foyer-bundle's
# post-install hook uses to restore a slot's filesystem label after RAUC
# overwrites it.
RDEPENDS:${PN} = "\
    util-linux-blkid \
    util-linux-lsblk \
    util-linux-sfdisk \
    util-linux-partx \
    util-linux-blockdev \
    e2fsprogs-e2fsck \
    e2fsprogs-resize2fs \
    e2fsprogs-tune2fs \
    "

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/data.mount                  ${D}${systemd_system_unitdir}/data.mount
    install -m 0644 ${UNPACKDIR}/efi.mount                   ${D}${systemd_system_unitdir}/efi.mount
    install -m 0644 ${UNPACKDIR}/var-lib-containers.mount    ${D}${systemd_system_unitdir}/var-lib-containers.mount
    install -m 0644 ${UNPACKDIR}/var-lib-homeassistant.mount ${D}${systemd_system_unitdir}/var-lib-homeassistant.mount
    install -m 0644 ${UNPACKDIR}/var-lib-foyer.mount         ${D}${systemd_system_unitdir}/var-lib-foyer.mount
    install -m 0644 ${UNPACKDIR}/foyer-grow-data.service     ${D}${systemd_system_unitdir}/foyer-grow-data.service

    install -d ${D}${libexecdir}/foyer
    install -m 0755 ${UNPACKDIR}/foyer-grow-data ${D}${libexecdir}/foyer/foyer-grow-data

    install -d ${D}${nonarch_libdir}/tmpfiles.d
    install -m 0644 ${UNPACKDIR}/foyer.conf ${D}${nonarch_libdir}/tmpfiles.d/foyer.conf

    # Mount points. They have to exist in the read-only rootfs, since nothing
    # can create them at runtime.
    install -d ${D}/data
    install -d ${D}/efi
    install -d ${D}${localstatedir}/lib/containers
    install -d ${D}${localstatedir}/lib/homeassistant
    install -d ${D}${localstatedir}/lib/foyer
}

FILES:${PN} += "\
    ${systemd_system_unitdir}/foyer-grow-data.service \
    ${libexecdir}/foyer/foyer-grow-data \
    ${nonarch_libdir}/tmpfiles.d/foyer.conf \
    /data \
    /efi \
    ${localstatedir}/lib/containers \
    ${localstatedir}/lib/homeassistant \
    ${localstatedir}/lib/foyer \
    "

# foyer-grow-data.service is deliberately absent: it has no [Install] section
# (nothing should pull it in on its own) and is started via data.mount's
# Requires=, so `systemctl enable` would have nothing to link.
SYSTEMD_SERVICE:${PN} = "\
    data.mount \
    efi.mount \
    var-lib-containers.mount \
    var-lib-homeassistant.mount \
    var-lib-foyer.mount \
    "
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
