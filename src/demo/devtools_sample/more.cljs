(ns devtools-sample.more
  (:require-macros [devtools-sample.logging :refer [log]])
  (:require [devtools.format :as format]
            [devtools.toolbox :as toolbox]))

(deftype SomeType [some-field])

(def test-interleaved #js {"js" true "nested" {:js false :nested #js {"js2" true "nested2" {:js2 false}}}})

(def v nil)

(def circular1 (atom nil))
(reset! circular1 circular1)

(def circular2 {:k (atom nil)})
(reset! (:k circular2) circular2)

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
                  (-header [_] (format/template "span"
                                                "color:white; background-color:brown; padding: 0px 4px"
                                                "testing reify"))
                  (-has-body [_] false)
                  (-body [_])))

(def long-string
  (str "First line\n"
       "second line\n"
       "third line is really "
       "looooooooooooooooooooooooooooooooooooooooooooooooooooooooong "
       "looooooooooooooooooooooooooooooooooooooooooooooooooooooooong "
       "looooooooooooooooooooooooooooooooooooooooooooooooooooooooong \n"
       "\n"
       "last line"))

(def busy-obj
  (js-obj
    "a" 1
    "s" "string"
    "f" (fn some-fn [] (str "do" "something"))))

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

  (log [::namespaced-keyword])
  (log [1 [2 [3 [4 [5 [6 [7 [8 [9]]]]]]]]])
  (log circular1)
  (log circular2)
  (log (var v))
  (log (toolbox/envelope (range 20)))
  (log (toolbox/envelope js/window "win envelope with a custom header"))
  (log (toolbox/envelope "hello!" #(str "envelope with a custom header function, the wrapped value is '" % "'")))
  (log (toolbox/envelope busy-obj
                         "busy js-object envelope with a custom style"
                         "color:white;background-color:purple;padding:0px 4px;"
                         "div"))

  ; see https://github.com/binaryage/cljs-devtools/issues/14#issuecomment-217485305
  (.log js/console {:value "b"
                    :map {:some-key "some-val"}
                    :vec [{:1 "1-val"
                           :2 "2-val"
                           :3 {:3-val-k "3-val"}
                           ;; :4 (range 0 20)
                           }]}))

(defn fn-returns-nil [])

(defn ^:export sanity-test-handler []
  ((fn-returns-nil) "param"))                                                                                                 ; a test for sanity checker

(defn ^:export breakpoint-test-handler []
  (simple-fn 10)
  (log state))



