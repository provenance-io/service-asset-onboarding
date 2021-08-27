# Asset Proto Format Standards

_AKA: Lessons we have learned the hard way_

- Where possible, follow the Uber style guide: https://github.com/uber/prototool/blob/dev/style/README.md

- For each enum, the first value should be `TYPENAME_UNKNOWN = 0;` (Do not use enum name `UNKNOWN` without a qualifier.)

- Proto Options all defined in the `options.proto` file and not in individual messages/files. Option field numbers
should be globally unique

- Why we represent UUID as string: https://newbedev.com/how-do-i-represent-a-uuid-in-a-protobuf-message

- `clang-format` docs https://releases.llvm.org/10.0.0/tools/clang/docs/ClangFormatStyleOptions.html

## Loans

- Only info common to all loans goes into the loan proto. For specific loan types, establish separate facts in the scope 
for that particular loan type


- what does and does NOT need to have a uuid on it


to consider:

- package structure
- file organization
- message names

https://github.com/marcoferrer/kroto-plus

https://github.com/envoyproxy/protoc-gen-validate

```
find . -name "*.proto"  -exec clang-format -i {} \;
 ```