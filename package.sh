#!/bin/sh

set -e

variant=$1
keystore=$2

if [ -z "$variant" ]
then
  echo "Usage: package.sh <variant> <optional keystore location>"
  exit 1
fi

base_url="-Dbase.url=http://stage.rabbitmq.com/examples/camstream"

if [ -n "${keystore}" ]
then
  signing="-Dsigning.alias=rabbitmq -Dsigning.keystore=${keystore} -Dsigning.storepass=changeit"
  dist_target=sign-dist
else
  signing=""
  dist_target=dist
fi

outputdir=packages/camstream
mkdir -p $outputdir

function build_package () {
    package=$1
    echo ant ${signing} ${base_url} clean ${dist_target}
    (cd $package && ant ${signing} ${base_url} clean ${dist_target}) && \
	cp -r $package/build/dist $outputdir/$package && \
	(cd $outputdir; zip -r $package.zip $package) && \
	rm -rf $outputdir/$package
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
