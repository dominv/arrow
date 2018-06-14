#!/usr/bin/env bash

sudo apt-get update

sudo apt-get install \
    apt-transport-https \
    ca-certificates \
    curl \
    software-properties-common

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -

sudo apt-key fingerprint 0EBFCD88

sudo add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"

sudo apt-get update

sudo apt-get -y install docker-ce

sudo service docker start

sudo apt-get -y install openjdk-8-jdk

sudo curl -L https://github.com/docker/compose/releases/download/1.21.2/docker-compose-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose

sudo apt install -y gradle

gradle wrapper

sh gradlew

git clone https://github.com/dominv/kotlin-web-demo

cd kotlin-web-demo/

sh gradlew

sh gradlew ::copyKotlinLibs

mkdir ./docker/frontend/war/

mkdir ./docker/backend/war/