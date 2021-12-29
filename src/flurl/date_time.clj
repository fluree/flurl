(ns flurl.date-time
  (:require [cap10morgan.parse-date-time :as pdt])
  (:import (java.time ZonedDateTime ZoneId)
    (java.time.format DateTimeFormatter)
    (java.time.temporal TemporalAccessor)))

(def ^:const basic-date-time-formatter
  "yyyy-MM-dd['T'HH:mm:ss[VV]]")

(def all-date-time-formatters
  [(DateTimeFormatter/ofPattern basic-date-time-formatter)
   (DateTimeFormatter/RFC_1123_DATE_TIME)])

(defn defaulting-date-time [date-time default]
  (reify TemporalAccessor
    (get [_this field] (if (.isSupported date-time field)
                         (.get date-time field)
                         (.get default field)))
    (getLong [_this field] (if (.isSupported date-time field)
                             (.getLong date-time field)
                             (.getLong default field)))
    (isSupported [_this field] (or (.isSupported date-time field)
                                   (.isSupported default field)))
    (query [_this query] (try
                           (.query date-time query)
                           (catch Throwable _
                             (.query default query))))
    (range [_this field] (if (.isSupported date-time field)
                           (.range date-time field)
                           (.range default field)))))

(defn date-time->defaulting [date-time]
  (let [defaults (ZonedDateTime/of 1970 1 1 12 0 0 0 (ZoneId/of "UTC"))]
    (defaulting-date-time date-time defaults)))

(defn parse [str]
  (->> str
       (pdt/parse-date-time all-date-time-formatters)
       date-time->defaulting))