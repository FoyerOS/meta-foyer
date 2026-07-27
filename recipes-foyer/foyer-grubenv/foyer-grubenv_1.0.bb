SUMMARY = "Initial GRUB environment block for Foyer's A/B slot selection"
DESCRIPTION = "Deploys a grubenv seeded with A_OK=1 so a freshly flashed image \
boots slot A. RAUC's grub bootloader backend rewrites this file in place with \
grub-editenv as slots are installed and marked good; it must already exist on \
the ESP, because grub-editenv can only update an existing block."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

DEPENDS = "grub-native"

INHIBIT_DEFAULT_DEPS = "1"

# Nothing to fetch, compile or package — this recipe only produces a deploy
# artifact that wic copies onto the ESP via IMAGE_EFI_BOOT_FILES.
do_fetch[noexec] = "1"
do_unpack[noexec] = "1"
do_patch[noexec] = "1"
do_configure[noexec] = "1"
do_compile[noexec] = "1"
do_install[noexec] = "1"
do_populate_sysroot[noexec] = "1"

PACKAGES = ""

inherit deploy nopackages

do_deploy() {
    grub-editenv ${DEPLOYDIR}/grubenv create
    grub-editenv ${DEPLOYDIR}/grubenv set ORDER="A B"
    grub-editenv ${DEPLOYDIR}/grubenv set A_OK=1
    grub-editenv ${DEPLOYDIR}/grubenv set B_OK=0
    grub-editenv ${DEPLOYDIR}/grubenv set A_TRY=0
    grub-editenv ${DEPLOYDIR}/grubenv set B_TRY=0
}

addtask deploy before do_build after do_install
