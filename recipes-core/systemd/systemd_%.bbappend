# /var/lib is made writable on our read-only rootfs by var-volatile-lib.service
# (oe-core's volatile-binds), which performs the bind mount directly with
# mount-copybind rather than through a static .mount unit. systemd only
# creates the corresponding var-lib.mount unit once it observes the mount
# appear in /proc/self/mountinfo, so services stock-ordered with a plain
# After=var-lib.mount (e.g. systemd-timesyncd, via its StateDirectory=) can
# race it at boot and fail with "Read-only file system" before the transient
# mount unit exists. RequiresMountsFor upgrades that to a hard dependency,
# which systemd resolves correctly regardless of ordering races.
#
# The default FILESPATH is anchored to the base recipe's own directory
# (oe-core), not this layer's — has to be added explicitly, same as
# recipes-core/rauc/rauc-conf.bbappend.
FILESEXTRAPATHS:prepend := "${THISDIR}/${BPN}:"

SRC_URI += "file://99-foyer-require-var-lib.conf"

do_install:append() {
    install -d ${D}${systemd_system_unitdir}/systemd-timesyncd.service.d/
    install -m 0644 ${UNPACKDIR}/99-foyer-require-var-lib.conf ${D}${systemd_system_unitdir}/systemd-timesyncd.service.d/
}
