# Default vfs driver avoids fakeroot issues during Yocto builds, but wastes
# gigabytes at runtime. We import at first boot, not build time, so use overlay.
do_install:append() {
    sed -i \
        -e 's|^driver = "vfs"|driver = "overlay"|' \
        ${D}${sysconfdir}/containers/storage.conf
}
