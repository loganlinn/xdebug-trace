(ns xdebug-trace.file
  "Lists and finds available trace files"
  (:require [xdebug-trace.util :refer [distinct-on]])
  (:import [java.io File FileFilter]))

(def trace-ext ".xt")

(defn ^FileFilter trace-files-filter []
  (reify FileFilter
    (accept [_ f]
      (.endsWith (.getName f) trace-ext))))

(defn ^FileFilter trace-file-filter [trace-name]
  (reify FileFilter
    (accept [_ f]
      (= (.getName f) (str trace-name trace-ext)))))

(defn find-traces
  "Returns sequence of all available trace files"
  [trace-dirs]
  (let [file-filter (trace-files-filter)]
    (->> trace-dirs
         (mapcat (fn [^File dir] (.listFiles dir file-filter)))
         (distinct-on (fn [^File f] (.getName f))))))

(defn find-trace [trace-dirs trace-name]
  (let [file-filter (trace-file-filter trace-name)]
    (some (fn [^File dir]
            (first (.listFiles dir file-filter)))
          trace-dirs)))
