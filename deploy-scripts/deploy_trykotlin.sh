#!/usr/bin/env bash

# Add trykotlin subtree
git subtree add -P .trykotlin -m "Add trykotlin subtree" https://github.com/dominv/kotlin-web-demo.git master

# Update arrow version
echo $VERSION_NAME > .trykotlin/arrowKtVersion
git add .trykotlin/arrowKtVersion
git commit -m "Upgrading arrow version in trykotlin"

# Push built subtree to trykotlinwebdemo
git subtree push -P .trykotlin https://github.com/dominv/kotlin-web-demo.git master
rm -rf .trykotlin
git add .trykotlin/
git commit -m "Cleaning trykotlin"