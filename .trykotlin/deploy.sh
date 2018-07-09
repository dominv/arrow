#!/usr/bin/env bash

rm -rf .cache

export ARROW_VERSION=$(cat arrowktversion)

git checkout master

git pull

sh gradlew clean

sh gradlew war

sudo cp ./kotlin.web.demo.server/build/libs/WebDemoWar.war ./docker/frontend/war/WebDemoWar.war

sudo cp ./kotlin.web.demo.backend/build/libs/WebDemoBackend.war ./docker/backend/war/WebDemoBackend.war

sudo docker-compose down

sudo docker-compose build

sudo docker-compose up -d