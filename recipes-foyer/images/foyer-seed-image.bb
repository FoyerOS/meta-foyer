SUMMARY = "Foyer container image seed slot"
DESCRIPTION = "A filesystem containing nothing but the OCI archives Foyer \
ships with, plus a manifest. wic writes it into the seedA partition, and RAUC \
treats it as a slot parented to the matching root slot so an update replaces \
the OS and its container images together, and a rollback reverts both."
LICENSE = "MIT"

inherit image

IMAGE_INSTALL = "foyer-container-seed"
IMAGE_FEATURES = ""
IMAGE_LINGUAS = ""
NO_RECOMMENDATIONS = "1"

# Overrides the machine's "wic wic.zst ext4" — this image is a partition
# payload, not a bootable disk, and asking for wic here would recurse.
IMAGE_FSTYPES = "ext4"

# Read-only content of a known size; no room needed for runtime growth.
IMAGE_OVERHEAD_FACTOR = "1.0"
IMAGE_ROOTFS_EXTRA_SPACE = "65536"
