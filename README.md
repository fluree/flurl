# flurl

flurl is for sending and optionally signing API requests to Fluree ledger server's HTTP API.

Its name is a riff on curl since it kind of works similarly.

## Usage

`flurl [options] api-endpoint [request-data]`

 - api-endpoint should be a Fluree ledger API URL like `http://localhost:8090/fdb/dbs`
 - request-data is any data your request needs to send in JSON or EDN format (e.g. `{"select": ["*"], "from": "_user"}`)

### Options
 - -d, --debug           Turn on debugging output
 - -h, --help            Print help
 - -e, --edn             Use EDN for request-data instead of JSON
 - -s, --sign            Enable request signing
 - -k, --private-key KEY Provide a private key or file containing one to sign requests with (defaults to `./default-private-key.txt`)

## Building

You'll need a GraalVM JDK installed with the native-image tool to build a binary.
You'll also need a recent version of the Clojure CLI tools.
Once those are in place, you can build a binary for your host platform by running `make`.

### Building in Docker

GraalVM builds in Docker can be tricky because native-image's resource usage
can get quite high. I recommend only building on the build machine's native
platform (i.e. don't rely on the built-in qemu emulation of other
architectures). You may also need to increase the memory and CPU allocated to
your Docker Desktop VM if you're using that.

To build in Docker run: `docker build -f Dockerfile.build -t flurl:local .`
If you're using buildx you may need to add the `--load` arg to the above
command.

This will result in a runnable image named `flurl:local`. You can also copy the
resulting Linux binary out of the image like so:

`docker create --name flurl-bin flurl:local`
`docker cp flurl-bin:/usr/local/bin/flurl .`

You'll then have the compiled native Linux binary in your current directory.
