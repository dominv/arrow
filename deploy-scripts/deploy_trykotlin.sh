#!/usr/bin/env bash

# Add and pull latest version of trykotlin subtree
git subtree add --prefix .trykotlin --message="Add trykotlin subtree" https://github.com/dominv/kotlin-web-demo.git master
#git subtree pull --prefix .trykotlin --message="Update trykotlin subtree" https://github.com/dominv/kotlin-web-demo.git master --squash

# Update arrow version
echo $VERSION_NAME > .trykotlin/arrowktversion
git add .trykotlin/arrowktversion
git commit -m "Upgrading arrow version in trykolin"

# Push built subtree to trykotlinwebdemo
git subtree push --prefix .trykotlin https://github.com/dominv/kotlin-web-demo.git master
rm -rf .trykotlin
git add .trykotlin/
git commit -m "Cleaning trykotlin"