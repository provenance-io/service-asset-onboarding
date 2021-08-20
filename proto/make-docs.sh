#!/usr/bin/env bash

# Doc generator from: https://github.com/pseudomuto/protoc-gen-doc
# NOTE - the docs builder will not create links to top-level protos
#           correctly.  To fix, the docs/index.html file is sed'd to
#           `s/href="#([A-Z])/href="#.$1/g`

set -e

EXTRA_CSS='.required {font-weight: bolder;}'

docker pull pseudomuto/protoc-gen-doc

rm -rf docs/*

FILE_LIST=$( find src/main/proto -name "*.proto" | sort | grep -v "other_loan_types.proto")
FILE_LIST=$( echo $FILE_LIST | sed 's|src/main/proto/||g' )

HTML_FILE="$(pwd)/docs/asset-protos.html"

set -x

docker run --entrypoint  "/bin/ls"  -v "$(pwd)/docs":/out -v "$(pwd)"/src/main/proto:/protos pseudomuto/protoc-gen-doc "/protos"
echo " "
echo " "
docker run -v "$(pwd)/docs":/out -v "$(pwd)"/src/main/proto:/protos pseudomuto/protoc-gen-doc $FILE_LIST

sed -e 's/href="#\([A-Z]\)/href="#\.\1/g' "$(pwd)/docs/index.html" > ${HTML_FILE}
rm "$(pwd)/docs/index.html"

# Fix up the title
sed -i '' -e 's/Protocol Documentation/Asset Model/' ${HTML_FILE}
# Fancy-up the CSS
#sed -i '' -e 's|Required|<span class="required">Required</span>|' ${HTML_FILE}

#sed -i '' -e "s|</style>|${EXTRA_CSS} </style>|" ${HTML_FILE}
