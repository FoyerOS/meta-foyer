SUMMARY = "Mark the booted slot good once the system is up"
DESCRIPTION = "Runs `rauc status mark-good` after multi-user.target. Without \
it every boot leaves its try flag set in grubenv and the bootloader eventually \
falls back to the other slot, so this unit is what distinguishes a successful \
update from one that needs rolling back."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

INHIBIT_DEFAULT_DEPS = "1"

SRC_URI = "file://foyer-boot-confirm.service file://foyer-boot-env-check"

S = "${UNPACKDIR}"

inherit systemd

# Without this, the "all"-arch package would bake in whichever machine
# happened to parse first and leak its RDEPENDS (e.g. grub-editenv) across
# every other machine that builds this recipe — the same reason
# rauc-conf.bbappend sets it.
PACKAGE_ARCH = "${MACHINE_ARCH}"

# The tool RAUC's bootloader backend shells out to in order to clear the try
# flag; grub-editenv on grub platforms, libubootenv-bin's fw_setenv on U-Boot
# ones. Packaged separately from the bootloader itself.
RDEPENDS:${PN} = "rauc ${FOYER_BOOT_TOOLS}"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/foyer-boot-confirm.service \
        ${D}${systemd_system_unitdir}/foyer-boot-confirm.service

    install -d ${D}${libexecdir}/foyer
    install -m 0755 ${UNPACKDIR}/foyer-boot-env-check \
        ${D}${libexecdir}/foyer/foyer-boot-env-check
}

FILES:${PN} += "${libexecdir}/foyer/foyer-boot-env-check"

SYSTEMD_SERVICE:${PN} = "foyer-boot-confirm.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
