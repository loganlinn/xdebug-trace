(ns xdebug-trace.view.util
  (:require [environ.core :refer [env]]
            [clojure.string :as str]))

(defn ensure-prefix
  [^String s prefix]
  (let [prefix (str prefix)]
    (if (.startsWith s prefix)
      s
      (str prefix s))))

(defn filepath->repo-url
  ([filename]
   (filepath->repo-url filename nil))
  ([filename line-num]
   (filepath->repo-url filename line-num (env :repo-base-url)))
  ([filename line-num base-url]
   (filepath->repo-url filename line-num base-url (some-> (env :docroot-pattern) re-pattern)))
  ([filename line-num base-url doc-root]
   (if (and base-url doc-root)
     (let [source-path (-> (str/replace-first filename doc-root "") (ensure-prefix \/))
           url (str base-url source-path)]
       (if line-num
         (str url "#L" line-num)
         url)))))
