(ns xdebug-trace.service
  (:require [xdebug-trace.view
             [trace :as view.trace]
             [index :as view.index]
             [layout :as view.layout]]
            [xdebug-trace.reader :as reader]
            [clojure.java.io :as io]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as resp]
            [compojure.core :refer [routes GET POST ANY]]
            [compojure.route :as route]
            [compojure.handler]
            [environ.core :refer [env]]))

(defn get-trace [path limit offset]
  (let [limit (if limit (Long/parseLong limit) 1e6)
        offset (if offset (Long/parseLong offset) 0)]
    (with-open [rdr (io/reader (io/resource (str "traces/" path)))]
      (->> (reader/trace-line-seq rdr)
           (drop offset)
           ;(take limit)
           (reader/read-trace)
           (view.trace/render-trace)))))

(defn trace-store []
  )

(defn create-app []
  (-> (routes
        (GET "/" [] (view.index/index))
        (GET "/trace/:id" [id limit offset]
             (get-trace id limit offset)))
      (view.layout/wrap-with-request)
      (wrap-resource "public")
      (compojure.handler/site)))
