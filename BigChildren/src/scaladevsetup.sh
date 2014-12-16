#!/bin/sh

download() {
  local zipname="${1##*/}"
  if [ ! -f ./$zipname ]; then
    echo "downloading "$1
    curl $1 -O
  fi
  tar -C /tmp -zxvf $zipname
  sudo su -c "mv /tmp/$2 /opt/$2"
  sudo su -c "ln -s /opt/$2/$2 /usr/local/bin/$2"
}

# Create java directory in home for java related installs that need to write
if [ ! -f ~/java ]; then
  mkdir ~/java
fi
# Create dirs for SBT
if [ ! -f ~/bin ]; then
  mkdir ~/bin
fi
if [ ! -f ~/.sbt/0.13/plugins/plugins.sbt ]; then
  mkdir -p ~/.sbt/0.13/plugins/
  echo 'addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.4.0")' >> ~/.sbt/0.13/plugins/plugins.sbt
fi

# various packages
sudo apt-get install nautilus-open-terminal
sudo apt-get install vim
sudo apt-get install curl
sudo apt-get install xclip
sudo apt-get install grsync
sudo apt-get install p7zip-rar p7zip-full unace unrar zip unzip sharutils rar uudeview mpack arj cabextract file-roller
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
#java 7 and java 8 - defaulting to java 7
sudo apt-get install oracle-java7-installer
sudo apt-get install oracle-java8-installer
sudo update-java-alternatives -s java-7-oracle
sudo apt-get install ant
#install git
#sudo add-apt-repository ppa:git-core/ppa
#sudo apt-get update
sudo apt-get install git

cd ~/Downloads

#scala
curl http://www.scala-lang.org/files/archive/scala-2.10.4.tgz -O
tar -C /tmp -zxvf scala-2.10.4.tgz
sudo su -c "mv /tmp/scala-2.10.4 /opt/scala"
#sbt
curl http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.13.2/sbt-launch.jar -O
mv sbt-launch.jar ~/bin/
launcher=~/bin/sbt
echo 'SBT_OPTS="-Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256M"' > $launcher
echo 'java $SBT_OPTS -jar `dirname $0`/sbt-launch.jar "$@"' >> $launcher
chmod u+x ~/bin/sbt
#play
curl http://downloads.typesafe.com/play/2.2.2/play-2.2.2.zip -O
unzip play-2.2.2.zip -d /tmp
mv /tmp/play-2.2.2 ~/java/play
#solr
curl http://ftp.kddilabs.jp/infosystems/apache/lucene/solr/4.7.2/solr-4.7.2.tgz -O
tar -C /tmp -zxvf solr-4.7.2.tgz
mv /tmp/solr-4.7.2 ~/java/solr

#Eclipse with Scala IDE
launcher=/tmp/tmp.desktop
download http://downloads.typesafe.com/scalaide-pack/3.0.3.vfinal-210-20140327/scala-SDK-3.0.3-2.10-linux.gtk.x86_64.tar.gz eclipse
touch $launcher
chmod o+w $launcher
echo '[Desktop Entry]' > $launcher
echo 'Version=3.0.3' >> $launcher
echo 'Name=Eclipse' >> $launcher
echo 'Exec=env UBUNTU_MENUPROXY=0 /opt/eclipse/eclipse' >> $launcher
echo 'Icon=/opt/eclipse/icon.xpm' >> $launcher
echo 'Type=Application' >> $launcher
echo 'Terminal=false' >> $launcher
echo 'Categories=IDE;Development' >> $launcher
chmod o-w $launcher
sudo mv $launcher /usr/share/applications/Eclipse.desktop

#LightTable
download https://d35ac8ww5dfjyg.cloudfront.net/playground/bins/0.6.5/LightTableLinux64.tar.gz LightTable
launcher=/tmp/tmp.desktop
touch $launcher
chmod o+w $launcher
echo '[Desktop Entry]' > $launcher
echo 'Version=0.6.5' >> $launcher
echo 'Name=LightTable' >> $launcher
echo 'Exec=env UBUNTU_MENUPROXY=0 /opt/LightTable/LightTable' >> $launcher
echo 'Icon=/opt/LightTable/core/img/lticon.png' >> $launcher
echo 'Type=Application' >> $launcher
echo 'Terminal=false' >> $launcher
echo 'Categories=IDE;Development' >> $launcher
chmod o-w $launcher
sudo mv $launcher /usr/share/applications/LightTableLauncher.desktop
