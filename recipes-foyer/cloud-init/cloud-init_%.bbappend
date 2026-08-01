# Foyer ships cloud-init in the base image (not a cloud-only variant): besides
# the obvious cloud/virt use case, its NoCloud datasource reads user-data off
# a plain FAT partition, which gives headless first-boot provisioning on the
# Pi for free too.

# The default FILESPATH is anchored to the base recipe's own directory
# (meta-virtualization), not this layer's — has to be added explicitly, same
# as recipes-core/rauc/rauc-conf.bbappend.
FILESEXTRAPATHS:prepend := "${THISDIR}/${BPN}:"

SRC_URI += "file://99-foyer.cfg"

do_install:append() {
    install -d ${D}${sysconfdir}/cloud/cloud.cfg.d
    install -m 0644 ${UNPACKDIR}/99-foyer.cfg ${D}${sysconfdir}/cloud/cloud.cfg.d/99-foyer.cfg
}
