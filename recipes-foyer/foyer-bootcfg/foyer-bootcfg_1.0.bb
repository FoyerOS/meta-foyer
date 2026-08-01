SUMMARY = "Initial GRUB environment and rendered grub.cfg for Foyer's A/B slot selection"
DESCRIPTION = "Deploys a grubenv seeded with A_OK=1 so a freshly flashed image \
boots slot A. RAUC's grub bootloader backend rewrites this file in place with \
grub-editenv as slots are installed and marked good; it must already exist on \
the ESP, because grub-editenv can only update an existing block. Also renders \
files/wic/foyer-grub.cfg.in into a deployed foyer-grub.cfg, substituting each \
machine's kernel image name and extra bootargs — this is what lets one grub \
A/B algorithm serve every EFI/grub machine (foyer-x86-64, foyer-arm64) \
instead of duplicating it per platform."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

DEPENDS = "grub-native"

# Fails loudly if a non-grub machine ever ends up depending on this by
# accident, rather than silently deploying a boot config nobody reads.
COMPATIBLE_MACHINE = "foyer-x86-64|foyer-arm64"

INHIBIT_DEFAULT_DEPS = "1"

# Referenced by absolute path below rather than through SRC_URI/FILESPATH:
# it also has to stay findable at the fixed path files/wic/foyer-grub.cfg.in
# for anyone editing boot logic to find both grub templates next to each
# other, the same way the wks files already reference it.
FOYER_GRUB_CFG_TEMPLATE = "${FOYER_LAYERDIR}/files/wic/foyer-grub.cfg.in"

# Nothing to fetch, build or package — this recipe only produces deploy
# artifacts that wic copies onto the ESP (grubenv) or reads directly by
# absolute path (foyer-grub.cfg).
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

    sed \
        -e 's|@@KERNEL_IMAGETYPE@@|${KERNEL_IMAGETYPE}|g' \
        -e 's|@@FOYER_BOOTARGS_EXTRA@@|${FOYER_BOOTARGS_EXTRA}|g' \
        ${FOYER_GRUB_CFG_TEMPLATE} > ${DEPLOYDIR}/foyer-grub.cfg
}

do_deploy[file-checksums] += "${FOYER_GRUB_CFG_TEMPLATE}:True"
do_deploy[vardeps] += "KERNEL_IMAGETYPE FOYER_BOOTARGS_EXTRA"

addtask deploy before do_build after do_install
