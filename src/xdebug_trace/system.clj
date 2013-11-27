(ns xdebug-trace.system
  (:require [xdebug-trace.service :as service]
            [clojure.java.io :as io]
            [datomic.api :as d]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]))


(defn- start-db [{uri :db/uri :as system}]
  (if uri
    (do
      (d/create-database uri)
      (assoc system :db (d/connect uri)))
    system))

(defn- start-server [{:keys [port trace-path] :as system}]
  (assoc system :server
         (run-jetty (service/create-app trace-path)
                    {:join? false :port port})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn system []
  (let [db-uri (env :datomic-uri "datomic:mem://xdebug-trace")]
    {:server nil
     :port (Integer/parseInt (:port env "8080"))
     :trace-path (or (env :trace-path)
                     (.getPath (io/resource "traces/")))
     :db/uri db-uri
     :db nil
     :db/delete (fn [] (d/delete-database db-uri))}))

(defn start [system]
  (-> system
      start-db
      start-server))

(defn stop [system]
  (when-let [server (:server system)]
    (.stop server))
  (dissoc system :server))

