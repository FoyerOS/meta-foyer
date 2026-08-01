SUMMARY = "Foyer production image"
LICENSE = "MIT"

inherit core-image

# read-only-rootfs is what makes A/B honest: an update replaces a whole slot
# with no drift to lose. overlayfs-etc gives /etc back as writable, with its
# upper layer on the config partition so it survives updates.
IMAGE_FEATURES = "read-only-rootfs overlayfs-etc"

# image_types_wic.bbclass unconditionally sets WKS_FILE_DEPENDS_BOOTLOADERS:aarch64
# to "grub-efi systemd-boot", purely from the TUNE_ARCH override matching —
# irrelevant on the Pi, which boots through neither. A plain or :aarch64
# assignment in the machine conf can't beat this (class assignments,
# inherited before this point in the recipe, still win over config-file
# ones for the same override); the machine-specific override here, applied
# after `inherit core-image` in this same recipe's parse, does.
WKS_FILE_DEPENDS_BOOTLOADERS:foyer-rpi5 = ""
WKS_FILE_DEPENDS_BOOTLOADERS:foyer-rpi4 = ""

IMAGE_INSTALL = "\
    packagegroup-core-boot \
    ${FOYER_KERNEL_IMAGE_PKG} \
    kernel-modules \
    podman \
    netavark \
    aardvark-dns \
    catatonit \
    fuse-overlayfs \
    slirp4netns \
    homeassistant \
    foyer-container-net \
    foyer-postgres \
    foyer-redis \
    affine \
    rust \
    concierge \
    haproxy \
    foyer-fs-layout \
    foyer-base-users \
    foyer-hw-preflight \
    foyer-seed-import \
    foyer-boot-confirm \
    rauc \
    rauc-conf \
    ${FOYER_BOOT_TOOLS} \
    cloud-init \
    cloud-init-systemd \
    shadow \
    "

# Firmware for the bare-metal NIC/microcode support foyer-net-x86.cfg and
# foyer-platform-x86.cfg turned on (see recipes-kernel/linux). Intel
# microcode is deliberately not here — oe-core has no intel-microcode
# recipe, only meta-intel does, and pulling in a whole BSP layer for one
# package is a poor trade; document the gap instead.
IMAGE_INSTALL:append:foyer-x86-64 = " linux-firmware-microcode-amd linux-firmware-bnx2x"

# cloud-init's own systemd units (and the generator that enables cloud-init.target
# dynamically at boot based on datasource detection) live in a separate
# "-systemd" subpackage that plain "cloud-init" does not RDEPENDS on — without
# this cloud-init installs but nothing ever runs it.

# Without this, /usr/bin/passwd is only busybox's applet, whose crypt
# implementation cannot verify a SHA-512 ($6$) hash and fails with "bad
# salt" — which breaks the forced admin password change end to end, since
# OpenSSH execs /usr/bin/passwd directly when PAM reports a required change.
# shadow's own PACKAGECONFIG already turns on PAM support because DISTRO
# has "pam" in DISTRO_FEATURES, and its ALTERNATIVE_PRIORITY (200) beats
# busybox's, so this is enough to make /usr/bin/passwd resolve correctly —
# except that image.bbclass's ROOTFS_RO_UNNEEDED unconditionally strips
# "shadow" back out of any read-only-rootfs image afterwards, on the
# assumption a read-only /etc has no use for it. That assumption doesn't
# hold here: overlayfs-etc makes /etc writable, so keep it.
ROOTFS_RO_UNNEEDED:remove = "shadow"

# bmaptool can flash a mostly-empty 19GB sparse image in the time it takes to
# write the ~2GB that is actually populated — this matters a lot on the Pi's
# SD card write speeds.
IMAGE_FSTYPES:append = " wic.bmap"

# The image is written into a fixed-size root slot (FOYER_ROOT_SLOT_SIZE), and
# nothing writes to the rootfs at runtime any more — the 8GB of headroom that
# used to exist purely so `podman load` could unpack into /var is gone along
# with the in-rootfs container tarball.
IMAGE_ROOTFS_EXTRA_SPACE = "131072"

# The ESP is built by the bootimg_efi wic plugin; drop in the initial boot env
# block alongside the bootloader so RAUC has a block to edit on the very first
# boot. Empty on platforms with no EFI boot partition (e.g. the Pi).
IMAGE_EFI_BOOT_FILES = "${FOYER_IMAGE_EFI_BOOT_FILES}"

# Artifacts the disk layout pulls in that bitbake cannot infer from the .wks:
# the seed filesystem written into seedA by rawcopy, and the initial boot env
# block above.
do_image_wic[depends] += "\
    foyer-seed-image:do_image_complete \
    ${FOYER_BOOTENV_DEPLOY_DEP} \
    "

# The included partitions file (and, on platforms still using a static grub
# config, the grub config itself) are referenced by absolute path rather than
# through WKS_FILE, so neither is covered by the wks checksum; register them
# explicitly or edits to the boot/partition logic will not trigger a rebuild.
do_image_wic[file-checksums] += "${FOYER_WKS_EXTRA_CHECKSUMS}"

# foyer-base-users' useradd invocation stamps today's date into shadow's
# last-changed field; overwrite it with 0 so pam_unix (like `chage -d 0`)
# requires a new password before the placeholder admin account can do
# anything else. Done here rather than in a first-boot service so it is
# deterministic and independent of post-install-logging.
ROOTFS_POSTPROCESS_COMMAND += "foyer_force_admin_password_change; "

foyer_force_admin_password_change () {
    shadow="${IMAGE_ROOTFS}${sysconfdir}/shadow"
    mode=$(stat -c %a "$shadow")
    sed -i -E 's/^(admin:[^:]*):[^:]*:/\1:0:/' "$shadow"
    chmod "$mode" "$shadow"
}
