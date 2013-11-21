(ns xdebug-trace.service
  "HTTP service"
  (:require [xdebug-trace.view
             [trace :as view.trace]
             [index :as view.index]
             [layout :as view.layout]]
            [xdebug-trace.reader :as reader]
            [xdebug-trace.util :refer [distinct-on]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as resp]
            [compojure.core :refer [routes GET POST ANY]]
            [compojure.route :as route]
            [compojure.handler]
            [environ.core :refer [env]])
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

(defn get-trace [^File file limit offset]
  (let [limit (if limit (Long/parseLong limit) 1e6)
        offset (if offset (Long/parseLong offset) 0)]
    (with-open [rdr (io/reader file)]
      (->> (reader/trace-line-seq rdr)
           (drop offset)
           ;(take limit)
           (reader/read-trace)
           (view.trace/render-trace)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn create-app [trace-path]
  (let [trace-dirs (map io/file (str/split trace-path #":"))]
    (-> (routes
          (GET "/" [] (view.index/index))
          (GET "/trace/:trace-name" [trace-name limit offset]
               (if-let [trace-file (find-trace trace-dirs trace-name)]
                 (get-trace trace-file limit offset)))
          (GET "/trace" []
               (let [trace-files (find-traces trace-dirs)]
                 (view.trace/render-traces trace-files))))
        (view.layout/wrap-with-request)
        (wrap-resource "public")
        (compojure.handler/site))))
