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

(defn get-private-key [key-or-file]
  (if (re-matches private-key-regex key-or-file)
    key-or-file
    (when (.exists (io/file key-or-file))
      (slurp key-or-file))))

(def cli-opts
  [["-d" "--debug" "Turn on debugging output"]
   ["-h" "--help"  "Print this message"]
   ["-s" "--sign"  "Enable request signing"]
   ["-k" "--private-key KEY"
    "Provide a private key or file containing one to sign requests with"
    :default (get-private-key "./private-key.txt")
    :default-desc "./private-key.txt"
    :parse-fn get-private-key
    :validate [#(and % (re-matches private-key-regex %) "Must be 64 hex digits")]]])

(defn usage [options-summary]
  (->> ["flurl is for sending and optionally signing API requests to Fluree ledger server's HTTP API"
        ""
        "Usage: flurl [options] api-endpoint [request-data]"
        " - api-endpoint should be a Fluree ledger API URL like http://localhost:8090/fdb/dbs"
        " - request-data is any data your request needs to send in EDN format (e.g. {:select [\"*\"] :from \"_user\"})"
        ""
        "Options:"
        options-summary
        ""]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-opts)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}

      errors
      {:exit-message (error-msg errors)}

      (and (:sign options) (not (:private-key options)))
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
  (let [private-key (:private-key options)
        sign-req?   (:sign options)
        req-body    (when req-data (-> req-data edn/read-string json/encode))
        req         (cond-> {:url     endpoint
                             :method  :get
                             :headers {"content-type" "application/json"}
                             :output  :auto}

                      req-body
                      (assoc :method :post, :body req-body)

                      sign-req?
                      (client/sign-request private-key))
        _           (debug/print "API request:" (pr-str req))
        result      (client/send-request req)]
    (debug/print "API response:" (pr-str result))
    (when (or (:error result) (< 299 (:status result)))
      (print (marker :red (str (:status result) " ERROR: "))))
    (println (-> result :body (json/decode true)))))

(defn -main [& args]
  (let [{:keys [exit-message ok?] :as params} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (run params))))
