(ns xdebug-trace.view.layout
  (:require [hiccup.core :refer [html]]
            [hiccup.page :as page]
            [hiccup.util :refer [url]]))

(def ^:dynamic *request* nil)

(defmacro with-request
  "Macro to bind a request to rendering scope"
  [request & body]
  `(binding [*request* ~request] ~@body))

(defn wrap-with-request
  "Middleware to bind current request to rendering scope"
  [handler]
  (fn [request]
    (with-request request
      (handler request))))

(defn- build-blocks
  [body]
  (into {} (for [[op & form-body] body]
             (condp = op
               'defblock (let [[block-name & block-body] form-body]
                           [(keyword block-name) block-body])
               :else (throw (RuntimeException. "Unexpected form"))))))

(defmacro defpage
  [title bindings & body]
  (let [blocks (build-blocks body)
        page-title (:title blocks "Xdebug Trace")]
    `(defn ~title ~bindings
       (page/html5
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
          [:title ~page-title]
          (page/include-css "/css/bootstrap.min.css")
          ~@(:head-end blocks)]
         [:body
          [:div#content.large-12.columns
           ~@(:content blocks)]
          (page/include-js "http://code.jquery.com/jquery.js"
                           "/js/bootstrap.min.js")
          ~@(:body-end blocks)]))))
