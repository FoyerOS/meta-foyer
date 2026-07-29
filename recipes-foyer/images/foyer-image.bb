SUMMARY = "Foyer production image"
LICENSE = "MIT"

inherit core-image

# read-only-rootfs is what makes A/B honest: an update replaces a whole slot
# with no drift to lose. overlayfs-etc gives /etc back as writable, with its
# upper layer on the config partition so it survives updates.
IMAGE_FEATURES = "read-only-rootfs overlayfs-etc"

IMAGE_INSTALL = "\
    packagegroup-core-boot \
    kernel-image-bzimage \
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
    foyer-fs-layout \
    foyer-seed-import \
    foyer-boot-confirm \
    rauc \
    rauc-conf \
    grub-editenv \
    "

# The image is written into a fixed-size root slot (FOYER_ROOT_SLOT_SIZE), and
# nothing writes to the rootfs at runtime any more — the 8GB of headroom that
# used to exist purely so `podman load` could unpack into /var is gone along
# with the in-rootfs container tarball.
IMAGE_ROOTFS_EXTRA_SPACE = "131072"

# The ESP is built by the bootimg_efi wic plugin; drop in the initial grubenv
# alongside grub so RAUC has a block to edit on the very first boot.
IMAGE_EFI_BOOT_FILES = "grubenv;EFI/BOOT/grubenv"

# Artifacts the disk layout pulls in that bitbake cannot infer from the .wks:
# the seed filesystem written into seedA by rawcopy, and the grubenv above.
do_image_wic[depends] += "\
    foyer-seed-image:do_image_complete \
    foyer-grubenv:do_deploy \
    "

# The grub config is referenced by absolute path from foyer.wks.in, so it is
# not covered by the wks checksum; register it explicitly or edits to the boot
# logic will not trigger a rebuild.
do_image_wic[file-checksums] += "${FOYER_LAYERDIR}/files/wic/foyer-grub.cfg:True"
