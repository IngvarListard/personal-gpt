(defproject personal-gpt "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [environ "1.2.0"]
                 [morse "0.4.3"]
                 [net.clojars.wkok/openai-clojure "0.6.0"]
                 [org.clojure/core.async "1.6.673"]
                 [markdown-clj "1.11.4"]]

  :plugins [[lein-environ "1.2.0"]
            [migratus-lein "0.7.3"]
            [lein-pprint "1.3.2"] ]

  :main ^:skip-aot personal-gpt.core
  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
