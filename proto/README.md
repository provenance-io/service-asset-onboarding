# Asset Proto Format Standards

_AKA: Lessons we have learned the hard way_

- Where possible, follow the Uber style guide: https://github.com/uber/prototool/blob/dev/style/README.md

- For each enum, the first value should be `TYPENAME_UNKNOWN = 0;` (Do not use enum name `UNKNOWN` without a qualifier.)

- Proto Options all defined in the `options.proto` file and not in individual messages/files. Option field numbers
should be globally unique

- Why we represent UUID as string: https://newbedev.com/how-do-i-represent-a-uuid-in-a-protobuf-message

## Loans

- Only info common to all loans goes into the loan proto. For specific loan types, establish separate facts in the scope 
for that particular loan type





to consider:

- package structure
- file organization
- message names