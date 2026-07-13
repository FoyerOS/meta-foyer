SUMMARY = "Foyer production image"
LICENSE = "MIT"

inherit core-image

IMAGE_FEATURES = ""

IMAGE_INSTALL = "\
    packagegroup-core-boot \
    nodejs \
    php \
    "
