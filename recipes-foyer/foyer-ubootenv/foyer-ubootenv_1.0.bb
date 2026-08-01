SUMMARY = "Initial U-Boot environment for Foyer's A/B slot selection on the Pi"
DESCRIPTION = "Deploys a uboot.env seeded with BOOT_ORDER='A B', BOOT_A_LEFT=3, \
BOOT_B_LEFT=0 — the U-Boot-side analogue of foyer-bootcfg's grubenv — so a \
freshly flashed image boots slot A. files/foyer-boot.cmd.in rewrites this \
file in place at every boot via fatload/env import/env export/fatwrite; this \
recipe only seeds the very first copy, which has to already exist because \
the boot script's fatload otherwise has nothing to import."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

DEPENDS = "u-boot-mkenvimage-native libubootenv-native"

COMPATIBLE_MACHINE = "^rpi$"
PACKAGE_ARCH = "${MACHINE_ARCH}"

INHIBIT_DEFAULT_DEPS = "1"

SRC_URI = "file://fw_env.config"

do_patch[noexec] = "1"
do_configure[noexec] = "1"
do_compile[noexec] = "1"
do_populate_sysroot[noexec] = "1"

S = "${UNPACKDIR}"

inherit deploy

# The size here must match CONFIG_ENV_SIZE (foyer-uboot.cfg), foyer_envsize
# in foyer-boot.cmd.in and the third field of fw_env.config below — all four
# address the same on-disk bytes in the same env-file format.
FOYER_UBOOTENV_SIZE = "0x4000"

do_install() {
    install -d ${D}${sysconfdir}
    install -m 0644 ${UNPACKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
}

do_deploy() {
    cat > ${WORKDIR}/uboot.env.txt <<EOF
BOOT_ORDER=A B
BOOT_A_LEFT=3
BOOT_B_LEFT=0
EOF
    mkenvimage -s ${FOYER_UBOOTENV_SIZE} -o ${DEPLOYDIR}/uboot.env ${WORKDIR}/uboot.env.txt

    # Read the just-deployed image back with the real tool against a config
    # staged to point at it directly (same offset/size as the real
    # fw_env.config, just a different device path). The failure mode this
    # guards against is nasty precisely because it doesn't fail loudly on
    # its own: fw_setenv can succeed against the wrong bytes, and rauc
    # status would then report healthy while the attempt counter behind it
    # never moves. Cheaper to catch a size/format mismatch here than to
    # find out via a silent rollback loop on hardware.
    printf '%s\t0x0000\t%s\n' "${DEPLOYDIR}/uboot.env" "${FOYER_UBOOTENV_SIZE}" > ${WORKDIR}/fw_env.config.staged
    got=$(fw_printenv -c ${WORKDIR}/fw_env.config.staged BOOT_ORDER)
    if [ "$got" != "BOOT_ORDER=A B" ]; then
        bbfatal_log "foyer-ubootenv: deployed uboot.env failed readback: got '$got', expected 'BOOT_ORDER=A B'"
    fi
}

addtask deploy before do_build after do_install

FILES:${PN} = "${sysconfdir}/fw_env.config"
