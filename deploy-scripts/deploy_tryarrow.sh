#!/usr/bin/env bash

# Add tryarrow subtree
git subtree add -P .tryarrow -m "Add tryarrow subtree" https://github.com/47deg/try.arrow-kt.io.git master

# Update arrow version
echo $VERSION_NAME > .tryarrow/arrowKtVersion
git add .tryarrow/arrowKtVersion
git commit -m "Upgrading arrow version in tryarrow"

# Push built subtree to tryarrow
git subtree push -P .tryarrow https://github.com/47deg/try.arrow-kt.io.git master

# Only needed in local to remove .tryarrow files. Not needed in arrowkt as long as this script will be run in travis
rm -rf .tryarrow
git add .tryarrow/
git commit -m "Cleaning tryarrow"