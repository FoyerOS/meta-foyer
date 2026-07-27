SUMMARY = "Container images baked into the Foyer seed slot"
DESCRIPTION = "Pulls the OCI images Foyer ships with at build time and lays \
them out, with a manifest, at the root of the seed filesystem. This is the \
only content of foyer-seed-image, which wic writes into the seedA partition \
and RAUC updates as a slot parented to the matching root slot."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

DEPENDS = "skopeo-native"

INHIBIT_DEFAULT_DEPS = "1"

# Images to bake in, as "<name>:<version>:<source>" triples. Adding another
# container here is the whole job — the import side is generic.
FOYER_SEED_IMAGES ?= "\
    homeassistant:2026.7.2:docker://ghcr.io/home-assistant/home-assistant \
    "

# skopeo needs the target architecture, not the build host's.
FOYER_SEED_ARCH ?= "amd64"
FOYER_SEED_ARCH:aarch64 = "arm64"
FOYER_SEED_ARCH:arm = "arm"

S = "${UNPACKDIR}"

do_pull_images[network] = "1"
do_pull_images[depends] = "skopeo-native:do_populate_sysroot"
do_pull_images[vardeps] = "FOYER_SEED_IMAGES FOYER_SEED_ARCH"
addtask pull_images after do_unpack before do_install

do_pull_images() {
    mkdir -p ${WORKDIR}/seed
    rm -f ${WORKDIR}/seed/seed.manifest

    for entry in ${FOYER_SEED_IMAGES}; do
        name=$(echo "$entry" | cut -d: -f1)
        version=$(echo "$entry" | cut -d: -f2)
        source=$(echo "$entry" | cut -d: -f3-)

        bbnote "Pulling $source:$version for $name (${FOYER_SEED_ARCH})"
        skopeo copy --override-os linux --override-arch ${FOYER_SEED_ARCH} \
            "$source:$version" \
            "oci-archive:${WORKDIR}/seed/$name-$version.tar:$name:$version"

        echo "$name $version $name-$version.tar" >> ${WORKDIR}/seed/seed.manifest
    done
}

do_install() {
    # Installed at the root of the filesystem: this package is the entire
    # content of foyer-seed-image, which becomes the seed partition, mounted
    # read-only at /usr/share/foyer/seed on the running system.
    install -d ${D}/
    install -m 0644 ${WORKDIR}/seed/seed.manifest ${D}/seed.manifest
    for tarball in ${WORKDIR}/seed/*.tar; do
        install -m 0644 "$tarball" ${D}/
    done
}

FILES:${PN} = "/seed.manifest /*.tar"

# The tarballs are already-compressed OCI archives; there is nothing for the
# packaging QA machinery to usefully do to them.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"
EXCLUDE_FROM_SHLIBS = "1"
