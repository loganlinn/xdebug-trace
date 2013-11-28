(ns xdebug-trace.view.layout
  (:require [hiccup.core :refer [html]]
            [hiccup.page :as page]
            [hiccup.util :refer [url]]))

(def ^:dynamic *request* nil)

(defn- build-blocks
  [body]
  (into {} (for [[op & form-body] body]
             (condp = op
               'defblock (let [[block-name & block-body] form-body]
                           [(keyword block-name) block-body])
               :else (throw (RuntimeException. "Unexpected form"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

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
          [:nav.navbar.navbar-default
           {:role "navigation"}
           [:div.navbar-header
            [:button.navbar-toggle
             {:type "button"
              :data-toggle "collapse"
              :data-target "#navbar-collapse"}
             [:span.sr-only "Toggle navigation"]
             [:span.icon-bar][:span.icon-bar][:span.icon-bar]]
            [:a.navbar-brand {:href "/"} "Xdebug Trace"]]
           [:div#navbar-collapse.collapse.navbar-collapse
            [:ul.nav.navbar-nav
             [:li [:a {:href "/trace"} "Traces"]]]] ]
          [:div.container
           ~@(:content blocks)]
          (page/include-js "http://code.jquery.com/jquery.js"
                           "/js/bootstrap.min.js")
          ~@(:body-end blocks)]))))
