(ns flurl.core
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [marker.core :refer [marker]]
            [flurl.debug :as debug]
            [flurl.ledger-client :as client])
  (:gen-class))

(def private-key-regex #"^[a-fA-F0-9]{64}$")

(defn private-key-literal-or-file? [v]
  (or (re-matches private-key-regex v)
      (let [f (io/file v)]
        (when (.exists f)
          (->> f slurp (re-matches private-key-regex))))))

(defn get-private-key [key-or-file]
  (if (re-matches private-key-regex key-or-file)
    key-or-file
    (when (.exists (io/file key-or-file))
      (debug/print "Reading private key from" key-or-file)
      (slurp key-or-file))))

(def cli-opts
  [["-d" "--debug" "Turn on debugging output"]
   ["-h" "--help" "Print this message"]
   ["-e" "--edn" "Use EDN for request-data instead of JSON"]
   ["-s" "--sign" "Enable request signing"]
   ["-k" "--private-key KEY"
    "Provide a private key or file containing one to sign requests with"
    :default "./default-private-key.txt"
    :validate [private-key-literal-or-file? "Must be 64 hex digits or path to a file containing the same"]]])

(defn usage [options-summary]
  (->> ["flurl is for sending and optionally signing API requests to Fluree ledger server's HTTP API"
        ""
        "Usage: flurl [options] api-endpoint [request-data]"
        " - api-endpoint should be a Fluree ledger API URL like http://localhost:8090/fdb/dbs"
        " - request-data is any data your request needs to send in JSON or EDN format (e.g. {\"select\": [\"*\"], \"from\": \"_user\"})"
        ""
        "Options:"
        options-summary
        ""]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline (map #(marker :red %) errors))))

(defn validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-opts)
        pk (-> options :private-key get-private-key)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}

      errors
      {:exit-message (error-msg errors)}

      (and (:sign options) (not pk))
      {:exit-message (error-msg ["Request signing requires a valid private key"])}

      (< 0 (count arguments) 3)
      {:endpoint (first arguments) :req-data (second arguments) :options options}

      :else
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn run [{:keys [endpoint req-data options]}]
  (when (:debug options) (debug/activate!))
  (let [private-key     (-> options :private-key get-private-key)
        sign-req?       (:sign options)
        use-edn?        (:edn options)
        req-data-parser (if use-edn? (comp json/encode edn/read-string) identity)
        req-body        (when req-data (req-data-parser req-data))
        req             (cond-> {:url     endpoint
                                 :method  :get
                                 :headers {"content-type" "application/json"}
                                 :output  :auto}

                                req-body
                                (assoc :method :post, :body req-body)

                                sign-req?
                                (client/sign-request private-key))
        _               (debug/print "API request:" (pr-str req))
        result          (client/send-request req)]
    (debug/print "API response:" (pr-str result))
    (when (or (:error result) (< 299 (:status result)))
      (print (marker :red (str (:status result) " ERROR: "))))
    (println (-> result :body (json/decode true)))))

(defn -main [& args]
  (let [{:keys [exit-message ok?] :as params} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (run params))))
