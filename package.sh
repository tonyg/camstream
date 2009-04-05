#!/bin/sh

set -e

variant=$1

if [ -z "$variant" ]
then
  echo "Usage: package.sh <variant>"
  exit 1
fi

base_url="-Dbase.url=http://cloud.github.com/downloads/tonyg/camstream/"

signing="-Dsigning.alias=camstream-unofficial -Dsigning.keystore=`pwd`/camstream-unofficial.keystore -Dsigning.storepass=changeit"
dist_target=sign-dist

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

cd $outputdir
mkdir flat
mkdir flat/tmp
cd flat/tmp
for d in ../../camcaptureJMF.zip ../../camdisplay.zip
do
    unzip $d '*.jar' '*.jnlp' || true
    mv */* ..
done
cd ..
rm -rf tmp
