(ns flurl.debug
  (:require [marker.core :refer [marker]]
            [clojure.string :as str])
  (:refer-clojure :exclude [print]))

(defonce on? (atom false))

(defn activate! []
  (reset! on? true))

(defn print [& msgs]
  (when @on?
    (let [msg (str/join " " (cons "DEBUG:" msgs))]
      (println (marker :yellow msg)))))
