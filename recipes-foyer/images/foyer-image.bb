SUMMARY = "Foyer production image"
LICENSE = "MIT"

inherit core-image

IMAGE_FEATURES = ""

IMAGE_INSTALL = "\
    packagegroup-core-boot \
    podman \
    netavark \
    aardvark-dns \
    catatonit \
    fuse-overlayfs \
    slirp4netns \
    homeassistant \
    "
# Decompression of images needs space. Give it 8GB to have extra headroom
# TODO: Slim it down
IMAGE_ROOTFS_EXTRA_SPACE = "8388608"
