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
    install -m 0644 ${UNPACKDIR}/home.mount                  ${D}${systemd_system_unitdir}/home.mount
    install -m 0644 ${UNPACKDIR}/var-lib-containers.mount    ${D}${systemd_system_unitdir}/var-lib-containers.mount
    install -m 0644 ${UNPACKDIR}/var-lib-homeassistant.mount ${D}${systemd_system_unitdir}/var-lib-homeassistant.mount
    install -m 0644 ${UNPACKDIR}/var-lib-foyer.mount         ${D}${systemd_system_unitdir}/var-lib-foyer.mount
    install -m 0644 ${UNPACKDIR}/var-lib-affine.mount        ${D}${systemd_system_unitdir}/var-lib-affine.mount
    install -m 0644 ${UNPACKDIR}/var-lib-postgres.mount      ${D}${systemd_system_unitdir}/var-lib-postgres.mount
    install -m 0644 ${UNPACKDIR}/var-lib-redis.mount         ${D}${systemd_system_unitdir}/var-lib-redis.mount
    install -m 0644 ${UNPACKDIR}/var-lib-cloud.mount         ${D}${systemd_system_unitdir}/var-lib-cloud.mount
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
    install -d ${D}${localstatedir}/lib/affine
    install -d ${D}${localstatedir}/lib/postgres
    install -d ${D}${localstatedir}/lib/redis
    install -d ${D}${localstatedir}/lib/cloud
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
    ${localstatedir}/lib/affine \
    ${localstatedir}/lib/postgres \
    ${localstatedir}/lib/redis \
    ${localstatedir}/lib/cloud \
    "

# foyer-grow-data.service is deliberately absent: it has no [Install] section
# (nothing should pull it in on its own) and is started via data.mount's
# Requires=, so `systemctl enable` would have nothing to link.
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
