#!/usr/bin/env bash

# Doc generator from: https://github.com/pseudomuto/protoc-gen-doc
# NOTE - the docs builder will not create links to top-level protos
#           correctly.  To fix, the docs/index.html file is sed'd to
#           `s/href="#([A-Z])/href="#.$1/g`

set -e

#EXTRA_CSS='.required {font-weight: bolder;}'
TMP_DIR=build/tmp-docs-protos
HTML_FILE="$(pwd)/docs/asset-protos.html"

docker pull pseudomuto/protoc-gen-doc

mkdir -p ${TMP_DIR}

cp -r src/main/proto/* ${TMP_DIR}
cp -r docs/validate ${TMP_DIR}

FILE_LIST=$( find ${TMP_DIR} -name "*.proto" | sort | grep -v "other_loan_types.proto")
FILE_LIST=$( echo $FILE_LIST | sed "s|${TMP_DIR}/||g" )

echo $FILE_LIST

set -x

docker run --entrypoint  "/bin/ls"  -v "$(pwd)/docs":/out -v "$(pwd)"/${TMP_DIR}:/protos pseudomuto/protoc-gen-doc "/protos"
echo " "
echo " "
docker run -v "$(pwd)/docs":/out -v "$(pwd)"/${TMP_DIR}:/protos pseudomuto/protoc-gen-doc $FILE_LIST --doc_opt=:validate/*

sed -e 's/href="#\([A-Z]\)/href="#\.\1/g' "$(pwd)/docs/index.html" > ${HTML_FILE}
rm "$(pwd)/docs/index.html"

# Fix up the title
sed -i '' -e 's/Protocol Documentation/Asset Model/' ${HTML_FILE}
# Fancy-up the CSS
#sed -i '' -e 's|Required|<span class="required">Required</span>|' ${HTML_FILE}

#sed -i '' -e "s|</style>|${EXTRA_CSS} </style>|" ${HTML_FILE}
