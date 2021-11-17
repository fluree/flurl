SOURCES := $(shell find src)

flurl: $(SOURCES) deps.edn .java-version
	clojure -M:native-image

.PHONY: clean
clean:
	rm -rf target/*
	rm -f flurl