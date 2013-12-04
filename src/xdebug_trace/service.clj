(ns xdebug-trace.service
  "HTTP service"
  (:require [xdebug-trace
             [trace :as trace]
             [reader :as reader]
             [file :as file]]
            [xdebug-trace.view
             [trace :as view.trace]
             [trace-summary :as view.trace-summary]
             [index :as view.index]
             [layout :as view.layout]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as res]
            [ring.util.time :as time]
            [compojure.core :refer [routes GET POST ANY]]
            [compojure.route :as route]
            [compojure.handler]
            [environ.core :refer [env]])
  (:import [java.io File]
           [java.util Date]))

(defn read-trace [^File file limit offset max-depth]
  (with-open [rdr (io/reader file)]
    (cond->> (reader/trace-line-seq rdr)
      offset (drop offset)
      limit  (take limit)
      ;; TODO max-depth
      true   (reader/read-trace))))

(defn- long-query-param
  "Extracts query param and parses as long"
  ([req param] (long-query-param req param nil))
  ([req param default]
   (if-let [v (get-in req [:query-params (name param)])]
     (try (Long/parseLong v)
          (catch RuntimeException _ default))
     default)))

(defn- not-modified-since?
  [{headers :headers :as req} last-modified]
  (if-let [ims (some-> (headers "if-modified-since") time/parse-date)]
    (not (.before ims last-modified))))

(defn view-trace-handler
  [{{:keys [trace-name]} :params :as req} trace-dirs]
  (if-let [^File trace-file (file/find-trace trace-dirs trace-name)]
    (let [lmodified (Date. (.lastModified trace-file))]
      (if (not-modified-since? req lmodified)
        (-> (res/response "") (res/status 304))
        (let [limit (long-query-param req :limit)
              offset (long-query-param req :offset)
              max-depth (long-query-param req :max-depth)
              trace (read-trace trace-file limit offset max-depth)]
          (-> (view.trace/render-trace trace-name trace {:max-depth max-depth})
              (res/response )
              (res/header "ETag" "123123123")
              (res/header "Last-Modified"
                          (time/format-date lmodified))
              (res/header "Cache-Control"
                          "no-transform,public,max-age=60,s-maxage=60")))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn create-handler [trace-dirs]
  (routes
    (GET "/" [] (view.index/index))
    (GET "/trace/:trace-name/summary/:fn-name"
         {{:keys [trace-name fn-name]} :params :as req}
         (if-let [^File trace-file (file/find-trace trace-dirs trace-name)]
           (let [limit (long-query-param req :limit)
                 offset (long-query-param req :offset)
                 max-depth (long-query-param req :max-depth)
                 trace (read-trace trace-file limit offset max-depth)]
             (pr-str (time (trace/fn-summary trace fn-name))))))
    (GET "/trace/:trace-name/summary"
         {{:keys [trace-name]} :params :as req}
         (if-let [^File trace-file (file/find-trace trace-dirs trace-name)]
           (let [limit (long-query-param req :limit)
                 offset (long-query-param req :offset)
                 max-depth (long-query-param req :max-depth)
                 n (long-query-param req :n 10)
                 trace (read-trace trace-file limit offset max-depth)
                 summary (trace/trace-summary trace)
                 top-n (trace/trace-summary-top-n n summary)]
             (view.trace-summary/trace-summary trace-name top-n n))))
    (GET "/trace/:trace-name" req (view-trace-handler req trace-dirs))
    (GET "/trace" []
         (let [trace-files (file/find-traces trace-dirs)]
           (view.trace/list-traces trace-files)))))

(defn create-app [trace-path]
  (let [trace-dirs (map io/file (str/split trace-path #":"))]
    (-> (create-handler trace-dirs)
        (view.layout/wrap-with-request)
        (wrap-resource "public")
        (compojure.handler/site))))
