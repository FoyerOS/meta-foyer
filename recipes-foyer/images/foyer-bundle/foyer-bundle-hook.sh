#!/bin/sh
#
# RAUC slot hooks for Foyer.
#
# post-install: restore the filesystem label on the slot we just wrote.
#
# RAUC installs an ext4 slot by writing the bundle's filesystem image over the
# partition. That replaces the ext4 superblock wholesale, including its label —
# so a freshly updated rootB comes up labelled "" rather than "rootB". Three
# things in this design look slots up by filesystem label and all of them break
# silently without this hook:
#
#   * on EFI machines, grub's `search --no-floppy --label --set=root rootB`,
#     which then leaves $root pointing at the wrong device, so
#     `linux /boot/<kernel>` fails and the bootloader drops to its menu
#     instead of booting the update. The Pi's boot script instead uses fixed
#     GPT partition numbers (rootA is always partition 2, rootB always 3 —
#     see files/wic/foyer-partitions.wks.inc), so it does not depend on this
#     label at all; e2label is still required there because...
#   * foyer-seed-mount's `blkid -L seed<slot>`, on every machine;
#   * anything else keyed on the labels documented in files/wic/foyer-partitions.wks.inc.
#
# The GPT partition names are untouched by the write, so they stay correct; it
# is only the filesystem labels that need restoring.

set -e

case "$1" in
    slot-post-install)
        case "$RAUC_SLOT_NAME" in
            rootfs.0) label=rootA ;;
            rootfs.1) label=rootB ;;
            seed.0)   label=seedA ;;
            seed.1)   label=seedB ;;
            *)
                echo "foyer-bundle-hook: no label mapping for slot '$RAUC_SLOT_NAME', leaving it alone"
                exit 0
                ;;
        esac

        echo "foyer-bundle-hook: labelling $RAUC_SLOT_DEVICE as '$label'"
        e2label "$RAUC_SLOT_DEVICE" "$label"
        ;;
    *)
        exit 1
        ;;
esac

exit 0
