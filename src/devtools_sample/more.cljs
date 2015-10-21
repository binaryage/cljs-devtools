(ns devtools-sample.more
  (:require-macros [devtools-sample.logging :refer [log]])
  (:require [devtools.format :as format]))

(defn log [& args] (.apply (aget js/console "log") js/console (into-array args)))

(deftype SomeType [some-field])

(def test-interleaved #js {"js" true "nested" {:js false :nested #js {"js2" true "nested2" {:js2 false}}}})

; defrecord with IDevtoolsFormat
(defrecord Language [lang]
  format/IDevtoolsFormat
  (-header [_] (format/template "span" "color:white; background-color:darkgreen; padding: 0px 4px" (str "Language: " lang)))
  (-has-body [_])
  (-body [_]))

(def test-lang (Language. "ClojureScript"))

; reify with IDevtoolsFormat
(def test-reify (reify
                  format/IDevtoolsFormat
                  (-header [_] (format/template "span" "color:white; background-color:brown; padding: 0px 4px" "testing reify"))
                  (-has-body [_] false)
                  (-body [_])))

(def long-string
  "First line
second line
third line is really looooooooooooooooooooooooooooooooooooooooooooooooooooooooong looooooooooooooooooooooooooooooooooooooooooooooooooooooooong looooooooooooooooooooooooooooooooooooooooooooooooooooooooong

last line")

(def global (atom []))

(defn break-into-this-fn [param]
  (let [range (range 3)
        seq (interleave (repeat :even) (repeat :odd))]
    (doseq [item range]
      (let [s (str item "(" (nth seq item) ") " param)]
        (reset! global (conj @global s))))))                                                                          ; <- put breakpoint HERE and see Scope variables in the Devtools

(defn more! []
  (log (SomeType. "some value"))
  (log [test-lang test-reify])
  (log (.-nested test-interleaved))
  (log test-interleaved)
  (log [long-string])

  (break-into-this-fn "postfix")
  (log global)

  (log [::namespaced-keyword])
  (log [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]]))

(defn fn-returns-nil [])

(defn ^:export sanity-test-handler []
  ((fn-returns-nil) "param"))                                                                                         ; a test for sanity checker



