(defproject xdebug-trace "0.1.0-SNAPSHOT"
  :description "Parse & analyze PHP's Xdebug trace files"
  :url "https://github.com/loganlinn/xdebug-trace"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.priority-map "0.0.4"]
                 [environ "0.4.0"]
                 [ring/ring-core "1.2.1"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.4"]
                 [clj-time "0.6.0"]]
  :plugins [[lein-daemon "0.5.4"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/java.classpath "0.2.0"]]}
             :uberjar {:main xdebug-trace.main
                       :aot [xdebug-trace.main]}
             :jdk6 {:dependencies [[org.codehaus.jsr166-mirror/jsr166y "1.7.0"]]}}
  :resource-paths ["resources" "upload"]
  :daemon {:server {:ns xdebug-trace.main
                    :pidfile "xdebug-trace-jetty.pid"}})
