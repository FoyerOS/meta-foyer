require foyer-image.bb

# Virtual-disk formats for the major cloud/virt targets. All four conversions
# are builtin to image_types.bbclass and depend only on qemu-img
# (qemu-system-native); no extra recipe changes needed to enable them.
IMAGE_FSTYPES:append = " wic.qcow2 wic.vmdk wic.vhdx wic.vdi"
