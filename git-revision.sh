#!/bin/sh
test -d .git || exit 1

SVN_VERSION=$(git merge-base HEAD master | git cat-file --batch | grep -o ^git-svn-id:\ http://josm.openstreetmap.de/svn/trunk\@\[0-9\]\* | tail -c +52)
GIT_REVISION=$(git rev-parse HEAD | head -c 7)
GIT_BRANCH=$(git name-rev --name-only HEAD)

test -z $SVN_VERSION || echo Revision: $SVN_VERSION
test -z $GIT_REVISION || echo Git-Revision: $GIT_REVISION
test -z $GIT_BRANCH || echo Git-Branch: $GIT_BRANCH
