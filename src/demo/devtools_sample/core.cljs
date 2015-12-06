(ns devtools-sample.core
  (:require-macros [devtools-sample.logging :refer [log]])
  (:require [clojure.string :as string]
            [devtools-sample.boot :refer [boot!]]
            [devtools-sample.more :refer [more!]]
            [devtools.format :as format]))

(boot!)

; --- MEAT STARTS HERE -->
; note: (log ...) function is a shorthand for (.log js/console ...)

(log nil 42 0.1 :keyword 'symbol "string" #"regexp" [1 2 3] #{1 2 3} {:k1 1 :k2 2} #js [1 2 3] #js {"k1" 1 "k2" 2})
(log [nil 42 0.1 :keyword 'symbol "string" #"regexp" [1 2 3] #{1 2 3} {:k1 1 :k2 2} #js [1 2 3] #js {"k1" 1 "k2" 2} (js/Date.)])
(log (range 100) (range 101) (range 220) (interleave (repeat :even) (repeat :odd)))
(log {:k1 'v1 :k2 'v2 :k3 'v3 :k4 'v4 :k5 'v5 :k6 'v6 :k7 'v7 :k8 'v8 :k9 'v9})
(log #{1 2 3 4 5 6 7 8 9 10 11 12 13 14 15})
(log [[js/window] (js-obj "k1" "v1" "k2" :v2) #(.log js/console "hello") (js* "function(x) { console.log(x); }")])
(log [1 2 3 4 5 [10 20 30 40 50 [100 200 300 400 500 [1000 2000 3000 4000 5000 :*]]]])
(log [1 2 3 [10 20 30 [100 200 300 [1000 2000 3000 :*]]]])
(log (atom {:number 0 :string "string" :keyword :keyword :symbol 'symbol :vector [0 1 2 3 4 5 6] :set '#{a b c} :map '{k1 v1 k2 v2}}))
(log (with-meta ["has meta data"] {:some "meta-data"}))

; custom formatter defined in user code
(deftype Person [name address]
  format/IDevtoolsFormat
  (-header [_] (format/template "span" "color:white; background-color:blue; padding: 0px 4px" (str "Person: " name)))
  (-has-body [_] (not (nil? address)))
  (-body [_] (format/standard-body-template (string/split-lines address))))

(log (Person. "John Doe" "Office 33\n27 Colmore Row\nBirmingham\nEngland") (Person. "Mr Homeless" nil))

; <-- MEAT STOPS HERE ---

(more!)