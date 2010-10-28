#!/bin/sh
test -d .git || exit 1

SVN_REVISION=$(git rev-list HEAD | git cat-file --batch | grep -o ^git-svn-id:\ http://josm.openstreetmap.de/svn/trunk@[0-9]\* | head -n 1 | tail -c +52)
GIT_REVISION=$(git rev-parse HEAD | head -c 7)
GIT_BRANCH=$(git name-rev --name-only HEAD)

test -z "$GIT_REVISION" || git update-index -q --refresh && test -z "$(git diff-index --name-only HEAD --)" || GIT_REVISION="$GIT_REVISION-dirty"

test -z $1 || echo -n > $1
test -z "$SVN_REVISION" || echo "Revision: $SVN_REVISION" && test -z $1 || echo "svn.revision=$SVN_REVISION" >> $1
test -z "$GIT_REVISION" || echo "Git-Revision: $GIT_REVISION" && test -z $1 || echo "git.revision=$GIT_REVISION" >> $1
test -z "$GIT_BRANCH" || echo "Git-Branch: $GIT_BRANCH" && test -z $1 || echo "git.branch=$GIT_BRANCH" >> $1
