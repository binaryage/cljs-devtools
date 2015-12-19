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

(def state (atom []))

(defn simple-fn [count]
  (let [rng (range count)]
    (doseq [item rng]
      (let [s (str "item=" item)]
        (swap! state conj s)))))                                                                                              ; <- set breakpoint HERE and see Scope variables in DevTools

(defn more! []
  (log (SomeType. "some value"))
  (log [test-lang test-reify])
  (log (.-nested test-interleaved))
  (log test-interleaved)
  (log [long-string])

  (simple-fn 10)
  (log state)

  (log [::namespaced-keyword])
  (log [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]]))

(defn fn-returns-nil [])

(defn ^:export sanity-test-handler []
  ((fn-returns-nil) "param"))                                                                                                 ; a test for sanity checker



