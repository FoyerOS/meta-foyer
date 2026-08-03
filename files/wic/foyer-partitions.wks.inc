# Partitions 2-7 of the Foyer OS disk layout — the part of the contract every
# machine shares. rootA is always GPT partition 2 and rootB always partition
# 3; the Pi's U-Boot boot script (files/foyer-boot.cmd.in) hardcodes those
# partition numbers, so this file must stay the single definition included by
# every per-machine wks rather than be duplicated.
#
#   2 rootA         ext4   OS slot A, populated at build         A/B
#   3 rootB         ext4   OS slot B, empty until first update   A/B
#   4 seedA         ext4   container image tarballs for slot A   A/B
#   5 seedB         ext4   container image tarballs for slot B   A/B
#   6 foyer-config  ext4   /etc overlay upper+work      never touched by updates
#   7 foyer-data    btrfs  all runtime state            never touched by updates
#
# Every partition carries the same string as both its filesystem label
# (--label) and its GPT partition name (--part-name), because the two are read
# by different consumers and neither covers all cases:
#   * the kernel's root= understands PARTLABEL= (GPT name) but not LABEL=
#     without an initramfs;
#   * grub's `search --label` and blkid -L read the filesystem label;
#   * the overlayfs-etc preinit runs before udev, so only blkid works there.
#
# --no-fstab-update everywhere: wic would otherwise inject device-node or UUID
# fstab entries, which is exactly the non-portable coupling this layout avoids.
# All mounts are declared by label in foyer-fs-layout instead.
#
# NOTE: one directive per line, no backslash continuations — wic's kickstart
# parser runs shlex.split() on each line individually and a trailing backslash
# raises "No escaped character".

part / --source rootfs --fstype=ext4 --label rootA --part-name rootA --align 1024 --fixed-size ${FOYER_ROOT_SLOT_SIZE} --no-fstab-update

part --fstype=ext4 --label rootB --part-name rootB --align 1024 --fixed-size ${FOYER_ROOT_SLOT_SIZE} --no-fstab-update

# rawcopy resolves this against DEPLOY_DIR_IMAGE. IMAGE_NAME_SUFFIX (".rootfs")
# is part of the deployed name, so spell it with the variable rather than
# hardcoding the infix.
#
# --fstype=ext4 is required even though rawcopy just dd's a prebuilt
# filesystem: wic's default fstype is vfat, and rawcopy picks the labelling
# tool from it, so without this it runs dosfslabel against an ext4 image.
part --source rawcopy --sourceparams="file=foyer-seed-image-${MACHINE}${IMAGE_NAME_SUFFIX}.ext4" --fstype=ext4 --label seedA --part-name seedA --align 1024 --fixed-size ${FOYER_SEED_SLOT_SIZE} --no-fstab-update

part --fstype=ext4 --label seedB --part-name seedB --align 1024 --fixed-size ${FOYER_SEED_SLOT_SIZE} --no-fstab-update

part --fstype=ext4 --label foyer-config --part-name foyer-config --align 1024 --fixed-size ${FOYER_CONFIG_SIZE} --no-fstab-update

# Last partition on purpose: foyer-grow-data.service expands it to fill the
# disk on first boot, so one image fits an SD card and a 1TB SSD alike.
part --fstype=btrfs --label foyer-data --part-name foyer-data --align 1024 --fixed-size ${FOYER_DATA_SIZE} --no-fstab-update
