(defproject patient_front "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.866"]
                 [reagent "1.1.0"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]
                 [cljs-http "0.1.46"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [adzerk/env "0.4.0"]]

  :plugins [[lein-cljsbuild "1.1.8"]]

  :source-paths ["src" "resources" "target"]
  :resource-paths ["target" "resources"]
  :clean-targets ^{:protect false} ["resources/public/js/" "target"]

  :cljsbuild {:builds {:dev  {:source-paths ["src"]
                              :resource-paths ["resources"]
                              :jvm-opts ["-DPATIENT_BACK_URL=http://localhost:8080"]
                              :compiler     {:optimizations :none
                                             :source-map    true}}
                       :prod {:source-paths ["src" "resources"]
                              :compiler {:output-dir  "resources/public/js/"
                                         :output-to    "resources/public/js/main.js"
                                         :source-map   "resources/public/js/main.js.map"
                                         :optimizations :advanced
                                         :pretty-print  false}}}
              }

  :aliases {"fig"       ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:min"   ["trampoline" "run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]}

  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.13"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]]

                   :resource-paths ["target" "resources/public/js/"]
                   :clean-targets ^{:protect false} ["target" "resources/public/js/"]}
             :prod {:jvm-opts ["-DPATIENT_BACK_URL=http://localhost:8080"]}})

