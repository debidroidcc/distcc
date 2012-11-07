#!/system/bin/sh

# this must be executed as superuser
# debian is then deployed in the /data directory

#spawn login shell
$busybox chroot $debian_dir /bin/bash -l

apt-get update
apt-get -y upgrade
apt-get -y install openssh-server vim net-tools