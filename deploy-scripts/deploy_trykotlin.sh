#!/usr/bin/env bash

# Add trykotlin subtree
git subtree add --prefix .trykotlin --message="Add trykotlin subtree" https://github.com/dominv/kotlin-web-demo.git master

# Update arrow version
echo $VERSION_NAME > .trykotlin/arrowktversion
git add .trykotlin/arrowktversion

# Push built subtree to trykotlinwebdemo
git subtree push --prefix .trykotlin https://github.com/dominv/kotlin-web-demo.git master
rm -rf .trykotlin
git add .trykotlin/
git commit -m "Cleaning trykotlin"