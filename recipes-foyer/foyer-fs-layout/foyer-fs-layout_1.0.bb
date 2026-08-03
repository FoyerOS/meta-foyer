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
    file://home.mount \
    file://var-lib-containers.mount \
    file://var-lib-homeassistant.mount \
    file://var-lib-foyer.mount \
    file://var-lib-affine.mount \
    file://var-lib-postgres.mount \
    file://var-lib-redis.mount \
    file://var-lib-cloud.mount \
    file://foyer-grow-data.service \
    file://foyer-grow-data \
    file://foyer-data-resize.service \
    file://foyer-data-resize \
    file://foyer.conf \
    "

S = "${UNPACKDIR}"

inherit systemd

# blkid/lsblk/sfdisk/partx/blockdev resolve and grow the foyer-data
# partition; findmnt and btrfs-tools grow the btrfs filesystem on it once
# mounted. blkid in particular is also what the overlayfs-etc preinit uses
# before udev exists, so busybox's version is not good enough.
# e2fsprogs-tune2fs is here for e2label, which foyer-bundle's post-install
# hook uses to restore a slot's filesystem label after RAUC overwrites it --
# rootA/rootB/seedA/seedB/foyer-config stay ext4.
RDEPENDS:${PN} = "\
    util-linux-blkid \
    util-linux-lsblk \
    util-linux-sfdisk \
    util-linux-partx \
    util-linux-blockdev \
    util-linux-findmnt \
    e2fsprogs-tune2fs \
    btrfs-tools \
    "

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/data.mount                  ${D}${systemd_system_unitdir}/data.mount
    install -m 0644 ${UNPACKDIR}/efi.mount                   ${D}${systemd_system_unitdir}/efi.mount
    install -m 0644 ${UNPACKDIR}/home.mount                  ${D}${systemd_system_unitdir}/home.mount
    install -m 0644 ${UNPACKDIR}/var-lib-containers.mount    ${D}${systemd_system_unitdir}/var-lib-containers.mount
    install -m 0644 ${UNPACKDIR}/var-lib-homeassistant.mount ${D}${systemd_system_unitdir}/var-lib-homeassistant.mount
    install -m 0644 ${UNPACKDIR}/var-lib-foyer.mount         ${D}${systemd_system_unitdir}/var-lib-foyer.mount
    install -m 0644 ${UNPACKDIR}/var-lib-affine.mount        ${D}${systemd_system_unitdir}/var-lib-affine.mount
    install -m 0644 ${UNPACKDIR}/var-lib-postgres.mount      ${D}${systemd_system_unitdir}/var-lib-postgres.mount
    install -m 0644 ${UNPACKDIR}/var-lib-redis.mount         ${D}${systemd_system_unitdir}/var-lib-redis.mount
    install -m 0644 ${UNPACKDIR}/var-lib-cloud.mount         ${D}${systemd_system_unitdir}/var-lib-cloud.mount
    install -m 0644 ${UNPACKDIR}/foyer-grow-data.service     ${D}${systemd_system_unitdir}/foyer-grow-data.service
    install -m 0644 ${UNPACKDIR}/foyer-data-resize.service   ${D}${systemd_system_unitdir}/foyer-data-resize.service

    install -d ${D}${libexecdir}/foyer
    install -m 0755 ${UNPACKDIR}/foyer-grow-data ${D}${libexecdir}/foyer/foyer-grow-data
    install -m 0755 ${UNPACKDIR}/foyer-data-resize ${D}${libexecdir}/foyer/foyer-data-resize

    install -d ${D}${nonarch_libdir}/tmpfiles.d
    install -m 0644 ${UNPACKDIR}/foyer.conf ${D}${nonarch_libdir}/tmpfiles.d/foyer.conf

    # Mount points. They have to exist in the read-only rootfs, since nothing
    # can create them at runtime.
    install -d ${D}/data
    install -d ${D}/efi
    install -d ${D}${localstatedir}/lib/containers
    install -d ${D}${localstatedir}/lib/homeassistant
    install -d ${D}${localstatedir}/lib/foyer
    install -d ${D}${localstatedir}/lib/affine
    install -d ${D}${localstatedir}/lib/postgres
    install -d ${D}${localstatedir}/lib/redis
    install -d ${D}${localstatedir}/lib/cloud
}

FILES:${PN} += "\
    ${systemd_system_unitdir}/foyer-grow-data.service \
    ${systemd_system_unitdir}/foyer-data-resize.service \
    ${libexecdir}/foyer/foyer-grow-data \
    ${libexecdir}/foyer/foyer-data-resize \
    ${nonarch_libdir}/tmpfiles.d/foyer.conf \
    /data \
    /efi \
    ${localstatedir}/lib/containers \
    ${localstatedir}/lib/homeassistant \
    ${localstatedir}/lib/foyer \
    ${localstatedir}/lib/affine \
    ${localstatedir}/lib/postgres \
    ${localstatedir}/lib/redis \
    ${localstatedir}/lib/cloud \
    "

# foyer-grow-data.service and foyer-data-resize.service are deliberately
# absent: neither has an [Install] section (nothing should pull them in on
# their own), so `systemctl enable` would have nothing to link. Both are
# started by data.mount's Requires=/Wants= instead, one on each side of the
# mount.
SYSTEMD_SERVICE:${PN} = "\
    data.mount \
    efi.mount \
    home.mount \
    var-lib-containers.mount \
    var-lib-homeassistant.mount \
    var-lib-foyer.mount \
    var-lib-affine.mount \
    var-lib-postgres.mount \
    var-lib-redis.mount \
    var-lib-cloud.mount \
    "
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
