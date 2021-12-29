(ns flurl.ledger-client
  (:require [org.httpkit.sni-client :as sni-client]
            [org.httpkit.client :as http]
            [fluree.crypto :as crypto]
            [clojure.string :as str]
            [flurl.debug :as debug])
  (:import (java.time ZonedDateTime ZoneOffset)
           (java.time.format DateTimeFormatter)
           (java.net URL)))

(alter-var-root
  #'org.httpkit.client/*default-client*
  (fn [_] sni-client/default-client))

(defn digest [req]
  (str "SHA-256=" (crypto/sha2-256 (:body req) :base64)))

(defn signing-string [req]
  (let [url    (-> req :url URL.)
        path   (str/lower-case (.getPath url))
        method (-> req :method name str/lower-case)]
    (str/join "\n" [(str "(request-target): " method " " path)
                    (str "date: " (get-in req [:headers "date"]))
                    (str "digest: " (get-in req [:headers "digest"]))])))

(defn sign-request
  ([private-key req]
   (sign-request private-key nil req))
  ([private-key date req]
   (let [dgst        (digest req)
         sign-date   (cond
                       (string? date)
                       date

                       (nil? date)
                       (.format (DateTimeFormatter/RFC_1123_DATE_TIME)
                                (ZonedDateTime/now (ZoneOffset/UTC)))

                       :else
                       (.format (DateTimeFormatter/RFC_1123_DATE_TIME)
                                date))

         req-to-sign (-> req
                         (assoc-in [:headers "date"] sign-date)
                         (assoc-in [:headers "digest"] dgst))
         sign-string (signing-string req-to-sign)
         sig         (crypto/sign-message sign-string private-key)]
     (debug/print "Signing string:" (pr-str sign-string))
     (assoc-in req-to-sign [:headers "signature"]
               (format "keyId=\"%s\",headers=\"%s\",algorithm=\"%s\",signature=\"%s\""
                       "na" "(request-target) date digest" "ecdsa-sha256"
                       sig)))))

(defn send-request [req]
  @(http/request req))
