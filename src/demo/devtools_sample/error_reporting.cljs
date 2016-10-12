(ns devtools-sample.error-reporting
  (:require-macros [devtools-sample.logging :refer [log]])
  (:require [clojure.string :as string]
            [devtools-sample.boot :refer [boot!]]
            [devtools.formatters.markup :refer [<standard-body>]]
            [devtools.formatters.templating :refer [render-markup]]
            [devtools.protocols :refer [IFormat]]))

(boot! "/src/demo/devtools_sample/error_reporting.cljs")

(enable-console-print!)

; --- MEAT STARTS HERE -->

(deftype Person [name address]
  IFormat
  (-header [_] (do
                 (throw (js/Error. "some problem"))
                 (render-markup [["span" "color:white;background-color:#999;padding:0px 4px;"] (str "Person: " name)])))
  (-has-body [_] (some? address))
  (-body [_] (render-markup (<standard-body> (string/split-lines address)))))

(deftype Person2 [name address]
  IFormat
  (-header [_] (do
                 (throw (ex-info "ex-info-based message" {:some "data"}))
                 (render-markup [["span" "color:white;background-color:#999;padding:0px 4px;"] (str "Person: " name)])))
  (-has-body [_] (some? address))
  (-body [_] (render-markup (<standard-body> (string/split-lines address)))))

(deftype Person3 [name address]
  IFormat
  (-header [_] (render-markup [["span" "color:white;background-color:#999;padding:0px 4px;"] (str "Person: " name)]))
  (-has-body [_] (some? address))
  (-body [_] (do
               (throw "a problem during body expansion")
               (render-markup (<standard-body> (string/split-lines address))))))

(deftype Person4 [name address]
  IFormat
  (-header [_] (render-markup [["span" "color:white;background-color:#999;padding:0px 4px;"] (str "Person: " name)]))
  (-has-body [_] (do
                   (throw "a problem in -has-body")
                   (some? address)))
  (-body [_] (render-markup (<standard-body> (string/split-lines address)))))

(log (Person. "John Doe" "Office 33\n27 Colmore Row\nBirmingham\nEngland"))
(log (Person2. "John Doe" "Office 33\n27 Colmore Row\nBirmingham\nEngland"))
(log (Person3. "John Doe" "Office 33\n27 Colmore Row\nBirmingham\nEngland"))
(log (Person4. "John Doe" "Office 33\n27 Colmore Row\nBirmingham\nEngland"))

; <-- MEAT STOPS HERE ---
