SUMMARY = "Mark the booted slot good once the system is up"
DESCRIPTION = "Runs `rauc status mark-good` after multi-user.target. Without \
it every boot leaves its try flag set in grubenv and the bootloader eventually \
falls back to the other slot, so this unit is what distinguishes a successful \
update from one that needs rolling back."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

INHIBIT_DEFAULT_DEPS = "1"

SRC_URI = "file://foyer-boot-confirm.service"

S = "${UNPACKDIR}"

inherit systemd

# grub-editenv is what RAUC's grub backend shells out to in order to clear the
# try flag; it is packaged separately from grub itself.
RDEPENDS:${PN} = "rauc grub-editenv"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/foyer-boot-confirm.service \
        ${D}${systemd_system_unitdir}/foyer-boot-confirm.service
}

SYSTEMD_SERVICE:${PN} = "foyer-boot-confirm.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
