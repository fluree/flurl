FROM debian:bullseye-slim

ADD https://github.com/fluree/flurl/releases/latest/download/flurl-linux /usr/local/bin/flurl
RUN ["chmod", "+x", "/usr/local/bin/flurl"]

ENTRYPOINT ["/usr/local/bin/flurl"]
