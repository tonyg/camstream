#!/bin/sh

set -e

version=$1
variant=$2
type=$3
keystore=$4

if [ -z "$type" ]
then
  echo "Usage: package.sh <version> <variant> <dev|stage|live> <optional keystore location>"
  exit 1
fi

if [ "live" = "${type}" ]
then
  base_url="-Dbase.url=http://www.rabbitmq.com/examples/camstream"
else
  base_url="-Dbase.url=http://stage.rabbitmq.com/examples/camstream"
fi

if [ -n "${keystore}" ]
then
  signing="-Dsigning.alias=rabbitmq -Dsigning.keystore=${keystore} -Dsigning.storepass=changeit"
  dist_target=sign-dist
else
  signing=""
  dist_target=dist
fi

outputdir=packages/camstream/$version
mkdir -p $outputdir

function build_package () {
    package=$1
    echo ant ${signing} ${base_url} clean ${dist_target}
    (cd $package && ant ${signing} ${base_url} clean ${dist_target}) && \
	cp -r $package/build/dist $package-$version && \
	zip -r $outputdir/$package-$version.zip $package-$version && \
	rm -rf $package-$version
}

case "$variant" in
    linux)
	build_package camcaptureLinux
	;;
    osx)
	build_package camcaptureOSX
	;;
    jmf)
	;;
    *)
	echo "Available variants: osx, linux, jmf"
	exit 1
esac

build_package camcaptureJMF
build_package camdisplay

sourcepackagename=camstream-source-$version
sourcepackagedir=$outputdir/$sourcepackagename
darcs get . $sourcepackagedir && \
    rm -rf $sourcepackagedir/_darcs && \
    (cd $outputdir && zip -r $sourcepackagename.zip $sourcepackagename) && \
    rm -rf $sourcepackagedir
