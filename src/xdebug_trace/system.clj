(ns xdebug-trace.system
  (:require [xdebug-trace.service :as service]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn system []
  {:server nil
   :port (Integer/parseInt (:port env "8080"))
   :trace-path (or (env :trace-path)
                   (.getPath (io/resource "traces/")))})

(defn start [{:keys [port trace-path] :as system}]
  (assoc system :server
         (run-jetty (service/create-app trace-path)
                    {:join? false :port port})))

(defn stop [system]
  (when-let [server (:server system)]
    (.stop server))
  (dissoc system :server))

