# A bbappend, not a new recipe: RPI_EXTRA_IMAGE_BOOT_FILES and
# do_image_wic[depends] in meta-raspberrypi's rpi-base.inc already reference
# rpi-u-boot-scr for boot.scr, and a second recipe deploying boot.scr would
# race it.
#
# Upstream's do_compile seds @@KERNEL_IMAGETYPE@@/@@KERNEL_BOOTCMD@@/@@BOOT_MEDIA@@
# into boot.cmd from boot.cmd.in and mkimages it to boot.scr; we replace it to
# consume our own template (with the same output path) instead, because
# upstream's script reads bootargs out of the firmware DTB's /chosen and we
# set them ourselves — see files/foyer-boot.cmd.in for the A/B slot logic.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "file://foyer-boot.cmd.in"

do_compile() {
    sed -e 's/@@KERNEL_IMAGETYPE@@/${KERNEL_IMAGETYPE}/' \
        -e 's/@@KERNEL_BOOTCMD@@/${KERNEL_BOOTCMD}/' \
        -e 's/@@BOOT_MEDIA@@/${BOOT_MEDIA}/' \
        -e 's/@@FOYER_BOOTARGS_EXTRA@@/${FOYER_BOOTARGS_EXTRA}/' \
        "${UNPACKDIR}/foyer-boot.cmd.in" > "${WORKDIR}/boot.cmd"
    mkimage -A ${UBOOT_ARCH} -T script -C none -n "Boot script" -d "${WORKDIR}/boot.cmd" boot.scr
}
