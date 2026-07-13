SUMMARY = "Home Assistant, baked in at build time as a podman Quadlet container"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

DEPENDS = "skopeo-native"
RDEPENDS:${PN} = "podman"

INHIBIT_DEFAULT_DEPS = "1"

HOMEASSISTANT_VERSION = "2026.7.2"
HOMEASSISTANT_IMAGE = "docker://ghcr.io/home-assistant/home-assistant:${HOMEASSISTANT_VERSION}"

SRC_URI = "\
    file://homeassistant.container \
    file://homeassistant-image-import.service \
    file://import-homeassistant-image \
    "

S = "${UNPACKDIR}"

inherit systemd

do_pull_image[network] = "1"
do_pull_image[nostamp] = "1"
do_pull_image[depends] = "skopeo-native:do_populate_sysroot"
addtask pull_image after do_unpack before do_install

do_pull_image() {
    skopeo copy --override-os linux --override-arch amd64 \
        ${HOMEASSISTANT_IMAGE} \
        oci-archive:${WORKDIR}/homeassistant.tar:homeassistant:${HOMEASSISTANT_VERSION}
}

do_install() {
    install -d ${D}${sysconfdir}/containers/systemd
    install -m 0644 ${UNPACKDIR}/homeassistant.container ${D}${sysconfdir}/containers/systemd/homeassistant.container

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/homeassistant-image-import.service ${D}${systemd_system_unitdir}/homeassistant-image-import.service

    install -d ${D}${libexecdir}/foyer
    install -m 0755 ${UNPACKDIR}/import-homeassistant-image ${D}${libexecdir}/foyer/import-homeassistant-image

    install -d ${D}/var/lib/foyer/containers
    install -m 0644 ${WORKDIR}/homeassistant.tar ${D}/var/lib/foyer/containers/homeassistant.tar

    install -d ${D}/var/lib/homeassistant
}

FILES:${PN} += "\
    ${sysconfdir}/containers/systemd/homeassistant.container \
    ${libexecdir}/foyer/import-homeassistant-image \
    /var/lib/foyer/containers/homeassistant.tar \
    /var/lib/homeassistant \
    "

SYSTEMD_SERVICE:${PN} = "homeassistant-image-import.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
