# Knobs shared by every Foyer machine. Each foyer-*.conf requires this file
# and overrides only what its platform actually needs to differ on — the goal
# is that recipes and wks files never hardcode a platform name, only these
# variables.

# --- partition sizing (see files/wic/foyer-partitions.wks.inc) ---
FOYER_ESP_SIZE        ?= "128M"
# foyer-image's rootfs measures ~856M; 3G leaves room to grow. Oversizing a
# slot is nearly free: the empty space costs nothing in wic.zst, and RAUC
# bundles carry a content-sized filesystem rather than a slot-sized one.
# foyer-image-dev overrides this — dbg-pkgs/dev-pkgs take it to ~4G.
FOYER_ROOT_SLOT_SIZE  ?= "3G"
# Archives are ~ their compressed registry size: Home Assistant is ~594 MiB
# compressed (~660M as an ext4); with AFFiNE, foyer-postgres and foyer-redis
# alongside it the four images total ~997 MiB, so 1536M would fit but with
# almost no headroom left to grow.
FOYER_SEED_SLOT_SIZE  ?= "2560M"
FOYER_CONFIG_SIZE     ?= "64M"
# Shipped size; foyer-grow-data.service expands it to fill the disk on first
# boot, which is what lets one image serve an SD card and a 1TB SSD. It still
# has to be large enough to work *without* growing, because the .wic is sized
# exactly to its partitions — flashed to a same-size target (or booted straight
# under QEMU) there is no free space to claim. First boot alone needs room for
# podman to untar the seed archives and populate its storage: ~2.2G for Home
# Assistant alone, plus roughly another 1.2G uncompressed for AFFiNE,
# foyer-postgres and foyer-redis — before either Postgres's own data or
# AFFiNE's blob storage grows on top of that. 512M was not enough and failed
# with ENOSPC.
FOYER_DATA_SIZE       ?= "8G"

# --- bootloader/kernel indirection ---
# kernel.bbclass names the deployed package kernel-image-${KERNEL_IMAGETYPE,,},
# so this has to track KERNEL_IMAGETYPE per machine (bzImage on x86, a plain
# "Image" everywhere else).
FOYER_KERNEL_IMAGE_PKG     ?= "kernel-image-bzimage"
FOYER_BOOT_TOOLS           ?= "grub-editenv"
FOYER_BOOTENV_DEPLOY_DEP   ?= "foyer-bootcfg:do_deploy"
FOYER_IMAGE_EFI_BOOT_FILES ?= "grubenv;EFI/BOOT/grubenv"
FOYER_WKS_EXTRA_CHECKSUMS  ?= "${FOYER_LAYERDIR}/files/wic/foyer-partitions.wks.inc:True"
# Extra kernel command-line fragment appended after the A/B slot-selection
# arguments (root=, rauc.slot=). Empty by default; platforms whose console
# does not default to a sane tty override it (see foyer-rpi5.conf).
FOYER_BOOTARGS_EXTRA       ?= ""
# RAM is a runtime property, so this is enforced by foyer-hw-preflight on the
# booted system, not at build time. 0 disables the gate entirely.
FOYER_MIN_MEMTOTAL_KB      ?= "0"

# foyer-partitions.wks.inc is pulled in by wic's `include`, so wic — not
# bitbake — expands these, out of the generated <image>.env file. Without
# this, wic fails with "cannot expand variable" for anything referenced only
# from the included file.
WICVARS:append = " FOYER_ROOT_SLOT_SIZE FOYER_SEED_SLOT_SIZE FOYER_CONFIG_SIZE FOYER_DATA_SIZE IMAGE_NAME_SUFFIX"

# --- persistent /etc on the config partition ---
# overlayfs-etc.bbclass requires these to be machine specific. The device is
# resolved by filesystem label rather than a device node: our preinit template
# (files/overlayfs-etc-preinit.sh.in) runs as PID 1 before udev exists, so
# /dev/disk/by-* is unavailable and the node name differs per platform
# (vda / sda / nvme0n1p6 / mmcblk0p6).
OVERLAYFS_ETC_MOUNT_POINT   = "/config"
OVERLAYFS_ETC_FSTYPE        = "ext4"
OVERLAYFS_ETC_DEVICE        = "LABEL=foyer-config"
OVERLAYFS_ETC_MOUNT_OPTIONS = "defaults"
OVERLAYFS_ETC_INIT_TEMPLATE = "${FOYER_LAYERDIR}/files/overlayfs-etc-preinit.sh.in"
