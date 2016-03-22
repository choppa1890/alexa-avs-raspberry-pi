#!/bin/bash

# Ensure we are running on Raspbian
lsb_release -a 2>/dev/null | grep Raspbian
if [ "$?" -ne "0" ]; then
    echo "This OS is not Raspbian. Exiting..."
    exit 1
fi

# Determine which version of Raspbian we are running on
VERSION=`lsb_release -c 2>/dev/null | awk '{print $2}'`
echo "Version of Raspbian determined to be: $VERSION"

if [ "$VERSION" == "jessie" ]; then
    UBUNTU_VERSION="trusty"
elif [ "$VERSION" == "wheezy" ]; then
    UBUNTU_VERSION="precise"
else
    echo "Not running Raspbian Wheezy or Jessie. Exiting..."
    exit 1;
fi

# Remove any existing Java
sudo apt-get remove --purge oracle-java8-jdk oracle-java7-jdk openjdk-7-jre openjdk-8-jre

# Install Java from Ubuntu's PPA
# http://linuxg.net/how-to-install-the-oracle-java-8-on-debian-wheezy-and-debian-jessie-via-repository/
sudo sh -c "echo \"deb http://ppa.launchpad.net/webupd8team/java/ubuntu $UBUNTU_VERSION main\" >> /etc/apt/sources.list"
sudo sh -c "echo \"deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu $UBUNTU_VERSION main\" >> /etc/apt/sources.list"
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886
sudo apt-get update
sudo apt-get install oracle-java8-installer
sudo apt-get install oracle-java8-set-default
