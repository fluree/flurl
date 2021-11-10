# flurl

flurl is for sending and optionally signing API requests to Fluree ledger server's HTTP API.

Its name is a riff on curl since it kind of works similarly.

## Usage

`flurl [options] api-endpoint [request-data]`

 - api-endpoint should be a Fluree ledger API URL like `http://localhost:8090/fdb/dbs`
 - request-data is any data your request needs to send in EDN format (e.g. `{:select ["*"] :from "_user"}`)

### Options
 - -d, --debug           Turn on debugging output
 - -h, --help            Print help
 - -s, --sign            Enable request signing
 - -k, --private-key KEY Provide a private key or file containing one to sign requests with (defaults to `./private-key.txt`)

## Building

You'll need a GraalVM JDK installed with the native-image tool to build a binary.
You'll also need a recent version of the Clojure CLI tools.
Once those are in place, you can build a binary for your host platform by running `make`.
