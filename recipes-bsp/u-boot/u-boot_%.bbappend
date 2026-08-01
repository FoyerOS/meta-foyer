# u-boot-configure.inc merges any .cfg file found in SRC_URI with
# merge_config.sh, so this is enough to fold foyer-uboot.cfg into whichever
# defconfig UBOOT_MACHINE selects.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://foyer-uboot.cfg"
