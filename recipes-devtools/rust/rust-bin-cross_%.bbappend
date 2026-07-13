# meta-rust-bin only knows about series up to wrynose and falls back to WORKDIR
# for anything newer, but blacksail unpacks into UNPACKDIR like its predecessors.
# Drop once https://github.com/rust-embedded/meta-rust-bin/pull/239 lands upstream.
python () {
    if d.getVar("LAYERSERIES_COMPAT_core") == "blacksail":
        d.setVar("COMPATIBLE_PACKDIR", d.getVar("UNPACKDIR"))
        d.setVar("COMPATIBLE_PACKDIR_string", "UNPACKDIR")
}
