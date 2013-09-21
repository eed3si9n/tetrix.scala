#!/bin/sh

docen=docs/$1/00.md
docja=docs.ja/$1/00.md

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
