# service-asset-onboarding

Asset onboarding to Provenance blockchain

```
Service
                        _                          _                              _  _               
                       | |                        | |                            | |(_)              
  __ _  ___  ___   ___ | |_  ______   ___   _ __  | |__    ___    __ _  _ __   __| | _  _ __    __ _ 
 / _` |/ __|/ __| / _ \| __||______| / _ \ |  _ \ |  _ \  / _ \  / _` ||  /_| / _` || ||  _ \  / _` |
| (_| |\__ \\__ \|  __/| |_         | (_) || | | || |_) || (_) || (_| || |   | (_| || || | | || (_| |
 \__,_||___/|___/ \___| \__|         \___/ |_| |_||_.__/  \___/  \__,_||_|    \__,_||_||_| |_| \__, |
                                                                                                __/ |
                                                                                               |___/ 

Provenance Blockchain Foundation
```

### Api Key

TODO: Remove Api Key note before going public
https://github.com/FigureTechnologies/cloud/pull/1570/files

## Toolchain

These tools are known to work. You can use others if you wish, but YMMV.

- Azul JDK 11
- Kotlin 1.5.0
- IntelliJ 2021.x or newer

## TestNet Setup

Build the SDK and CLI app:
```shell
$ ./gradlew build -x test
```

Ensure that the asset onboarding service account has sufficient hash to write the specifications to the blockchain (`250,000,000nhash` should be sufficient).

Write the contract/scope/record specifications to the blockchain:
```shell
$ ./cli/bin/cli write-specs \
    --contract-spec-id ${ASSET_CONTRACT_SPEC_ID} \
    --scope-spec-id ${ASSET_SCOPE_SPEC_ID} \
    --key-mnemonic "${ASSET_ONBOARDING_SERVICE_TESTNET_BIP39_KEY_MNEMONIC}" \
    --chain-id pio-testnet-1 \
    --node https://XXX.XXX.XXX.XXX:9090
```

### for Loan State

```
./gradlew build
./dc.sh up
./dc.sh local_specs

Requestor key address tp1mryqzguyelef5dae7k6l22tnls93cvrc60tjdc
Broadcasting metadata TX (estimated gas: 83208, estimated fees: 198139050 nhash)...
TX (height: 0, txhash: 888F9E7BED315F81DC878D9BA7AB3310EE55EC7111033F4075ED7ADBE3B177E1, code: 4, gasWanted: 104010, gasUsed: 53800)
```

## Setup

Anyone wanting to onboard assets to Provenance will need a keypair and Provenance account. Generate one and remember it:
```shell
$ provenanced keys add SomeOnboarder --hd-path "44'/1'/0'/0/0"  -i --testnet
```

Alternatively for local testing, address `tp1mryqzguyelef5dae7k6l22tnls93cvrc60tjdc` from the following mnemonic is created and funded with `nhash`:

```
jealous bright oyster fluid guide talent crystal minor modify broken stove spoon pen thank action smart enemy chunk ladder soon focus recall elite pulp
```

## Run the service locally

```shell
$ ./dc.sh up
$ ./gradlew build service:bootrun
```

## Onboard using CLI

Build the CLI app:
```shell
$ ./gradlew build -x test
```

Onboard a test asset (using address from the above mentioned default mnemonic):
```shell
$ ./cli/bin/cli onboard cli/src/test/json/asset1.json
```

## Using testnet and mainnet

Environment variables for the `container` spring profile - configuration values for using public networks:

```
          testnet:
            PROVENANCE_IS_MAINNET: "false"
            ASSET_CONTRACT_SPEC_ID: "f97ecc5d-c580-478d-be02-6c1b0c32235f"
            ASSET_SCOPE_SPEC_ID: "551b5eca-921d-4ba7-aded-3966b224f44b"
            ASSET_MANAGER_PUBLIC_KEY: "BDSR5zvFFuLOMgqRH7MhPmpZzz3KlZL0oVQgbIuD4TCbhHUB2MgETo99sRm7xGJWIWGbBgcilc63mD0zxI6zUCo="
            LOAN_STATE_CONTRACT_SPEC_ID: "63a8bb4c-c6e0-4cb5-993b-b134c4b5cbbb"
            LOAN_STATE_SCOPE_SPEC_ID: "2eeada14-07cb-45fe-af6d-fdc48b627817"
          mainnet:
            PROVENANCE_IS_MAINNET: "true"
            ASSET_CONTRACT_SPEC_ID: "33bc3ec3-7623-476a-9713-b6efd47e3b74"
            ASSET_SCOPE_SPEC_ID: "1ae489e2-9c42-4d5d-aba9-b9e07de2f87e"
            LOAN_STATE_CONTRACT_SPEC_ID: "23089739-4fd7-4c70-8e32-a6f0500ef4c1"
            LOAN_STATE_SCOPE_SPEC_ID: "2148d958-2b92-440b-b7ff-ed0263583090"
            ASSET_MANAGER_PUBLIC_KEY: "BOWm7T9YcvJNqwi60haIQHoAj8YlyLguirg83cdo3tx3hUqSBMcwZdrLn6gCCYCcF2CtEZBTvFgYc2CmMCU5NQg="
```
