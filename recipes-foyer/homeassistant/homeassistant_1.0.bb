SUMMARY = "Home Assistant, run as a podman Quadlet container"
DESCRIPTION = "Ships only the Quadlet unit. The container image itself is \
baked into the seed slot by foyer-container-seed and loaded into podman by \
foyer-seed-import, so an A/B update replaces the image alongside the OS while \
Home Assistant's own data stays on the data partition."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

INHIBIT_DEFAULT_DEPS = "1"

# Must match the version baked in via FOYER_SEED_IMAGES in
# foyer-container-seed_1.0.bb — the Quadlet references the loaded image by tag.
HOMEASSISTANT_VERSION = "2026.7.2"

SRC_URI = "file://homeassistant.container"

S = "${UNPACKDIR}"

# The image import and the /var/lib/homeassistant bind mount both have to be in
# place before the container starts.
RDEPENDS:${PN} = "podman foyer-seed-import foyer-fs-layout"

do_install() {
    install -d ${D}${sysconfdir}/containers/systemd
    install -m 0644 ${UNPACKDIR}/homeassistant.container \
        ${D}${sysconfdir}/containers/systemd/homeassistant.container
}

FILES:${PN} += "${sysconfdir}/containers/systemd/homeassistant.container"
