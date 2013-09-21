#!/bin/sh

## mod.sh 2

withzero=0$1

mkdir docs/$withzero
mkdir docs.ja/$withzero

docen=docs/$withzero/00.md
docja=docs.ja/$withzero/00.md

cp ../eed3si9n.com/original/tetrix-in-scala-day$1.md $docen
cp ../eed3si9n.com/original/tetrix-in-scala-day$1.ja.md $docja

perl -pi -e 's/\$/\\\$/g;' $docen
perl -pi -e 's/<scala>/\`\`\`scala/g;' $docen
perl -pi -e 's/<\/scala>/\`\`\`/g;' $docen
perl -pi -e 's/<code>/\`\`\`/g;' $docen
perl -pi -e 's/<\/code>/\`\`\`/g;' $docen

perl -pi -e 's/\$/\\\$/g;' $docja
perl -pi -e 's/<scala>/\`\`\`scala/g;' $docja
perl -pi -e 's/<\/scala>/\`\`\`/g;' $docja
perl -pi -e 's/<code>/\`\`\`/g;' $docja
perl -pi -e 's/<\/code>/\`\`\`/g;' $docja
