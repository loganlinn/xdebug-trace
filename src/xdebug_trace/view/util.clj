(ns xdebug-trace.view.util
  (:require [xdebug-trace.view.layout :as layout]
            [xdebug-trace.util :refer [query-str]]
            [hiccup.def :refer [defelem]]
            [environ.core :refer [env]]
            [clojure.string :as str]))

(defn- ensure-prefix
  [^String s prefix]
  (if (.startsWith s (str prefix))
    s
    (str prefix s)))

(defn- ensure-no-prefix
  [^String s prefix]
  (let [prefix (str prefix)]
    (if (.startsWith s prefix)
      (.substring s (count prefix))
      s)))

(defn- default-repo-base-url []
  (let [v (env :repo-base-url)]
    (when-not (str/blank? v) v)))

(defn- default-doc-root []
  (if-let [v (env :docroot)]
    (when-not (str/blank? v) (re-pattern v))))

(defn filepath->repo-url
  ([filename]
   (filepath->repo-url filename nil))
  ([filename line-num]
   (filepath->repo-url filename line-num (default-repo-base-url)))
  ([filename line-num base-url]
   (filepath->repo-url filename line-num base-url (default-doc-root)))
  ([filename line-num base-url doc-root]
   (if (and base-url doc-root)
     (let [source-path (-> (str/replace-first filename doc-root "") (ensure-prefix \/))
           url (str (ensure-no-prefix base-url \/) (ensure-prefix source-path \/))]
       (if line-num
         (str url "#L" line-num)
         url)))))

(defn- stringify-keys [m]
  (into {} (for [[k v] m] [(name k) v])))

(defn merged-query-str [m]
  (-> (:query-params layout/*request*)
      (merge (stringify-keys m))
      query-str))

(defelem css-bar-chart
  ([width] (css-bar-chart width 0.))
  ([width offset]
   [:div.css-bar-chart
    [:div
     {:style (str "width:" (format "%.2f" (max (* width 100.) 1.))"%;"
                  "margin-left:" (format "%.2f" (* offset 100.)) "%")}
     "&nbsp;"]]))
