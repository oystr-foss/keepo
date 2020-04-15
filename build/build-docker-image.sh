#!/usr/bin/env bash

if [ -d "tmp" ]; then
  rm -rf tmp
fi

name=keepo
playSecret=$(head -c 32 /dev/urandom | base64)
ipAddr=$(ip addr | grep "inet " | grep dynamic | awk '{print $2}' | awk -F"/" '{print $1}' || ifconfig | grep "inet " | grep netmask | grep -v 127.0.0.1 | grep -v 172.\*.\*.1 | awk '{print $2}')

echo "Building ${name}"

mkdir tmp
unzip -q ../target/universal/package.zip -d tmp

pip3 install -r requirements.txt &&
 ./get-build-info.py --repo .. --info --with-date > tmp/conf/build-info.txt
cp Dockerfile tmp

cat << EOF > tmp/run
#!/usr/bin/env bash
/opt/"${name}"/service/bin/run -Dconfig.file=/opt/"${name}"/service/shared/conf/local.conf -Dplay.http.secret.key="${playSecret}" -Dapp.server.addr="${ipAddr}"
EOF

chmod +x tmp/run
cd tmp || exit

docker build -t "${name}":v1.0 .
