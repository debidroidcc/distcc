﻿
#!/system/bin/sh

# this must be executed as superuser
# debian is then deployed in the /data directory

# break on errors:
set -e

# some constants:
debootstrap_file_url=http://debian-armhf-bootstrap.googlecode.com/files/debian_bootstrap.tar.gz
busybox=/data/data/de.tubs.ibr.distcc/app_bin/busybox
target_mountpoint=/data # this one might be derived from the $target_dir via df or mount
target_dir=$target_mountpoint
debian_dir=$target_dir/debian

# remount target mountpoint
echo "Remounting $target_mountpoint"
$busybox mount -o remount,exec,dev,suid $target_mountpoint

# unpack bootstrapped debian:
cd $target_dir
echo "downloading debian_bootstrap.tar.gz"
$busybox wget -O debian_bootstrap.tar.gz $debootstrap_file_url
echo "untaring..."
$busybox tar -xzf debian_bootstrap.tar.gz
$busybox rm debian_bootstrap.tar.gz

# second stage:
export PATH=/usr/bin:/usr/sbin:/bin:$PATH
export HOME=/root
$busybox chroot $debian_dir /debootstrap/debootstrap --second-stage

# config stuff:
echo 'deb http://ftp.us.debian.org/debian/ sid main contrib non-free' >> $debian_dir/etc/apt/sources.list
echo 'nameserver 8.8.8.8' >> $debian_dir/etc/resolv.conf

# some useful mounts
$busybox chroot $debian_dir /bin/mount -t devpts devpts /dev/pts
$busybox chroot $debian_dir /bin/mount -t proc proc /proc
$busybox chroot $debian_dir /bin/mount -t sysfs sysfs /sys

# apt-get stuff
echo "calling apt-get update"
$busybox chroot $debian_dir /usr/bin/apt-get update
echo "calling apt-get -y upgrade"
$busybox chroot $debian_dir /usr/bin/apt-get -y upgrade
echo "calling apt-get -y install libgmp3-dev libmpfr-dev libmpc-dev"
$busybox chroot $debian_dir /usr/bin/apt-get -y install libgmp3-dev libmpfr-dev libmpc-dev
echo "calling apt-get -y install openssh-server vim net-tools wget gcc make sudo patch"
$busybox chroot $debian_dir /usr/bin/apt-get -y install openssh-server vim net-tools wget gcc make sudo patch build-essentials


# replace sshd port & restart
cat $debian_dir/etc/ssh/sshd_config | sed -e 's/Port 22/Port 222/g' > $debian_dir/etc/ssh/sshd_config
$busybox chroot $debian_dir /etc/init.d/ssh restart

$busybox chroot $debian_dir /usr/bin/wget https://raw.github.com/debidroidcc/debidroidcc/master/build-cross-cc.sh -O /opt/build-cross-cc.sh --no-check-certificate
echo 'building cross compiler, this might take a few minutes!'
$busybox chroot $debian_dir /bin/bash /opt/build-cross-cc.sh 1> /dev/null
echo 'cross compiler completed...'

# spawn login shell
# $busybox chroot $debian_dir /bin/bash -l
